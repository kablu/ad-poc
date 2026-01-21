# Advanced RA Features - Enterprise Implementation Guide

## Table of Contents
1. [Auto-Enrollment Implementation](#1-auto-enrollment-implementation)
2. [Scheduled Enrollment Implementation](#2-scheduled-enrollment-implementation)
3. [Bulk Enrollment Implementation](#3-bulk-enrollment-implementation)
4. [Just-In-Time (JIT) Provisioning](#4-just-in-time-jit-provisioning)
5. [Device Auto-Registration](#5-device-auto-registration)
6. [Architecture & Integration](#6-architecture--integration)

---

## 1. Auto-Enrollment Implementation

### 1.1 Overview
Auto-Enrollment automatically provisions certificates to eligible users and devices without manual intervention, similar to Microsoft AD CS Auto-Enrollment via Group Policy.

### 1.2 Architecture Components

```
┌──────────────────────────────────────────────────────────────┐
│                    Auto-Enrollment System                     │
├──────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌────────────────┐      ┌─────────────────┐                │
│  │  Policy Engine │─────►│  Eligibility    │                │
│  │  (Templates &  │      │  Checker        │                │
│  │   AD Groups)   │      │  (AD Queries)   │                │
│  └────────────────┘      └─────────────────┘                │
│           │                       │                          │
│           ▼                       ▼                          │
│  ┌────────────────────────────────────────┐                 │
│  │     Auto-Enrollment Service            │                 │
│  │  - Detect missing certificates         │                 │
│  │  - Generate CSR automatically          │                 │
│  │  - Submit to CA                        │                 │
│  │  - Distribute certificates             │                 │
│  └────────────────────────────────────────┘                 │
│           │                       │                          │
│           ▼                       ▼                          │
│  ┌────────────────┐      ┌─────────────────┐                │
│  │  CA Integration│      │  Certificate    │                │
│  │  Module        │      │  Distribution   │                │
│  └────────────────┘      └─────────────────┘                │
│                                                               │
└──────────────────────────────────────────────────────────────┘
```

### 1.3 Database Schema Extensions

```sql
-- Auto-Enrollment Configuration Table
CREATE TABLE auto_enrollment_policies (
    policy_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    policy_name VARCHAR(255) NOT NULL,
    template_id UUID NOT NULL REFERENCES certificate_templates(template_id),
    enabled BOOLEAN DEFAULT true,

    -- Eligibility Criteria
    eligible_ad_groups TEXT[], -- Array of AD group DNs
    eligible_departments TEXT[],
    eligible_user_types TEXT[], -- ['EMPLOYEE', 'CONTRACTOR', 'SERVICE_ACCOUNT']

    -- Enrollment Behavior
    auto_approve BOOLEAN DEFAULT false, -- Skip RA Officer approval
    enrollment_trigger VARCHAR(50), -- 'LOGIN', 'SCHEDULED', 'GROUP_CHANGE'
    enrollment_schedule JSONB, -- Cron expression for scheduled checks

    -- Certificate Lifecycle
    auto_renew BOOLEAN DEFAULT true,
    renewal_threshold_days INTEGER DEFAULT 30, -- Renew 30 days before expiry
    auto_revoke_on_user_disable BOOLEAN DEFAULT true,

    -- Key Generation
    key_generation_location VARCHAR(20), -- 'CLIENT', 'SERVER', 'HSM'
    key_algorithm VARCHAR(50) DEFAULT 'RSA',
    key_size INTEGER DEFAULT 2048,

    -- Distribution
    distribution_methods TEXT[], -- ['WEB_DOWNLOAD', 'EMAIL', 'CERT_STORE', 'LDAP']

    -- Notifications
    notify_on_enrollment BOOLEAN DEFAULT true,
    notify_on_renewal BOOLEAN DEFAULT false,

    -- Audit
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(user_id),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID REFERENCES users(user_id)
);

-- Auto-Enrollment Job Tracking
CREATE TABLE auto_enrollment_jobs (
    job_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    policy_id UUID REFERENCES auto_enrollment_policies(policy_id),
    job_type VARCHAR(50), -- 'ENROLLMENT', 'RENEWAL', 'REVOCATION'
    trigger VARCHAR(50), -- 'LOGIN', 'SCHEDULED', 'MANUAL'

    -- Job Status
    status VARCHAR(50), -- 'PENDING', 'RUNNING', 'COMPLETED', 'FAILED'
    started_at TIMESTAMP,
    completed_at TIMESTAMP,

    -- Statistics
    users_processed INTEGER DEFAULT 0,
    certificates_issued INTEGER DEFAULT 0,
    certificates_renewed INTEGER DEFAULT 0,
    failures INTEGER DEFAULT 0,

    -- Details
    error_log TEXT,
    processed_users JSONB, -- Array of user IDs and results

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_auto_enrollment_jobs_status ON auto_enrollment_jobs(status);
CREATE INDEX idx_auto_enrollment_jobs_policy ON auto_enrollment_jobs(policy_id);

-- Auto-Enrollment User State
CREATE TABLE auto_enrollment_user_state (
    state_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(user_id),
    policy_id UUID REFERENCES auto_enrollment_policies(policy_id),

    -- Current Certificate
    current_certificate_id UUID REFERENCES certificates(certificate_id),
    last_enrollment_date TIMESTAMP,
    next_renewal_date TIMESTAMP,

    -- Enrollment History
    enrollment_count INTEGER DEFAULT 0,
    renewal_count INTEGER DEFAULT 0,
    last_failure_date TIMESTAMP,
    last_failure_reason TEXT,

    -- Flags
    auto_enrollment_enabled BOOLEAN DEFAULT true,
    manual_override BOOLEAN DEFAULT false, -- Admin disabled auto-enrollment for this user

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    UNIQUE(user_id, policy_id)
);

CREATE INDEX idx_auto_enrollment_user_state_user ON auto_enrollment_user_state(user_id);
CREATE INDEX idx_auto_enrollment_user_state_renewal ON auto_enrollment_user_state(next_renewal_date);
```

### 1.4 Backend Implementation

#### 1.4.1 Auto-Enrollment Service (Java Spring Boot)

```java
package com.company.ra.service.enrollment;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AutoEnrollmentService {

    @Autowired
    private AutoEnrollmentPolicyRepository policyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActiveDirectoryService adService;

    @Autowired
    private CertificateRequestService certRequestService;

    @Autowired
    private CertificateAuthorityService caService;

    @Autowired
    private KeyGenerationService keyGenService;

    @Autowired
    private CertificateDistributionService distributionService;

    @Autowired
    private AuditService auditService;

    /**
     * Trigger auto-enrollment on user login
     * Called by authentication filter after successful AD authentication
     */
    @Transactional
    public AutoEnrollmentResult processUserLogin(User user) {
        auditService.log(AuditAction.AUTO_ENROLLMENT_TRIGGERED, user.getUserId(),
            "trigger", "LOGIN");

        // Find applicable policies for this user
        List<AutoEnrollmentPolicy> policies = findApplicablePolicies(user,
            EnrollmentTrigger.LOGIN);

        if (policies.isEmpty()) {
            return AutoEnrollmentResult.noAction("No applicable policies");
        }

        AutoEnrollmentResult result = new AutoEnrollmentResult();

        for (AutoEnrollmentPolicy policy : policies) {
            try {
                // Check if user already has valid certificate for this policy
                Optional<Certificate> existingCert =
                    checkExistingCertificate(user, policy);

                if (existingCert.isPresent()) {
                    Certificate cert = existingCert.get();

                    // Check if renewal needed
                    if (isRenewalNeeded(cert, policy)) {
                        renewCertificate(user, cert, policy, result);
                    } else {
                        result.addSkipped(policy, "Valid certificate exists");
                    }
                } else {
                    // Enroll new certificate
                    enrollNewCertificate(user, policy, result);
                }

            } catch (Exception e) {
                result.addFailure(policy, e.getMessage());
                auditService.log(AuditAction.AUTO_ENROLLMENT_FAILED, user.getUserId(),
                    "policy", policy.getPolicyId(), "error", e.getMessage());
            }
        }

        return result;
    }

    /**
     * Scheduled job for auto-enrollment checks
     * Runs every hour to check for users needing enrollment/renewal
     */
    @Scheduled(cron = "0 0 * * * *") // Every hour
    @Transactional
    public void scheduledAutoEnrollmentCheck() {
        List<AutoEnrollmentPolicy> policies = policyRepository
            .findByEnabledAndEnrollmentTrigger(true, EnrollmentTrigger.SCHEDULED);

        for (AutoEnrollmentPolicy policy : policies) {
            AutoEnrollmentJob job = createJob(policy, EnrollmentTrigger.SCHEDULED);

            try {
                processScheduledEnrollment(policy, job);
            } catch (Exception e) {
                job.setStatus(JobStatus.FAILED);
                job.setErrorLog(e.getMessage());
                auditService.log(AuditAction.AUTO_ENROLLMENT_JOB_FAILED,
                    "policy", policy.getPolicyId(), "error", e.getMessage());
            } finally {
                job.setCompletedAt(LocalDateTime.now());
                jobRepository.save(job);
            }
        }
    }

    /**
     * Process scheduled auto-enrollment for a policy
     */
    private void processScheduledEnrollment(AutoEnrollmentPolicy policy,
                                           AutoEnrollmentJob job) {
        job.setStatus(JobStatus.RUNNING);
        job.setStartedAt(LocalDateTime.now());
        jobRepository.save(job);

        // Get eligible users from AD
        List<User> eligibleUsers = getEligibleUsers(policy);
        job.setUsersProcessed(eligibleUsers.size());

        int issued = 0, renewed = 0, failures = 0;
        List<Map<String, Object>> processedUsers = new ArrayList<>();

        for (User user : eligibleUsers) {
            Map<String, Object> userResult = new HashMap<>();
            userResult.put("userId", user.getUserId());
            userResult.put("username", user.getUsername());

            try {
                Optional<Certificate> existingCert =
                    checkExistingCertificate(user, policy);

                if (existingCert.isPresent()) {
                    Certificate cert = existingCert.get();
                    if (isRenewalNeeded(cert, policy)) {
                        renewCertificateInternal(user, cert, policy);
                        renewed++;
                        userResult.put("action", "RENEWED");
                    } else {
                        userResult.put("action", "SKIPPED");
                        userResult.put("reason", "Valid certificate exists");
                    }
                } else {
                    enrollNewCertificateInternal(user, policy);
                    issued++;
                    userResult.put("action", "ENROLLED");
                }

                userResult.put("status", "SUCCESS");

            } catch (Exception e) {
                failures++;
                userResult.put("status", "FAILED");
                userResult.put("error", e.getMessage());
            }

            processedUsers.add(userResult);
        }

        job.setCertificatesIssued(issued);
        job.setCertificatesRenewed(renewed);
        job.setFailures(failures);
        job.setProcessedUsers(processedUsers);
        job.setStatus(JobStatus.COMPLETED);

        auditService.log(AuditAction.AUTO_ENROLLMENT_JOB_COMPLETED,
            "policy", policy.getPolicyId(),
            "issued", issued,
            "renewed", renewed,
            "failures", failures);
    }

    /**
     * Find applicable auto-enrollment policies for a user
     */
    private List<AutoEnrollmentPolicy> findApplicablePolicies(User user,
                                                              EnrollmentTrigger trigger) {
        // Get all enabled policies for this trigger
        List<AutoEnrollmentPolicy> policies = policyRepository
            .findByEnabledAndEnrollmentTrigger(true, trigger);

        // Get user's AD groups
        Set<String> userGroups = adService.getUserGroups(user);

        // Filter policies based on eligibility
        return policies.stream()
            .filter(policy -> isUserEligible(user, userGroups, policy))
            .collect(Collectors.toList());
    }

    /**
     * Check if user is eligible for a policy
     */
    private boolean isUserEligible(User user, Set<String> userGroups,
                                   AutoEnrollmentPolicy policy) {
        // Check AD group membership
        if (policy.getEligibleAdGroups() != null &&
            !policy.getEligibleAdGroups().isEmpty()) {

            boolean inGroup = policy.getEligibleAdGroups().stream()
                .anyMatch(userGroups::contains);

            if (!inGroup) {
                return false;
            }
        }

        // Check department
        if (policy.getEligibleDepartments() != null &&
            !policy.getEligibleDepartments().isEmpty()) {

            if (!policy.getEligibleDepartments().contains(user.getDepartment())) {
                return false;
            }
        }

        // Check user type
        if (policy.getEligibleUserTypes() != null &&
            !policy.getEligibleUserTypes().isEmpty()) {

            if (!policy.getEligibleUserTypes().contains(user.getUserType())) {
                return false;
            }
        }

        // Check manual override (admin disabled for this user)
        AutoEnrollmentUserState userState =
            userStateRepository.findByUserIdAndPolicyId(user.getUserId(),
                policy.getPolicyId());

        if (userState != null && userState.isManualOverride()) {
            return false;
        }

        return true;
    }

    /**
     * Check if user has existing valid certificate for policy
     */
    private Optional<Certificate> checkExistingCertificate(User user,
                                                           AutoEnrollmentPolicy policy) {
        return certificateRepository
            .findByUserIdAndTemplateIdAndStatus(
                user.getUserId(),
                policy.getTemplateId(),
                CertificateStatus.ACTIVE
            )
            .stream()
            .filter(cert -> cert.getNotAfter().isAfter(LocalDateTime.now()))
            .findFirst();
    }

    /**
     * Check if certificate renewal is needed
     */
    private boolean isRenewalNeeded(Certificate cert, AutoEnrollmentPolicy policy) {
        if (!policy.isAutoRenew()) {
            return false;
        }

        LocalDateTime renewalThreshold = LocalDateTime.now()
            .plusDays(policy.getRenewalThresholdDays());

        return cert.getNotAfter().isBefore(renewalThreshold);
    }

    /**
     * Enroll new certificate for user
     */
    private void enrollNewCertificate(User user, AutoEnrollmentPolicy policy,
                                     AutoEnrollmentResult result) {
        try {
            enrollNewCertificateInternal(user, policy);
            result.addSuccess(policy, "Certificate enrolled");

        } catch (Exception e) {
            result.addFailure(policy, e.getMessage());
            throw e;
        }
    }

    private void enrollNewCertificateInternal(User user,
                                             AutoEnrollmentPolicy policy) {
        // 1. Get certificate template
        CertificateTemplate template = templateRepository
            .findById(policy.getTemplateId())
            .orElseThrow(() -> new IllegalStateException("Template not found"));

        // 2. Generate key pair based on policy
        KeyPair keyPair;
        if (policy.getKeyGenerationLocation() == KeyGenLocation.SERVER) {
            keyPair = keyGenService.generateKeyPair(
                policy.getKeyAlgorithm(),
                policy.getKeySize()
            );
        } else {
            throw new UnsupportedOperationException(
                "Client-side key generation requires user interaction");
        }

        // 3. Build Subject DN from user's AD attributes
        String subjectDN = buildSubjectDN(user, template);

        // 4. Generate CSR
        PKCS10CertificationRequest csr = csrService.generateCSR(
            subjectDN,
            keyPair,
            template.getKeyUsage(),
            template.getExtendedKeyUsage(),
            buildSubjectAlternativeNames(user, template)
        );

        // 5. Create certificate request
        CertificateRequest request = certRequestService.createAutoEnrollmentRequest(
            user.getUserId(),
            csr,
            template,
            policy
        );

        // 6. Auto-approve if policy allows
        if (policy.isAutoApprove()) {
            request.setStatus(RequestStatus.APPROVED);
            request.setApprovedAt(LocalDateTime.now());
            request.setApprovedBy("SYSTEM_AUTO_ENROLLMENT");
            certRequestService.save(request);

            auditService.log(AuditAction.CSR_AUTO_APPROVED, user.getUserId(),
                "request", request.getRequestId(), "policy", policy.getPolicyId());
        }

        // 7. Submit to CA
        Certificate certificate = caService.issueCertificate(csr, template);
        certificate.setRequestId(request.getRequestId());
        certificate.setUserId(user.getUserId());
        certificateRepository.save(certificate);

        // 8. Store private key securely (if server-side generation)
        if (policy.getKeyGenerationLocation() == KeyGenLocation.SERVER) {
            keyStorageService.storePrivateKey(
                certificate.getCertificateId(),
                keyPair.getPrivate(),
                user.getUsername() // Encryption password
            );
        }

        // 9. Distribute certificate
        distributionService.distribute(certificate, keyPair.getPrivate(), policy);

        // 10. Update user state
        updateUserState(user, policy, certificate);

        // 11. Notify user
        if (policy.isNotifyOnEnrollment()) {
            notificationService.sendEnrollmentNotification(user, certificate, template);
        }

        auditService.log(AuditAction.AUTO_ENROLLMENT_SUCCESS, user.getUserId(),
            "certificate", certificate.getCertificateId(),
            "policy", policy.getPolicyId());
    }

    /**
     * Renew existing certificate
     */
    private void renewCertificate(User user, Certificate oldCert,
                                 AutoEnrollmentPolicy policy,
                                 AutoEnrollmentResult result) {
        try {
            renewCertificateInternal(user, oldCert, policy);
            result.addSuccess(policy, "Certificate renewed");

        } catch (Exception e) {
            result.addFailure(policy, e.getMessage());
            throw e;
        }
    }

    private void renewCertificateInternal(User user, Certificate oldCert,
                                         AutoEnrollmentPolicy policy) {
        // Generate new key pair
        KeyPair keyPair = keyGenService.generateKeyPair(
            policy.getKeyAlgorithm(),
            policy.getKeySize()
        );

        // Reuse same Subject DN
        PKCS10CertificationRequest csr = csrService.generateCSR(
            oldCert.getSubjectDN(),
            keyPair,
            oldCert.getKeyUsage(),
            oldCert.getExtendedKeyUsage(),
            oldCert.getSubjectAlternativeNames()
        );

        // Submit renewal
        Certificate newCert = caService.issueCertificate(csr,
            templateRepository.findById(policy.getTemplateId()).get());

        newCert.setUserId(user.getUserId());
        newCert.setPreviousCertificateId(oldCert.getCertificateId());
        certificateRepository.save(newCert);

        // Store private key
        if (policy.getKeyGenerationLocation() == KeyGenLocation.SERVER) {
            keyStorageService.storePrivateKey(
                newCert.getCertificateId(),
                keyPair.getPrivate(),
                user.getUsername()
            );
        }

        // Distribute
        distributionService.distribute(newCert, keyPair.getPrivate(), policy);

        // Update user state
        updateUserState(user, policy, newCert);

        // Optionally revoke old certificate
        if (policy.isAutoRevokeOnRenewal()) {
            caService.revokeCertificate(oldCert.getSerialNumber(),
                RevocationReason.SUPERSEDED);
            oldCert.setStatus(CertificateStatus.REVOKED);
            oldCert.setRevocationDate(LocalDateTime.now());
            oldCert.setRevocationReason(RevocationReason.SUPERSEDED.name());
            certificateRepository.save(oldCert);
        }

        // Notify
        if (policy.isNotifyOnRenewal()) {
            notificationService.sendRenewalNotification(user, newCert);
        }

        auditService.log(AuditAction.AUTO_RENEWAL_SUCCESS, user.getUserId(),
            "old_cert", oldCert.getCertificateId(),
            "new_cert", newCert.getCertificateId());
    }

    /**
     * Build Subject DN from user's AD attributes and template
     */
    private String buildSubjectDN(User user, CertificateTemplate template) {
        // Template example: "CN=${displayName}, E=${mail}, OU=${department}, O=Company, C=US"
        String dnTemplate = template.getSubjectDnTemplate();

        return dnTemplate
            .replace("${displayName}", user.getDisplayName())
            .replace("${username}", user.getUsername())
            .replace("${mail}", user.getEmail())
            .replace("${department}", user.getDepartment())
            .replace("${employeeId}", user.getEmployeeId());
    }

    /**
     * Build Subject Alternative Names
     */
    private List<String> buildSubjectAlternativeNames(User user,
                                                      CertificateTemplate template) {
        List<String> sans = new ArrayList<>();

        // Template example: ["email:${mail}", "UPN:${userPrincipalName}"]
        for (String sanTemplate : template.getSanTemplate()) {
            String san = sanTemplate
                .replace("${mail}", user.getEmail())
                .replace("${userPrincipalName}", user.getUsername());
            sans.add(san);
        }

        return sans;
    }

    /**
     * Update auto-enrollment user state
     */
    private void updateUserState(User user, AutoEnrollmentPolicy policy,
                                Certificate certificate) {
        AutoEnrollmentUserState state = userStateRepository
            .findByUserIdAndPolicyId(user.getUserId(), policy.getPolicyId())
            .orElse(new AutoEnrollmentUserState(user.getUserId(), policy.getPolicyId()));

        state.setCurrentCertificateId(certificate.getCertificateId());
        state.setLastEnrollmentDate(LocalDateTime.now());
        state.setNextRenewalDate(certificate.getNotAfter()
            .minusDays(policy.getRenewalThresholdDays()));
        state.setEnrollmentCount(state.getEnrollmentCount() + 1);
        state.setUpdatedAt(LocalDateTime.now());

        userStateRepository.save(state);
    }

    /**
     * Get eligible users for a policy (for scheduled jobs)
     */
    private List<User> getEligibleUsers(AutoEnrollmentPolicy policy) {
        // Query AD for users in eligible groups
        Set<String> eligibleUsers = new HashSet<>();

        if (policy.getEligibleAdGroups() != null) {
            for (String groupDN : policy.getEligibleAdGroups()) {
                Set<String> groupMembers = adService.getGroupMembers(groupDN);
                eligibleUsers.addAll(groupMembers);
            }
        }

        // If no groups specified, get all active users
        if (eligibleUsers.isEmpty()) {
            return userRepository.findByIsActive(true);
        }

        // Convert usernames to User objects
        return userRepository.findByUsernameIn(eligibleUsers);
    }

    /**
     * Create enrollment job record
     */
    private AutoEnrollmentJob createJob(AutoEnrollmentPolicy policy,
                                       EnrollmentTrigger trigger) {
        AutoEnrollmentJob job = new AutoEnrollmentJob();
        job.setPolicyId(policy.getPolicyId());
        job.setJobType(JobType.ENROLLMENT);
        job.setTrigger(trigger.name());
        job.setStatus(JobStatus.PENDING);
        return jobRepository.save(job);
    }
}

/**
 * Result object for auto-enrollment operations
 */
@Data
public class AutoEnrollmentResult {
    private List<PolicyResult> successes = new ArrayList<>();
    private List<PolicyResult> failures = new ArrayList<>();
    private List<PolicyResult> skipped = new ArrayList<>();

    public void addSuccess(AutoEnrollmentPolicy policy, String message) {
        successes.add(new PolicyResult(policy.getPolicyId(), policy.getPolicyName(), message));
    }

    public void addFailure(AutoEnrollmentPolicy policy, String error) {
        failures.add(new PolicyResult(policy.getPolicyId(), policy.getPolicyName(), error));
    }

    public void addSkipped(AutoEnrollmentPolicy policy, String reason) {
        skipped.add(new PolicyResult(policy.getPolicyId(), policy.getPolicyName(), reason));
    }

    public static AutoEnrollmentResult noAction(String reason) {
        AutoEnrollmentResult result = new AutoEnrollmentResult();
        result.addSkipped(null, reason);
        return result;
    }

    @Data
    @AllArgsConstructor
    public static class PolicyResult {
        private UUID policyId;
        private String policyName;
        private String message;
    }
}

// Enums
public enum EnrollmentTrigger {
    LOGIN, SCHEDULED, GROUP_CHANGE, MANUAL
}

public enum KeyGenLocation {
    CLIENT, SERVER, HSM
}

public enum JobStatus {
    PENDING, RUNNING, COMPLETED, FAILED
}

public enum JobType {
    ENROLLMENT, RENEWAL, REVOCATION
}
```

#### 1.4.2 Authentication Filter Integration

```java
package com.company.ra.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Trigger auto-enrollment on successful authentication
 */
@Component
public class AutoEnrollmentAuthenticationHandler implements AuthenticationSuccessHandler {

    @Autowired
    private AutoEnrollmentService autoEnrollmentService;

    @Autowired
    private UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                       HttpServletResponse response,
                                       Authentication authentication)
            throws IOException {

        // Get authenticated user
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalStateException("User not found"));

        // Trigger auto-enrollment asynchronously (non-blocking)
        CompletableFuture.runAsync(() -> {
            try {
                AutoEnrollmentResult result = autoEnrollmentService.processUserLogin(user);

                // Store result in session for dashboard display
                request.getSession().setAttribute("autoEnrollmentResult", result);

            } catch (Exception e) {
                // Log error but don't fail authentication
                log.error("Auto-enrollment failed for user: " + username, e);
            }
        });

        // Continue with normal authentication flow
        response.sendRedirect("/dashboard");
    }
}
```

#### 1.4.3 REST API Endpoints

```java
package com.company.ra.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auto-enrollment")
public class AutoEnrollmentController {

    @Autowired
    private AutoEnrollmentService autoEnrollmentService;

    @Autowired
    private AutoEnrollmentPolicyRepository policyRepository;

    @Autowired
    private AutoEnrollmentJobRepository jobRepository;

    /**
     * Get all auto-enrollment policies
     */
    @GetMapping("/policies")
    @PreAuthorize("hasAnyRole('RA_ADMIN', 'RA_OFFICER')")
    public ResponseEntity<List<AutoEnrollmentPolicy>> getAllPolicies() {
        return ResponseEntity.ok(policyRepository.findAll());
    }

    /**
     * Get specific policy
     */
    @GetMapping("/policies/{policyId}")
    @PreAuthorize("hasAnyRole('RA_ADMIN', 'RA_OFFICER')")
    public ResponseEntity<AutoEnrollmentPolicy> getPolicy(@PathVariable UUID policyId) {
        return policyRepository.findById(policyId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create new auto-enrollment policy
     */
    @PostMapping("/policies")
    @PreAuthorize("hasRole('RA_ADMIN')")
    public ResponseEntity<AutoEnrollmentPolicy> createPolicy(
            @RequestBody AutoEnrollmentPolicy policy,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userRepository.findByUsername(userDetails.getUsername()).get();
        policy.setCreatedBy(user.getUserId());

        AutoEnrollmentPolicy saved = policyRepository.save(policy);

        auditService.log(AuditAction.AUTO_ENROLLMENT_POLICY_CREATED,
            user.getUserId(), "policy", saved.getPolicyId());

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * Update policy
     */
    @PutMapping("/policies/{policyId}")
    @PreAuthorize("hasRole('RA_ADMIN')")
    public ResponseEntity<AutoEnrollmentPolicy> updatePolicy(
            @PathVariable UUID policyId,
            @RequestBody AutoEnrollmentPolicy policy,
            @AuthenticationPrincipal UserDetails userDetails) {

        return policyRepository.findById(policyId)
            .map(existing -> {
                User user = userRepository.findByUsername(userDetails.getUsername()).get();

                // Update fields
                existing.setPolicyName(policy.getPolicyName());
                existing.setEnabled(policy.isEnabled());
                existing.setEligibleAdGroups(policy.getEligibleAdGroups());
                existing.setAutoApprove(policy.isAutoApprove());
                existing.setAutoRenew(policy.isAutoRenew());
                existing.setRenewalThresholdDays(policy.getRenewalThresholdDays());
                existing.setUpdatedBy(user.getUserId());
                existing.setUpdatedAt(LocalDateTime.now());

                AutoEnrollmentPolicy updated = policyRepository.save(existing);

                auditService.log(AuditAction.AUTO_ENROLLMENT_POLICY_UPDATED,
                    user.getUserId(), "policy", policyId);

                return ResponseEntity.ok(updated);
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Delete policy
     */
    @DeleteMapping("/policies/{policyId}")
    @PreAuthorize("hasRole('RA_ADMIN')")
    public ResponseEntity<Void> deletePolicy(@PathVariable UUID policyId,
                                            @AuthenticationPrincipal UserDetails userDetails) {

        return policyRepository.findById(policyId)
            .map(policy -> {
                policyRepository.delete(policy);

                User user = userRepository.findByUsername(userDetails.getUsername()).get();
                auditService.log(AuditAction.AUTO_ENROLLMENT_POLICY_DELETED,
                    user.getUserId(), "policy", policyId);

                return ResponseEntity.noContent().<Void>build();
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Manually trigger auto-enrollment for specific user
     */
    @PostMapping("/users/{userId}/enroll")
    @PreAuthorize("hasAnyRole('RA_ADMIN', 'RA_OFFICER')")
    public ResponseEntity<AutoEnrollmentResult> triggerEnrollment(
            @PathVariable UUID userId,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User not found"));

        AutoEnrollmentResult result = autoEnrollmentService.processUserLogin(user);

        User admin = userRepository.findByUsername(userDetails.getUsername()).get();
        auditService.log(AuditAction.AUTO_ENROLLMENT_MANUAL_TRIGGER,
            admin.getUserId(), "target_user", userId);

        return ResponseEntity.ok(result);
    }

    /**
     * Trigger bulk auto-enrollment for a policy
     */
    @PostMapping("/policies/{policyId}/enroll-bulk")
    @PreAuthorize("hasAnyRole('RA_ADMIN', 'RA_OFFICER')")
    public ResponseEntity<AutoEnrollmentJob> triggerBulkEnrollment(
            @PathVariable UUID policyId,
            @AuthenticationPrincipal UserDetails userDetails) {

        AutoEnrollmentPolicy policy = policyRepository.findById(policyId)
            .orElseThrow(() -> new NotFoundException("Policy not found"));

        // Create job
        AutoEnrollmentJob job = autoEnrollmentService.createManualJob(policy);

        // Trigger asynchronously
        CompletableFuture.runAsync(() -> {
            autoEnrollmentService.processScheduledEnrollment(policy, job);
        });

        User admin = userRepository.findByUsername(userDetails.getUsername()).get();
        auditService.log(AuditAction.AUTO_ENROLLMENT_BULK_TRIGGERED,
            admin.getUserId(), "policy", policyId, "job", job.getJobId());

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(job);
    }

    /**
     * Get enrollment jobs
     */
    @GetMapping("/jobs")
    @PreAuthorize("hasAnyRole('RA_ADMIN', 'RA_OFFICER')")
    public ResponseEntity<List<AutoEnrollmentJob>> getJobs(
            @RequestParam(required = false) UUID policyId,
            @RequestParam(required = false) JobStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<AutoEnrollmentJob> jobs;
        if (policyId != null && status != null) {
            jobs = jobRepository.findByPolicyIdAndStatus(policyId, status, pageable);
        } else if (policyId != null) {
            jobs = jobRepository.findByPolicyId(policyId, pageable);
        } else if (status != null) {
            jobs = jobRepository.findByStatus(status, pageable);
        } else {
            jobs = jobRepository.findAll(pageable);
        }

        return ResponseEntity.ok(jobs.getContent());
    }

    /**
     * Get job details
     */
    @GetMapping("/jobs/{jobId}")
    @PreAuthorize("hasAnyRole('RA_ADMIN', 'RA_OFFICER')")
    public ResponseEntity<AutoEnrollmentJob> getJob(@PathVariable UUID jobId) {
        return jobRepository.findById(jobId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get user's auto-enrollment state
     */
    @GetMapping("/users/{userId}/state")
    @PreAuthorize("hasAnyRole('RA_ADMIN', 'RA_OFFICER') or #userId == authentication.principal.userId")
    public ResponseEntity<List<AutoEnrollmentUserState>> getUserState(@PathVariable UUID userId) {
        List<AutoEnrollmentUserState> states = userStateRepository.findByUserId(userId);
        return ResponseEntity.ok(states);
    }

    /**
     * Disable auto-enrollment for specific user (manual override)
     */
    @PostMapping("/users/{userId}/policies/{policyId}/disable")
    @PreAuthorize("hasRole('RA_ADMIN')")
    public ResponseEntity<Void> disableAutoEnrollment(
            @PathVariable UUID userId,
            @PathVariable UUID policyId,
            @AuthenticationPrincipal UserDetails userDetails) {

        AutoEnrollmentUserState state = userStateRepository
            .findByUserIdAndPolicyId(userId, policyId)
            .orElse(new AutoEnrollmentUserState(userId, policyId));

        state.setManualOverride(true);
        state.setAutoEnrollmentEnabled(false);
        userStateRepository.save(state);

        User admin = userRepository.findByUsername(userDetails.getUsername()).get();
        auditService.log(AuditAction.AUTO_ENROLLMENT_DISABLED,
            admin.getUserId(), "user", userId, "policy", policyId);

        return ResponseEntity.noContent().build();
    }

    /**
     * Enable auto-enrollment for user
     */
    @PostMapping("/users/{userId}/policies/{policyId}/enable")
    @PreAuthorize("hasRole('RA_ADMIN')")
    public ResponseEntity<Void> enableAutoEnrollment(
            @PathVariable UUID userId,
            @PathVariable UUID policyId,
            @AuthenticationPrincipal UserDetails userDetails) {

        AutoEnrollmentUserState state = userStateRepository
            .findByUserIdAndPolicyId(userId, policyId)
            .orElse(new AutoEnrollmentUserState(userId, policyId));

        state.setManualOverride(false);
        state.setAutoEnrollmentEnabled(true);
        userStateRepository.save(state);

        User admin = userRepository.findByUsername(userDetails.getUsername()).get();
        auditService.log(AuditAction.AUTO_ENROLLMENT_ENABLED,
            admin.getUserId(), "user", userId, "policy", policyId);

        return ResponseEntity.noContent().build();
    }
}
```

### 1.5 Certificate Distribution Service

```java
package com.company.ra.service.distribution;

import org.springframework.stereotype.Service;
import java.security.PrivateKey;
import java.util.List;

@Service
public class CertificateDistributionService {

    @Autowired
    private EmailService emailService;

    @Autowired
    private WebDownloadService webDownloadService;

    @Autowired
    private LdapPublishService ldapPublishService;

    /**
     * Distribute certificate based on policy configuration
     */
    public void distribute(Certificate certificate, PrivateKey privateKey,
                          AutoEnrollmentPolicy policy) {

        User user = userRepository.findById(certificate.getUserId()).get();
        List<String> methods = policy.getDistributionMethods();

        for (String method : methods) {
            switch (method) {
                case "WEB_DOWNLOAD":
                    makeAvailableForWebDownload(certificate, privateKey, user);
                    break;

                case "EMAIL":
                    sendViaEmail(certificate, privateKey, user);
                    break;

                case "LDAP":
                    publishToLdap(certificate, user);
                    break;

                case "CERT_STORE":
                    // Requires client agent
                    notifyClientAgent(certificate, user);
                    break;

                default:
                    log.warn("Unknown distribution method: " + method);
            }
        }
    }

    /**
     * Web download - make available on RA portal
     */
    private void makeAvailableForWebDownload(Certificate certificate,
                                            PrivateKey privateKey, User user) {
        // Certificate already in database, user can download via portal

        // Create PKCS#12 file
        byte[] p12 = pkcs12Service.createPKCS12(
            certificate,
            privateKey,
            user.getUsername() // Use username as password
        );

        // Store in secure temporary location
        webDownloadService.storePKCS12(certificate.getCertificateId(), p12);

        // Mark as available
        certificate.setDownloadAvailable(true);
        certificateRepository.save(certificate);
    }

    /**
     * Email distribution
     */
    private void sendViaEmail(Certificate certificate, PrivateKey privateKey, User user) {
        // Create password-protected PKCS#12
        String password = generateSecurePassword();
        byte[] p12 = pkcs12Service.createPKCS12(certificate, privateKey, password);

        // Send email with attachment
        emailService.sendCertificateEmail(
            user.getEmail(),
            "Your Certificate is Ready",
            buildEmailBody(user, certificate, password),
            new EmailAttachment("certificate.p12", p12)
        );
    }

    /**
     * Publish to LDAP/AD
     */
    private void publishToLdap(Certificate certificate, User user) {
        ldapPublishService.publishCertificate(user.getAdDn(), certificate);
    }

    /**
     * Notify client agent (for automatic installation)
     */
    private void notifyClientAgent(Certificate certificate, User user) {
        // Send push notification to client agent
        // Agent will download and install certificate
        clientAgentService.notifyCertificateAvailable(user, certificate);
    }

    private String buildEmailBody(User user, Certificate certificate, String password) {
        return String.format("""
            Dear %s,

            Your certificate has been successfully issued and is attached to this email.

            Certificate Details:
            - Subject: %s
            - Serial Number: %s
            - Valid From: %s
            - Valid Until: %s

            The attached PKCS#12 file contains both your certificate and private key.

            Password to import: %s

            Please import this certificate into your browser or email client.

            Best regards,
            Certificate Authority Team
            """,
            user.getDisplayName(),
            certificate.getSubjectDN(),
            certificate.getSerialNumber(),
            certificate.getNotBefore(),
            certificate.getNotAfter(),
            password
        );
    }

    private String generateSecurePassword() {
        SecureRandom random = new SecureRandom();
        return new BigInteger(130, random).toString(32).substring(0, 12);
    }
}
```

### 1.6 Frontend Implementation (React)

#### 1.6.1 Auto-Enrollment Policy Management UI

```typescript
// src/pages/AutoEnrollment/PolicyList.tsx

import React, { useEffect, useState } from 'react';
import { Table, Button, Switch, Tag, Space, Modal, message } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, PlayCircleOutlined } from '@ant-design/icons';
import { Link, useNavigate } from 'react-router-dom';
import { autoEnrollmentApi } from '../../services/api';
import type { AutoEnrollmentPolicy } from '../../types';

export const PolicyList: React.FC = () => {
    const [policies, setPolicies] = useState<AutoEnrollmentPolicy[]>([]);
    const [loading, setLoading] = useState(false);
    const navigate = useNavigate();

    useEffect(() => {
        loadPolicies();
    }, []);

    const loadPolicies = async () => {
        setLoading(true);
        try {
            const response = await autoEnrollmentApi.getAllPolicies();
            setPolicies(response.data);
        } catch (error) {
            message.error('Failed to load policies');
        } finally {
            setLoading(false);
        }
    };

    const togglePolicy = async (policyId: string, enabled: boolean) => {
        try {
            await autoEnrollmentApi.updatePolicy(policyId, { enabled });
            message.success(`Policy ${enabled ? 'enabled' : 'disabled'}`);
            loadPolicies();
        } catch (error) {
            message.error('Failed to update policy');
        }
    };

    const deletePolicy = (policyId: string) => {
        Modal.confirm({
            title: 'Delete Policy',
            content: 'Are you sure you want to delete this policy? This action cannot be undone.',
            okText: 'Delete',
            okType: 'danger',
            onOk: async () => {
                try {
                    await autoEnrollmentApi.deletePolicy(policyId);
                    message.success('Policy deleted');
                    loadPolicies();
                } catch (error) {
                    message.error('Failed to delete policy');
                }
            },
        });
    };

    const triggerBulkEnrollment = async (policyId: string) => {
        Modal.confirm({
            title: 'Trigger Bulk Enrollment',
            content: 'This will process auto-enrollment for all eligible users. Continue?',
            okText: 'Trigger',
            onOk: async () => {
                try {
                    const response = await autoEnrollmentApi.triggerBulkEnrollment(policyId);
                    message.success(`Enrollment job started: ${response.data.jobId}`);
                    navigate(`/auto-enrollment/jobs/${response.data.jobId}`);
                } catch (error) {
                    message.error('Failed to trigger enrollment');
                }
            },
        });
    };

    const columns = [
        {
            title: 'Policy Name',
            dataIndex: 'policyName',
            key: 'policyName',
            render: (text: string, record: AutoEnrollmentPolicy) => (
                <Link to={`/auto-enrollment/policies/${record.policyId}`}>{text}</Link>
            ),
        },
        {
            title: 'Certificate Template',
            dataIndex: 'templateName',
            key: 'templateName',
        },
        {
            title: 'Trigger',
            dataIndex: 'enrollmentTrigger',
            key: 'enrollmentTrigger',
            render: (trigger: string) => (
                <Tag color={trigger === 'LOGIN' ? 'blue' : 'green'}>{trigger}</Tag>
            ),
        },
        {
            title: 'Auto-Approve',
            dataIndex: 'autoApprove',
            key: 'autoApprove',
            render: (autoApprove: boolean) => (
                <Tag color={autoApprove ? 'success' : 'default'}>
                    {autoApprove ? 'Yes' : 'No'}
                </Tag>
            ),
        },
        {
            title: 'Auto-Renew',
            dataIndex: 'autoRenew',
            key: 'autoRenew',
            render: (autoRenew: boolean) => (
                <Tag color={autoRenew ? 'success' : 'default'}>
                    {autoRenew ? 'Yes' : 'No'}
                </Tag>
            ),
        },
        {
            title: 'Enabled',
            dataIndex: 'enabled',
            key: 'enabled',
            render: (enabled: boolean, record: AutoEnrollmentPolicy) => (
                <Switch
                    checked={enabled}
                    onChange={(checked) => togglePolicy(record.policyId, checked)}
                />
            ),
        },
        {
            title: 'Actions',
            key: 'actions',
            render: (_: any, record: AutoEnrollmentPolicy) => (
                <Space>
                    <Button
                        type="link"
                        icon={<EditOutlined />}
                        onClick={() => navigate(`/auto-enrollment/policies/${record.policyId}/edit`)}
                    >
                        Edit
                    </Button>
                    <Button
                        type="link"
                        icon={<PlayCircleOutlined />}
                        onClick={() => triggerBulkEnrollment(record.policyId)}
                    >
                        Trigger
                    </Button>
                    <Button
                        type="link"
                        danger
                        icon={<DeleteOutlined />}
                        onClick={() => deletePolicy(record.policyId)}
                    >
                        Delete
                    </Button>
                </Space>
            ),
        },
    ];

    return (
        <div>
            <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between' }}>
                <h1>Auto-Enrollment Policies</h1>
                <Button
                    type="primary"
                    icon={<PlusOutlined />}
                    onClick={() => navigate('/auto-enrollment/policies/new')}
                >
                    Create Policy
                </Button>
            </div>

            <Table
                columns={columns}
                dataSource={policies}
                rowKey="policyId"
                loading={loading}
                pagination={{ pageSize: 20 }}
            />
        </div>
    );
};
```

#### 1.6.2 Policy Create/Edit Form

```typescript
// src/pages/AutoEnrollment/PolicyForm.tsx

import React, { useEffect, useState } from 'react';
import {
    Form, Input, Select, Switch, InputNumber, Button, Card, Space,
    message, Row, Col, Tag,
} from 'antd';
import { SaveOutlined, ArrowLeftOutlined } from '@ant-design/icons';
import { useNavigate, useParams } from 'react-router-dom';
import { autoEnrollmentApi, templateApi, adService } from '../../services/api';
import type { AutoEnrollmentPolicy, CertificateTemplate } from '../../types';

const { Option } = Select;

export const PolicyForm: React.FC = () => {
    const [form] = Form.useForm();
    const [loading, setLoading] = useState(false);
    const [templates, setTemplates] = useState<CertificateTemplate[]>([]);
    const [adGroups, setAdGroups] = useState<string[]>([]);
    const navigate = useNavigate();
    const { policyId } = useParams<{ policyId: string }>();
    const isEdit = !!policyId;

    useEffect(() => {
        loadTemplates();
        loadADGroups();
        if (isEdit) {
            loadPolicy();
        }
    }, [policyId]);

    const loadTemplates = async () => {
        try {
            const response = await templateApi.getAll();
            setTemplates(response.data);
        } catch (error) {
            message.error('Failed to load templates');
        }
    };

    const loadADGroups = async () => {
        try {
            const response = await adService.getAllGroups();
            setAdGroups(response.data);
        } catch (error) {
            message.error('Failed to load AD groups');
        }
    };

    const loadPolicy = async () => {
        setLoading(true);
        try {
            const response = await autoEnrollmentApi.getPolicy(policyId!);
            form.setFieldsValue(response.data);
        } catch (error) {
            message.error('Failed to load policy');
        } finally {
            setLoading(false);
        }
    };

    const onFinish = async (values: any) => {
        setLoading(true);
        try {
            if (isEdit) {
                await autoEnrollmentApi.updatePolicy(policyId!, values);
                message.success('Policy updated successfully');
            } else {
                await autoEnrollmentApi.createPolicy(values);
                message.success('Policy created successfully');
            }
            navigate('/auto-enrollment/policies');
        } catch (error) {
            message.error('Failed to save policy');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div>
            <Button
                icon={<ArrowLeftOutlined />}
                onClick={() => navigate('/auto-enrollment/policies')}
                style={{ marginBottom: 16 }}
            >
                Back to Policies
            </Button>

            <Card title={isEdit ? 'Edit Auto-Enrollment Policy' : 'Create Auto-Enrollment Policy'}>
                <Form
                    form={form}
                    layout="vertical"
                    onFinish={onFinish}
                    initialValues={{
                        enabled: true,
                        autoApprove: false,
                        autoRenew: true,
                        renewalThresholdDays: 30,
                        enrollmentTrigger: 'LOGIN',
                        keyGenerationLocation: 'SERVER',
                        keyAlgorithm: 'RSA',
                        keySize: 2048,
                        distributionMethods: ['WEB_DOWNLOAD'],
                        notifyOnEnrollment: true,
                        notifyOnRenewal: false,
                    }}
                >
                    <Row gutter={16}>
                        <Col span={12}>
                            <Form.Item
                                label="Policy Name"
                                name="policyName"
                                rules={[{ required: true, message: 'Please enter policy name' }]}
                            >
                                <Input placeholder="e.g., Email Certificate Auto-Enrollment" />
                            </Form.Item>
                        </Col>

                        <Col span={12}>
                            <Form.Item
                                label="Certificate Template"
                                name="templateId"
                                rules={[{ required: true, message: 'Please select template' }]}
                            >
                                <Select placeholder="Select certificate template">
                                    {templates.map((template) => (
                                        <Option key={template.templateId} value={template.templateId}>
                                            {template.templateName}
                                        </Option>
                                    ))}
                                </Select>
                            </Form.Item>
                        </Col>
                    </Row>

                    <Row gutter={16}>
                        <Col span={12}>
                            <Form.Item
                                label="Enrollment Trigger"
                                name="enrollmentTrigger"
                                rules={[{ required: true }]}
                            >
                                <Select>
                                    <Option value="LOGIN">On Login</Option>
                                    <Option value="SCHEDULED">Scheduled</Option>
                                    <Option value="GROUP_CHANGE">On Group Change</Option>
                                </Select>
                            </Form.Item>
                        </Col>

                        <Col span={12}>
                            <Form.Item label="Enabled" name="enabled" valuePropName="checked">
                                <Switch />
                            </Form.Item>
                        </Col>
                    </Row>

                    <Card title="Eligibility Criteria" style={{ marginBottom: 16 }} size="small">
                        <Form.Item
                            label="Eligible AD Groups"
                            name="eligibleAdGroups"
                            tooltip="Users must be members of these groups to be eligible"
                        >
                            <Select
                                mode="multiple"
                                placeholder="Select AD groups"
                                showSearch
                                filterOption={(input, option) =>
                                    (option?.children as string).toLowerCase().indexOf(input.toLowerCase()) >= 0
                                }
                            >
                                {adGroups.map((group) => (
                                    <Option key={group} value={group}>
                                        {group}
                                    </Option>
                                ))}
                            </Select>
                        </Form.Item>

                        <Form.Item label="Eligible Departments" name="eligibleDepartments">
                            <Select
                                mode="tags"
                                placeholder="Enter departments (e.g., IT, Engineering)"
                            />
                        </Form.Item>

                        <Form.Item label="Eligible User Types" name="eligibleUserTypes">
                            <Select mode="multiple" placeholder="Select user types">
                                <Option value="EMPLOYEE">Employee</Option>
                                <Option value="CONTRACTOR">Contractor</Option>
                                <Option value="SERVICE_ACCOUNT">Service Account</Option>
                            </Select>
                        </Form.Item>
                    </Card>

                    <Card title="Enrollment Behavior" style={{ marginBottom: 16 }} size="small">
                        <Row gutter={16}>
                            <Col span={8}>
                                <Form.Item
                                    label="Auto-Approve"
                                    name="autoApprove"
                                    valuePropName="checked"
                                    tooltip="Skip RA Officer approval for automatic issuance"
                                >
                                    <Switch />
                                </Form.Item>
                            </Col>

                            <Col span={8}>
                                <Form.Item
                                    label="Auto-Renew"
                                    name="autoRenew"
                                    valuePropName="checked"
                                    tooltip="Automatically renew certificates before expiry"
                                >
                                    <Switch />
                                </Form.Item>
                            </Col>

                            <Col span={8}>
                                <Form.Item
                                    label="Renewal Threshold (Days)"
                                    name="renewalThresholdDays"
                                    tooltip="Renew certificate N days before expiration"
                                >
                                    <InputNumber min={1} max={365} style={{ width: '100%' }} />
                                </Form.Item>
                            </Col>
                        </Row>

                        <Form.Item
                            label="Auto-Revoke on User Disable"
                            name="autoRevokeOnUserDisable"
                            valuePropName="checked"
                        >
                            <Switch />
                        </Form.Item>
                    </Card>

                    <Card title="Key Generation" style={{ marginBottom: 16 }} size="small">
                        <Row gutter={16}>
                            <Col span={8}>
                                <Form.Item
                                    label="Key Generation Location"
                                    name="keyGenerationLocation"
                                >
                                    <Select>
                                        <Option value="SERVER">Server-Side</Option>
                                        <Option value="CLIENT">Client-Side (requires agent)</Option>
                                        <Option value="HSM">HSM</Option>
                                    </Select>
                                </Form.Item>
                            </Col>

                            <Col span={8}>
                                <Form.Item label="Key Algorithm" name="keyAlgorithm">
                                    <Select>
                                        <Option value="RSA">RSA</Option>
                                        <Option value="ECDSA">ECDSA</Option>
                                    </Select>
                                </Form.Item>
                            </Col>

                            <Col span={8}>
                                <Form.Item label="Key Size" name="keySize">
                                    <Select>
                                        <Option value={2048}>2048</Option>
                                        <Option value={3072}>3072</Option>
                                        <Option value={4096}>4096</Option>
                                    </Select>
                                </Form.Item>
                            </Col>
                        </Row>
                    </Card>

                    <Card title="Distribution & Notifications" style={{ marginBottom: 16 }} size="small">
                        <Form.Item label="Distribution Methods" name="distributionMethods">
                            <Select mode="multiple">
                                <Option value="WEB_DOWNLOAD">Web Download</Option>
                                <Option value="EMAIL">Email</Option>
                                <Option value="CERT_STORE">Certificate Store (via agent)</Option>
                                <Option value="LDAP">LDAP/AD Publish</Option>
                            </Select>
                        </Form.Item>

                        <Row gutter={16}>
                            <Col span={12}>
                                <Form.Item
                                    label="Notify on Enrollment"
                                    name="notifyOnEnrollment"
                                    valuePropName="checked"
                                >
                                    <Switch />
                                </Form.Item>
                            </Col>

                            <Col span={12}>
                                <Form.Item
                                    label="Notify on Renewal"
                                    name="notifyOnRenewal"
                                    valuePropName="checked"
                                >
                                    <Switch />
                                </Form.Item>
                            </Col>
                        </Row>
                    </Card>

                    <Form.Item>
                        <Space>
                            <Button type="primary" htmlType="submit" icon={<SaveOutlined />} loading={loading}>
                                {isEdit ? 'Update Policy' : 'Create Policy'}
                            </Button>
                            <Button onClick={() => navigate('/auto-enrollment/policies')}>
                                Cancel
                            </Button>
                        </Space>
                    </Form.Item>
                </Form>
            </Card>
        </div>
    );
};
```

### 1.7 User Dashboard - Auto-Enrollment Status

```typescript
// src/components/Dashboard/AutoEnrollmentStatus.tsx

import React, { useEffect, useState } from 'react';
import { Card, List, Tag, Button, Space, Alert } from 'antd';
import { CheckCircleOutlined, SyncOutlined, DownloadOutlined } from '@ant-design/icons';
import { autoEnrollmentApi, certificateApi } from '../../services/api';
import type { AutoEnrollmentUserState } from '../../types';

export const AutoEnrollmentStatus: React.FC = () => {
    const [enrollmentStates, setEnrollmentStates] = useState<AutoEnrollmentUserState[]>([]);
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        loadEnrollmentStatus();
    }, []);

    const loadEnrollmentStatus = async () => {
        setLoading(true);
        try {
            const response = await autoEnrollmentApi.getMyEnrollmentState();
            setEnrollmentStates(response.data);
        } catch (error) {
            console.error('Failed to load enrollment status', error);
        } finally {
            setLoading(false);
        }
    };

    const downloadCertificate = async (certificateId: string) => {
        try {
            const response = await certificateApi.downloadPKCS12(certificateId);
            // Trigger download
            const url = window.URL.createObjectURL(new Blob([response.data]));
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', `certificate_${certificateId}.p12`);
            document.body.appendChild(link);
            link.click();
            link.remove();
        } catch (error) {
            console.error('Failed to download certificate', error);
        }
    };

    return (
        <Card title="Auto-Enrollment Status" loading={loading}>
            {enrollmentStates.length === 0 ? (
                <Alert
                    message="No auto-enrollment policies apply to you"
                    description="Contact your administrator if you need a certificate."
                    type="info"
                    showIcon
                />
            ) : (
                <List
                    dataSource={enrollmentStates}
                    renderItem={(state) => (
                        <List.Item
                            actions={[
                                state.currentCertificateId && (
                                    <Button
                                        type="link"
                                        icon={<DownloadOutlined />}
                                        onClick={() => downloadCertificate(state.currentCertificateId!)}
                                    >
                                        Download
                                    </Button>
                                ),
                            ]}
                        >
                            <List.Item.Meta
                                avatar={
                                    state.currentCertificateId ? (
                                        <CheckCircleOutlined style={{ fontSize: 24, color: '#52c41a' }} />
                                    ) : (
                                        <SyncOutlined spin style={{ fontSize: 24, color: '#1890ff' }} />
                                    )
                                }
                                title={state.policyName}
                                description={
                                    <Space direction="vertical" size="small">
                                        <div>
                                            <Tag color={state.currentCertificateId ? 'success' : 'processing'}>
                                                {state.currentCertificateId ? 'Enrolled' : 'Pending'}
                                            </Tag>
                                        </div>
                                        {state.lastEnrollmentDate && (
                                            <div>Last Enrolled: {new Date(state.lastEnrollmentDate).toLocaleDateString()}</div>
                                        )}
                                        {state.nextRenewalDate && (
                                            <div>Next Renewal: {new Date(state.nextRenewalDate).toLocaleDateString()}</div>
                                        )}
                                    </Space>
                                }
                            />
                        </List.Item>
                    )}
                />
            )}
        </Card>
    );
};
```

---

## 2. Scheduled Enrollment Implementation

### 2.1 Overview
Scheduled enrollment executes certificate provisioning at predetermined times using cron-like scheduling, independent of user login events.

### 2.2 Scheduling Configuration

```yaml
# application.yml - Spring Boot Configuration

spring:
  task:
    scheduling:
      pool:
        size: 10 # Thread pool for scheduled tasks

auto-enrollment:
  scheduler:
    enabled: true
    default-cron: "0 0 2 * * ?" # 2 AM daily
    max-concurrent-jobs: 5
```

### 2.3 Scheduled Job Service

```java
package com.company.ra.service.scheduling;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

@Service
public class ScheduledEnrollmentService {

    @Autowired
    private AutoEnrollmentService autoEnrollmentService;

    @Autowired
    private AutoEnrollmentPolicyRepository policyRepository;

    @Autowired
    private JobExecutionService jobExecutionService;

    /**
     * Master scheduler - runs every hour to check for scheduled policies
     */
    @Scheduled(cron = "0 0 * * * *") // Every hour
    public void checkScheduledPolicies() {
        List<AutoEnrollmentPolicy> scheduledPolicies = policyRepository
            .findByEnabledAndEnrollmentTrigger(true, EnrollmentTrigger.SCHEDULED);

        LocalDateTime now = LocalDateTime.now();

        for (AutoEnrollmentPolicy policy : scheduledPolicies) {
            if (shouldExecuteNow(policy, now)) {
                executeScheduledEnrollment(policy);
            }
        }
    }

    /**
     * Check if policy should execute based on its schedule
     */
    private boolean shouldExecuteNow(AutoEnrollmentPolicy policy, LocalDateTime now) {
        if (policy.getEnrollmentSchedule() == null) {
            return false;
        }

        // Parse cron expression from policy
        String cronExpr = policy.getEnrollmentSchedule().get("cron").asText();
        CronExpression cron = CronExpression.parse(cronExpr);

        // Check if current time matches cron expression
        LocalDateTime lastExecution = getLastExecutionTime(policy.getPolicyId());
        LocalDateTime nextExecution = cron.next(lastExecution);

        return nextExecution != null && !nextExecution.isAfter(now);
    }

    /**
     * Execute scheduled enrollment for a policy
     */
    private void executeScheduledEnrollment(AutoEnrollmentPolicy policy) {
        // Check if already running
        if (jobExecutionService.isRunning(policy.getPolicyId())) {
            log.warn("Enrollment job already running for policy: {}", policy.getPolicyId());
            return;
        }

        AutoEnrollmentJob job = createJob(policy, EnrollmentTrigger.SCHEDULED);

        // Execute asynchronously
        CompletableFuture.runAsync(() -> {
            try {
                autoEnrollmentService.processScheduledEnrollment(policy, job);
            } catch (Exception e) {
                log.error("Scheduled enrollment failed for policy: {}", policy.getPolicyId(), e);
                job.setStatus(JobStatus.FAILED);
                job.setErrorLog(e.getMessage());
            } finally {
                job.setCompletedAt(LocalDateTime.now());
                jobRepository.save(job);

                // Update last execution time
                updateLastExecutionTime(policy.getPolicyId(), LocalDateTime.now());
            }
        });
    }

    private LocalDateTime getLastExecutionTime(UUID policyId) {
        return jobRepository
            .findTopByPolicyIdAndStatusOrderByCompletedAtDesc(policyId, JobStatus.COMPLETED)
            .map(AutoEnrollmentJob::getCompletedAt)
            .orElse(LocalDateTime.now().minusYears(1)); // Default to 1 year ago
    }

    private void updateLastExecutionTime(UUID policyId, LocalDateTime time) {
        // Store in cache or database
        executionTimeCache.put(policyId, time);
    }

    private AutoEnrollmentJob createJob(AutoEnrollmentPolicy policy, EnrollmentTrigger trigger) {
        AutoEnrollmentJob job = new AutoEnrollmentJob();
        job.setPolicyId(policy.getPolicyId());
        job.setJobType(JobType.ENROLLMENT);
        job.setTrigger(trigger.name());
        job.setStatus(JobStatus.PENDING);
        return jobRepository.save(job);
    }
}
```

### 2.4 Cron Expression Builder UI

```typescript
// src/components/CronBuilder/CronBuilder.tsx

import React, { useState } from 'react';
import { Input, Select, Radio, InputNumber, Space, Form } from 'antd';
import cronstrue from 'cronstrue';

const { Option } = Select;

interface CronBuilderProps {
    value?: string;
    onChange?: (cron: string) => void;
}

export const CronBuilder: React.FC<CronBuilderProps> = ({ value, onChange }) => {
    const [cronType, setCronType] = useState<'simple' | 'advanced'>('simple');
    const [frequency, setFrequency] = useState<'daily' | 'weekly' | 'monthly'>('daily');
    const [hour, setHour] = useState(2);
    const [minute, setMinute] = useState(0);
    const [dayOfWeek, setDayOfWeek] = useState(1);
    const [dayOfMonth, setDayOfMonth] = useState(1);

    const buildCronExpression = () => {
        let cron = '';

        if (frequency === 'daily') {
            cron = `0 ${minute} ${hour} * * ?`;
        } else if (frequency === 'weekly') {
            cron = `0 ${minute} ${hour} ? * ${dayOfWeek}`;
        } else if (frequency === 'monthly') {
            cron = `0 ${minute} ${hour} ${dayOfMonth} * ?`;
        }

        return cron;
    };

    const handleChange = () => {
        const cron = buildCronExpression();
        onChange?.(cron);
    };

    const currentCron = cronType === 'simple' ? buildCronExpression() : value || '';

    return (
        <div>
            <Radio.Group value={cronType} onChange={(e) => setCronType(e.target.value)} style={{ marginBottom: 16 }}>
                <Radio value="simple">Simple Schedule</Radio>
                <Radio value="advanced">Advanced (Cron Expression)</Radio>
            </Radio.Group>

            {cronType === 'simple' ? (
                <Space direction="vertical" style={{ width: '100%' }}>
                    <Form.Item label="Frequency">
                        <Select value={frequency} onChange={(val) => { setFrequency(val); handleChange(); }}>
                            <Option value="daily">Daily</Option>
                            <Option value="weekly">Weekly</Option>
                            <Option value="monthly">Monthly</Option>
                        </Select>
                    </Form.Item>

                    <Space>
                        <Form.Item label="Hour">
                            <InputNumber
                                min={0}
                                max={23}
                                value={hour}
                                onChange={(val) => { setHour(val!); handleChange(); }}
                            />
                        </Form.Item>

                        <Form.Item label="Minute">
                            <InputNumber
                                min={0}
                                max={59}
                                value={minute}
                                onChange={(val) => { setMinute(val!); handleChange(); }}
                            />
                        </Form.Item>
                    </Space>

                    {frequency === 'weekly' && (
                        <Form.Item label="Day of Week">
                            <Select value={dayOfWeek} onChange={(val) => { setDayOfWeek(val); handleChange(); }}>
                                <Option value={1}>Monday</Option>
                                <Option value={2}>Tuesday</Option>
                                <Option value={3}>Wednesday</Option>
                                <Option value={4}>Thursday</Option>
                                <Option value={5}>Friday</Option>
                                <Option value={6}>Saturday</Option>
                                <Option value={0}>Sunday</Option>
                            </Select>
                        </Form.Item>
                    )}

                    {frequency === 'monthly' && (
                        <Form.Item label="Day of Month">
                            <InputNumber
                                min={1}
                                max={31}
                                value={dayOfMonth}
                                onChange={(val) => { setDayOfMonth(val!); handleChange(); }}
                            />
                        </Form.Item>
                    )}
                </Space>
            ) : (
                <Form.Item label="Cron Expression">
                    <Input
                        value={value}
                        onChange={(e) => onChange?.(e.target.value)}
                        placeholder="0 0 2 * * ?"
                    />
                </Form.Item>
            )}

            <div style={{ marginTop: 16, padding: 12, background: '#f0f0f0', borderRadius: 4 }}>
                <strong>Schedule:</strong> {cronstrue.toString(currentCron)}
            </div>
        </div>
    );
};
```

---

## 3. Bulk Enrollment Implementation

### 3.1 Overview
Bulk enrollment allows administrators to process certificate enrollment for multiple users simultaneously, with progress tracking and error handling.

### 3.2 Bulk Enrollment Service

```java
package com.company.ra.service.bulk;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.concurrent.*;

@Service
public class BulkEnrollmentService {

    @Autowired
    private AutoEnrollmentService autoEnrollmentService;

    @Autowired
    private UserRepository userRepository;

    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    /**
     * Process bulk enrollment for multiple users
     */
    public BulkEnrollmentJob processB ulkEnrollment(List<UUID> userIds,
                                                   AutoEnrollmentPolicy policy) {
        BulkEnrollmentJob job = createBulkJob(userIds.size(), policy);

        CompletableFuture.runAsync(() -> {
            job.setStatus(JobStatus.RUNNING);
            job.setStartedAt(LocalDateTime.now());
            bulkJobRepository.save(job);

            List<CompletableFuture<UserEnrollmentResult>> futures = new ArrayList<>();

            for (UUID userId : userIds) {
                CompletableFuture<UserEnrollmentResult> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        User user = userRepository.findById(userId).orElseThrow();
                        autoEnrollmentService.enrollNewCertificateInternal(user, policy);

                        return UserEnrollmentResult.success(userId);
                    } catch (Exception e) {
                        return UserEnrollmentResult.failure(userId, e.getMessage());
                    }
                }, executorService);

                futures.add(future);
            }

            // Wait for all to complete
            CompletableFuture<Void> allOf = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
            );

            try {
                allOf.get(1, TimeUnit.HOURS); // Timeout after 1 hour

                // Collect results
                int successCount = 0;
                int failureCount = 0;
                List<UserEnrollmentResult> results = new ArrayList<>();

                for (CompletableFuture<UserEnrollmentResult> future : futures) {
                    UserEnrollmentResult result = future.get();
                    results.add(result);

                    if (result.isSuccess()) {
                        successCount++;
                    } else {
                        failureCount++;
                    }
                }

                job.setSuccessCount(successCount);
                job.setFailureCount(failureCount);
                job.setResults(results);
                job.setStatus(JobStatus.COMPLETED);

            } catch (Exception e) {
                job.setStatus(JobStatus.FAILED);
                job.setErrorLog(e.getMessage());
            } finally {
                job.setCompletedAt(LocalDateTime.now());
                bulkJobRepository.save(job);
            }
        });

        return job;
    }

    /**
     * Process bulk enrollment from CSV file
     */
    public BulkEnrollmentJob processBulkEnrollmentFromCSV(MultipartFile csvFile,
                                                         AutoEnrollmentPolicy policy) {
        try {
            List<UUID> userIds = parseCsvFile(csvFile);
            return processBulkEnrollment(userIds, policy);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse CSV file", e);
        }
    }

    private List<UUID> parseCsvFile(MultipartFile file) throws IOException {
        List<UUID> userIds = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream()))) {

            String line;
            boolean isHeader = true;

            while ((line = reader.readLine()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue; // Skip header
                }

                String[] parts = line.split(",");
                if (parts.length > 0) {
                    String username = parts[0].trim();
                    User user = userRepository.findByUsername(username).orElse(null);
                    if (user != null) {
                        userIds.add(user.getUserId());
                    }
                }
            }
        }

        return userIds;
    }

    private BulkEnrollmentJob createBulkJob(int totalUsers, AutoEnrollmentPolicy policy) {
        BulkEnrollmentJob job = new BulkEnrollmentJob();
        job.setPolicyId(policy.getPolicyId());
        job.setTotalUsers(totalUsers);
        job.setStatus(JobStatus.PENDING);
        return bulkJobRepository.save(job);
    }
}

@Data
class UserEnrollmentResult {
    private UUID userId;
    private boolean success;
    private String error;

    public static UserEnrollmentResult success(UUID userId) {
        UserEnrollmentResult result = new UserEnrollmentResult();
        result.setUserId(userId);
        result.setSuccess(true);
        return result;
    }

    public static UserEnrollmentResult failure(UUID userId, String error) {
        UserEnrollmentResult result = new UserEnrollmentResult();
        result.setUserId(userId);
        result.setSuccess(false);
        result.setError(error);
        return result;
    }
}
```

### 3.3 Bulk Enrollment UI

```typescript
// src/pages/AutoEnrollment/BulkEnrollment.tsx

import React, { useState } from 'react';
import { Card, Upload, Button, Table, Progress, Alert, Space, Select, message } from 'antd';
import { UploadOutlined, PlayCircleOutlined, DownloadOutlined } from '@ant-design/icons';
import { autoEnrollmentApi } from '../../services/api';
import type { UploadFile } from 'antd/es/upload/interface';

export const BulkEnrollment: React.FC = () => {
    const [file, setFile] = useState<UploadFile | null>(null);
    const [policyId, setPolicyId] = useState<string>('');
    const [job, setJob] = useState<any>(null);
    const [loading, setLoading] = useState(false);

    const handleUpload = async () => {
        if (!file || !policyId) {
            message.error('Please select file and policy');
            return;
        }

        setLoading(true);
        try {
            const formData = new FormData();
            formData.append('file', file as any);
            formData.append('policyId', policyId);

            const response = await autoEnrollmentApi.bulkEnrollFromCSV(formData);
            setJob(response.data);
            message.success('Bulk enrollment started');

            // Poll for job status
            pollJobStatus(response.data.jobId);
        } catch (error) {
            message.error('Failed to start bulk enrollment');
        } finally {
            setLoading(false);
        }
    };

    const pollJobStatus = async (jobId: string) => {
        const interval = setInterval(async () => {
            try {
                const response = await autoEnrollmentApi.getJob(jobId);
                setJob(response.data);

                if (response.data.status === 'COMPLETED' || response.data.status === 'FAILED') {
                    clearInterval(interval);
                }
            } catch (error) {
                clearInterval(interval);
            }
        }, 2000);
    };

    return (
        <div>
            <Card title="Bulk Enrollment">
                <Space direction="vertical" style={{ width: '100%' }} size="large">
                    <Alert
                        message="Upload CSV File"
                        description="CSV file should contain usernames in the first column (header required)"
                        type="info"
                        showIcon
                    />

                    <Upload
                        beforeUpload={(file) => {
                            setFile(file);
                            return false;
                        }}
                        maxCount={1}
                        accept=".csv"
                    >
                        <Button icon={<UploadOutlined />}>Select CSV File</Button>
                    </Upload>

                    <Select
                        placeholder="Select Auto-Enrollment Policy"
                        style={{ width: '100%' }}
                        onChange={setPolicyId}
                    >
                        {/* Populate with policies */}
                    </Select>

                    <Button
                        type="primary"
                        icon={<PlayCircleOutlined />}
                        onClick={handleUpload}
                        loading={loading}
                        disabled={!file || !policyId}
                    >
                        Start Bulk Enrollment
                    </Button>

                    {job && (
                        <Card title="Job Progress" size="small">
                            <Space direction="vertical" style={{ width: '100%' }}>
                                <div>
                                    <strong>Status:</strong> {job.status}
                                </div>
                                <Progress
                                    percent={Math.round((job.successCount + job.failureCount) / job.totalUsers * 100)}
                                    status={job.status === 'COMPLETED' ? 'success' : 'active'}
                                />
                                <div>
                                    Success: {job.successCount} | Failures: {job.failureCount} | Total: {job.totalUsers}
                                </div>

                                {job.status === 'COMPLETED' && job.results && (
                                    <Table
                                        dataSource={job.results}
                                        columns={[
                                            { title: 'User ID', dataIndex: 'userId' },
                                            {
                                                title: 'Status',
                                                dataIndex: 'success',
                                                render: (success: boolean) => success ? 'Success' : 'Failed',
                                            },
                                            { title: 'Error', dataIndex: 'error' },
                                        ]}
                                        size="small"
                                        pagination={{ pageSize: 10 }}
                                    />
                                )}
                            </Space>
                        </Card>
                    )}
                </Space>
            </Card>
        </div>
    );
};
```

---

## 4. Just-In-Time (JIT) Provisioning

### 4.1 Overview
Just-In-Time Provisioning issues certificates on-demand when a user first accesses a resource, rather than proactively enrolling all users. This reduces certificate sprawl and ensures certificates are only issued when needed.

### 4.2 Use Cases
- **VPN Access**: Issue certificate when user first connects to VPN
- **WiFi Authentication**: Issue certificate on first WiFi access attempt
- **Application Access**: Issue certificate when accessing secure application
- **API Access**: Issue client certificate for API authentication

### 4.3 JIT Provisioning Flow

```
┌──────────────────────────────────────────────────────────────┐
│              Just-In-Time Provisioning Flow                   │
├──────────────────────────────────────────────────────────────┤
│                                                               │
│  1. User attempts to access resource (VPN, WiFi, App)        │
│                        │                                      │
│                        ▼                                      │
│  2. Resource checks for valid certificate                    │
│     - Certificate exists? → Allow access                     │
│     - No certificate? → Redirect to JIT enrollment           │
│                        │                                      │
│                        ▼                                      │
│  3. JIT Service validates user eligibility                   │
│     - Check AD authentication                                │
│     - Check authorization (AD groups)                        │
│     - Check certificate quota                                │
│                        │                                      │
│                        ▼                                      │
│  4. Generate certificate (fast path)                         │
│     - Generate key pair (server-side for speed)              │
│     - Create CSR with user attributes                        │
│     - Auto-approve (no manual review)                        │
│     - Submit to CA (low-latency API)                         │
│                        │                                      │
│                        ▼                                      │
│  5. Deliver certificate immediately                          │
│     - Return PKCS#12 to client                               │
│     - Install in certificate store (automatic)               │
│     - Redirect to original resource                          │
│                        │                                      │
│                        ▼                                      │
│  6. User accesses resource with new certificate              │
│                                                               │
└──────────────────────────────────────────────────────────────┘
```

### 4.4 Database Schema for JIT

```sql
-- JIT Provisioning Configuration
CREATE TABLE jit_provisioning_config (
    config_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    config_name VARCHAR(255) NOT NULL,
    enabled BOOLEAN DEFAULT true,

    -- Resource Integration
    resource_type VARCHAR(50), -- 'VPN', 'WIFI', 'APPLICATION', 'API'
    resource_identifier VARCHAR(255), -- Resource URL or ID

    -- Certificate Template
    template_id UUID REFERENCES certificate_templates(template_id),

    -- Eligibility
    eligible_ad_groups TEXT[],
    eligible_departments TEXT[],

    -- Behavior
    certificate_lifetime_hours INTEGER DEFAULT 720, -- 30 days
    allow_multiple_certs BOOLEAN DEFAULT false,

    -- Performance
    max_issuance_time_seconds INTEGER DEFAULT 10, -- SLA for issuance
    cache_timeout_minutes INTEGER DEFAULT 5,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- JIT Provisioning Requests
CREATE TABLE jit_provisioning_requests (
    request_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    config_id UUID REFERENCES jit_provisioning_config(config_id),
    user_id UUID REFERENCES users(user_id),

    -- Request Context
    resource_type VARCHAR(50),
    resource_accessed VARCHAR(500),
    client_ip_address VARCHAR(45),
    user_agent TEXT,

    -- Processing
    status VARCHAR(50), -- 'PENDING', 'PROCESSING', 'ISSUED', 'FAILED'
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    processing_time_ms INTEGER,

    -- Result
    certificate_id UUID REFERENCES certificates(certificate_id),
    error_message TEXT,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_jit_requests_user ON jit_provisioning_requests(user_id);
CREATE INDEX idx_jit_requests_status ON jit_provisioning_requests(status);
CREATE INDEX idx_jit_requests_created ON jit_provisioning_requests(created_at);
```

### 4.5 JIT Provisioning Service

```java
package com.company.ra.service.jit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.KeyPair;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class JITProvisioningService {

    @Autowired
    private JITProvisioningConfigRepository configRepository;

    @Autowired
    private JITProvisioningRequestRepository requestRepository;

    @Autowired
    private ActiveDirectoryService adService;

    @Autowired
    private KeyGenerationService keyGenService;

    @Autowired
    private CSRService csrService;

    @Autowired
    private CertificateAuthorityService caService;

    @Autowired
    private CertificateRepository certificateRepository;

    @Autowired
    private AuditService auditService;

    // Cache for recently issued certificates (avoid duplicate issuance)
    private final Cache<String, Certificate> certificateCache = CacheBuilder.newBuilder()
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build();

    /**
     * Provision certificate Just-In-Time for user accessing resource
     *
     * @return Issued certificate (fast path, target < 10 seconds)
     */
    @Transactional
    public JITProvisioningResult provisionCertificate(
            User user,
            String resourceType,
            String resourceIdentifier,
            String clientIp) {

        long startTime = System.currentTimeMillis();

        // 1. Find applicable JIT config
        JITProvisioningConfig config = findConfigForResource(resourceType, resourceIdentifier);
        if (config == null || !config.isEnabled()) {
            return JITProvisioningResult.failure("JIT provisioning not configured for this resource");
        }

        // 2. Create request record
        JITProvisioningRequest request = createRequest(user, config, resourceType,
            resourceIdentifier, clientIp);

        try {
            // 3. Check cache for recently issued certificate
            String cacheKey = user.getUserId() + ":" + config.getConfigId();
            Certificate cachedCert = certificateCache.getIfPresent(cacheKey);

            if (cachedCert != null && isCertificateValid(cachedCert)) {
                request.setStatus(RequestStatus.ISSUED);
                request.setCertificateId(cachedCert.getCertificateId());
                request.setCompletedAt(LocalDateTime.now());
                request.setProcessingTimeMs((int)(System.currentTimeMillis() - startTime));
                requestRepository.save(request);

                auditService.log(AuditAction.JIT_PROVISION_CACHE_HIT, user.getUserId(),
                    "certificate", cachedCert.getCertificateId());

                return JITProvisioningResult.success(cachedCert, request.getProcessingTimeMs());
            }

            // 4. Check if user already has valid certificate for this resource
            Optional<Certificate> existingCert = findExistingCertificate(user, config);
            if (existingCert.isPresent() && !config.isAllowMultipleCerts()) {
                Certificate cert = existingCert.get();

                // Cache for future requests
                certificateCache.put(cacheKey, cert);

                request.setStatus(RequestStatus.ISSUED);
                request.setCertificateId(cert.getCertificateId());
                request.setCompletedAt(LocalDateTime.now());
                request.setProcessingTimeMs((int)(System.currentTimeMillis() - startTime));
                requestRepository.save(request);

                return JITProvisioningResult.success(cert, request.getProcessingTimeMs());
            }

            // 5. Check eligibility (fast authorization check)
            if (!isUserEligible(user, config)) {
                request.setStatus(RequestStatus.FAILED);
                request.setErrorMessage("User not eligible for JIT provisioning");
                request.setCompletedAt(LocalDateTime.now());
                requestRepository.save(request);

                auditService.log(AuditAction.JIT_PROVISION_DENIED, user.getUserId(),
                    "reason", "not_eligible");

                return JITProvisioningResult.failure("User not authorized for this resource");
            }

            // 6. Generate certificate (FAST PATH - server-side key generation)
            request.setStatus(RequestStatus.PROCESSING);
            requestRepository.save(request);

            Certificate certificate = generateCertificateQuickly(user, config);

            // 7. Cache certificate
            certificateCache.put(cacheKey, certificate);

            // 8. Update request with success
            request.setStatus(RequestStatus.ISSUED);
            request.setCertificateId(certificate.getCertificateId());
            request.setCompletedAt(LocalDateTime.now());
            request.setProcessingTimeMs((int)(System.currentTimeMillis() - startTime));
            requestRepository.save(request);

            // 9. Audit log
            auditService.log(AuditAction.JIT_PROVISION_SUCCESS, user.getUserId(),
                "certificate", certificate.getCertificateId(),
                "resource", resourceType,
                "processing_time_ms", request.getProcessingTimeMs());

            // 10. Check SLA compliance
            if (request.getProcessingTimeMs() > config.getMaxIssuanceTimeSeconds() * 1000) {
                log.warn("JIT provisioning exceeded SLA: {}ms > {}ms",
                    request.getProcessingTimeMs(),
                    config.getMaxIssuanceTimeSeconds() * 1000);
            }

            return JITProvisioningResult.success(certificate, request.getProcessingTimeMs());

        } catch (Exception e) {
            request.setStatus(RequestStatus.FAILED);
            request.setErrorMessage(e.getMessage());
            request.setCompletedAt(LocalDateTime.now());
            request.setProcessingTimeMs((int)(System.currentTimeMillis() - startTime));
            requestRepository.save(request);

            auditService.log(AuditAction.JIT_PROVISION_FAILED, user.getUserId(),
                "error", e.getMessage());

            return JITProvisioningResult.failure(e.getMessage());
        }
    }

    /**
     * Generate certificate with optimizations for speed
     */
    private Certificate generateCertificateQuickly(User user, JITProvisioningConfig config) {
        // Get template
        CertificateTemplate template = templateRepository
            .findById(config.getTemplateId())
            .orElseThrow(() -> new IllegalStateException("Template not found"));

        // Generate key pair (RSA 2048 for speed, or use pre-generated pool)
        KeyPair keyPair = keyGenService.generateKeyPair("RSA", 2048);

        // Build Subject DN
        String subjectDN = buildSubjectDN(user, template);

        // Build SANs
        List<String> sans = buildSubjectAlternativeNames(user, template);

        // Generate CSR
        PKCS10CertificationRequest csr = csrService.generateCSR(
            subjectDN,
            keyPair,
            template.getKeyUsage(),
            template.getExtendedKeyUsage(),
            sans
        );

        // Submit to CA (use fast path API if available)
        Certificate certificate = caService.issueCertificateQuickly(csr, template,
            config.getCertificateLifetimeHours());

        certificate.setUserId(user.getUserId());
        certificate.setTemplateId(template.getTemplateId());
        certificate.setJitProvisioned(true);

        certificateRepository.save(certificate);

        // Store private key securely
        keyStorageService.storePrivateKey(
            certificate.getCertificateId(),
            keyPair.getPrivate(),
            user.getUsername()
        );

        return certificate;
    }

    /**
     * Find JIT config for resource
     */
    private JITProvisioningConfig findConfigForResource(String resourceType,
                                                        String resourceIdentifier) {
        return configRepository
            .findByResourceTypeAndResourceIdentifier(resourceType, resourceIdentifier)
            .orElse(null);
    }

    /**
     * Check if user is eligible for JIT provisioning
     */
    private boolean isUserEligible(User user, JITProvisioningConfig config) {
        // Check AD groups (cached for performance)
        if (config.getEligibleAdGroups() != null && !config.getEligibleAdGroups().isEmpty()) {
            Set<String> userGroups = adService.getUserGroupsCached(user);

            boolean inGroup = config.getEligibleAdGroups().stream()
                .anyMatch(userGroups::contains);

            if (!inGroup) {
                return false;
            }
        }

        // Check department
        if (config.getEligibleDepartments() != null &&
            !config.getEligibleDepartments().isEmpty()) {

            if (!config.getEligibleDepartments().contains(user.getDepartment())) {
                return false;
            }
        }

        // Check user account is active
        if (!user.isActive()) {
            return false;
        }

        return true;
    }

    /**
     * Find existing valid certificate
     */
    private Optional<Certificate> findExistingCertificate(User user,
                                                          JITProvisioningConfig config) {
        return certificateRepository
            .findByUserIdAndTemplateIdAndStatus(
                user.getUserId(),
                config.getTemplateId(),
                CertificateStatus.ACTIVE
            )
            .stream()
            .filter(cert -> cert.getNotAfter().isAfter(LocalDateTime.now()))
            .findFirst();
    }

    /**
     * Check if certificate is still valid
     */
    private boolean isCertificateValid(Certificate cert) {
        return cert.getStatus() == CertificateStatus.ACTIVE &&
               cert.getNotAfter().isAfter(LocalDateTime.now());
    }

    private JITProvisioningRequest createRequest(User user, JITProvisioningConfig config,
                                                String resourceType, String resourceId,
                                                String clientIp) {
        JITProvisioningRequest request = new JITProvisioningRequest();
        request.setConfigId(config.getConfigId());
        request.setUserId(user.getUserId());
        request.setResourceType(resourceType);
        request.setResourceAccessed(resourceId);
        request.setClientIpAddress(clientIp);
        request.setStatus(RequestStatus.PENDING);
        request.setStartedAt(LocalDateTime.now());
        return requestRepository.save(request);
    }

    private String buildSubjectDN(User user, CertificateTemplate template) {
        return template.getSubjectDnTemplate()
            .replace("${displayName}", user.getDisplayName())
            .replace("${username}", user.getUsername())
            .replace("${mail}", user.getEmail())
            .replace("${department}", user.getDepartment());
    }

    private List<String> buildSubjectAlternativeNames(User user, CertificateTemplate template) {
        List<String> sans = new ArrayList<>();
        for (String sanTemplate : template.getSanTemplate()) {
            String san = sanTemplate
                .replace("${mail}", user.getEmail())
                .replace("${userPrincipalName}", user.getUsername());
            sans.add(san);
        }
        return sans;
    }
}

/**
 * Result object for JIT provisioning
 */
@Data
@AllArgsConstructor
public class JITProvisioningResult {
    private boolean success;
    private Certificate certificate;
    private String errorMessage;
    private Integer processingTimeMs;

    public static JITProvisioningResult success(Certificate cert, int processingTimeMs) {
        return new JITProvisioningResult(true, cert, null, processingTimeMs);
    }

    public static JITProvisioningResult failure(String error) {
        return new JITProvisioningResult(false, null, error, null);
    }
}

// Enums
public enum RequestStatus {
    PENDING, PROCESSING, ISSUED, FAILED
}
```

### 4.6 JIT REST API Endpoint

```java
package com.company.ra.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/jit-provisioning")
public class JITProvisioningController {

    @Autowired
    private JITProvisioningService jitService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Request JIT certificate provisioning
     * Endpoint called by client when accessing resource without certificate
     */
    @PostMapping("/provision")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<JITProvisioningResponse> provisionCertificate(
            @RequestBody JITProvisioningRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpRequest) {

        User user = userRepository.findByUsername(userDetails.getUsername())
            .orElseThrow(() -> new NotFoundException("User not found"));

        String clientIp = getClientIpAddress(httpRequest);

        JITProvisioningResult result = jitService.provisionCertificate(
            user,
            request.getResourceType(),
            request.getResourceIdentifier(),
            clientIp
        );

        if (result.isSuccess()) {
            // Return certificate as PKCS#12 (with private key)
            byte[] pkcs12 = pkcs12Service.createPKCS12(
                result.getCertificate(),
                keyStorageService.getPrivateKey(result.getCertificate().getCertificateId()),
                user.getUsername() // Use username as password
            );

            JITProvisioningResponse response = JITProvisioningResponse.builder()
                .success(true)
                .certificateId(result.getCertificate().getCertificateId())
                .serialNumber(result.getCertificate().getSerialNumber())
                .notBefore(result.getCertificate().getNotBefore())
                .notAfter(result.getCertificate().getNotAfter())
                .pkcs12(Base64.getEncoder().encodeToString(pkcs12))
                .processingTimeMs(result.getProcessingTimeMs())
                .build();

            return ResponseEntity.ok(response);

        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(JITProvisioningResponse.builder()
                    .success(false)
                    .errorMessage(result.getErrorMessage())
                    .build());
        }
    }

    /**
     * Check if user has certificate for resource
     */
    @GetMapping("/check")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CertificateCheckResponse> checkCertificate(
            @RequestParam String resourceType,
            @RequestParam String resourceIdentifier,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userRepository.findByUsername(userDetails.getUsername()).get();

        JITProvisioningConfig config = jitService.findConfigForResource(
            resourceType, resourceIdentifier);

        if (config == null) {
            return ResponseEntity.ok(CertificateCheckResponse.builder()
                .hasCertificate(false)
                .jitAvailable(false)
                .build());
        }

        Optional<Certificate> cert = jitService.findExistingCertificate(user, config);

        return ResponseEntity.ok(CertificateCheckResponse.builder()
            .hasCertificate(cert.isPresent())
            .jitAvailable(true)
            .certificateId(cert.map(Certificate::getCertificateId).orElse(null))
            .expiresAt(cert.map(Certificate::getNotAfter).orElse(null))
            .build());
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

@Data
@Builder
class JITProvisioningResponse {
    private boolean success;
    private String certificateId;
    private String serialNumber;
    private LocalDateTime notBefore;
    private LocalDateTime notAfter;
    private String pkcs12; // Base64 encoded
    private Integer processingTimeMs;
    private String errorMessage;
}

@Data
@Builder
class CertificateCheckResponse {
    private boolean hasCertificate;
    private boolean jitAvailable;
    private String certificateId;
    private LocalDateTime expiresAt;
}
```

### 4.7 Client Integration Example (VPN)

```python
# VPN Client - Python Example
import requests
import base64
import os
from cryptography import x509
from cryptography.hazmat.primitives.serialization import pkcs12

class VPNJITClient:
    def __init__(self, ra_url, username, password):
        self.ra_url = ra_url
        self.username = username
        self.password = password
        self.jwt_token = None

    def authenticate(self):
        """Authenticate with RA and get JWT token"""
        # Challenge-response authentication (as documented in CLAUDE.md)
        response = requests.post(f"{self.ra_url}/api/v1/auth/challenge",
                                json={"username": self.username})
        challenge = response.json()

        # ... encrypt response with password (see CLAUDE.md for details)

        login_response = requests.post(f"{self.ra_url}/api/v1/auth/login",
                                      json={
                                          "challenge_id": challenge['challenge_id'],
                                          "username": self.username,
                                          "response": encrypted_response
                                      })

        self.jwt_token = login_response.json()['access_token']

    def connect_to_vpn(self):
        """Connect to VPN with JIT certificate provisioning"""
        # Check if certificate exists
        check_response = requests.get(
            f"{self.ra_url}/api/v1/jit-provisioning/check",
            headers={"Authorization": f"Bearer {self.jwt_token}"},
            params={
                "resourceType": "VPN",
                "resourceIdentifier": "corporate-vpn-01"
            }
        )

        check_data = check_response.json()

        if not check_data['hasCertificate']:
            # Request JIT provisioning
            print("No certificate found. Requesting JIT provisioning...")

            provision_response = requests.post(
                f"{self.ra_url}/api/v1/jit-provisioning/provision",
                headers={"Authorization": f"Bearer {self.jwt_token}"},
                json={
                    "resourceType": "VPN",
                    "resourceIdentifier": "corporate-vpn-01"
                }
            )

            if provision_response.status_code == 200:
                provision_data = provision_response.json()
                print(f"Certificate provisioned in {provision_data['processingTimeMs']}ms")

                # Decode PKCS#12
                pkcs12_bytes = base64.b64decode(provision_data['pkcs12'])

                # Import certificate into system certificate store
                self.import_certificate(pkcs12_bytes, self.username)

            else:
                print("JIT provisioning failed:", provision_response.json())
                return False

        # Connect to VPN with certificate
        return self.establish_vpn_connection()

    def import_certificate(self, pkcs12_bytes, password):
        """Import PKCS#12 certificate into system store"""
        private_key, certificate, additional_certs = pkcs12.load_key_and_certificates(
            pkcs12_bytes,
            password.encode()
        )

        # Save certificate and private key for VPN client
        cert_path = os.path.expanduser("~/.vpn/client.crt")
        key_path = os.path.expanduser("~/.vpn/client.key")

        with open(cert_path, "wb") as f:
            f.write(certificate.public_bytes(encoding=serialization.Encoding.PEM))

        with open(key_path, "wb") as f:
            f.write(private_key.private_bytes(
                encoding=serialization.Encoding.PEM,
                format=serialization.PrivateFormat.TraditionalOpenSSL,
                encryption_algorithm=serialization.NoEncryption()
            ))

        print(f"Certificate imported: {cert_path}")

    def establish_vpn_connection(self):
        """Connect to VPN using certificate"""
        # Use OpenVPN or other VPN client with certificate authentication
        import subprocess

        result = subprocess.run([
            "openvpn",
            "--config", "/etc/openvpn/client.conf",
            "--cert", os.path.expanduser("~/.vpn/client.crt"),
            "--key", os.path.expanduser("~/.vpn/client.key")
        ])

        return result.returncode == 0

# Usage
client = VPNJITClient(
    ra_url="https://ra.company.com",
    username="kablu@company.com",
    password="user_password"
)

client.authenticate()
client.connect_to_vpn()
```

---

## 5. Device Auto-Registration (SCEP/EST)

### 5.1 Overview
Device Auto-Registration enables IoT devices, network equipment, and servers to automatically obtain certificates using industry-standard protocols like SCEP (Simple Certificate Enrollment Protocol) and EST (Enrollment over Secure Transport).

### 5.2 Protocol Support

#### SCEP (Simple Certificate Enrollment Protocol) - RFC 8894
- **Use Cases**: Cisco routers, switches, firewalls, IoT devices
- **Transport**: HTTP-based
- **Security**: Challenge password or pre-shared key
- **Operations**: GetCACert, PKIOperation (enrollment), GetCRL

#### EST (Enrollment over Secure Transport) - RFC 7030
- **Use Cases**: Modern devices, cloud services, containers
- **Transport**: HTTPS with TLS client authentication
- **Security**: TLS with mutual authentication
- **Operations**: Simple Enrollment, Re-enrollment, CSR Attributes, Server Key Generation

### 5.3 Database Schema for Device Registration

```sql
-- Device Registration Configuration
CREATE TABLE device_registration_config (
    config_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    protocol VARCHAR(20), -- 'SCEP', 'EST'
    enabled BOOLEAN DEFAULT true,

    -- SCEP Configuration
    scep_challenge_password VARCHAR(255),
    scep_challenge_validity_hours INTEGER DEFAULT 24,

    -- EST Configuration
    est_require_tls_auth BOOLEAN DEFAULT true,
    est_allow_anonymous_csr_attrs BOOLEAN DEFAULT true,

    -- Certificate Policy
    template_id UUID REFERENCES certificate_templates(template_id),
    device_cert_validity_days INTEGER DEFAULT 365,
    auto_approve_devices BOOLEAN DEFAULT false,

    -- Device Identification
    require_device_identifier BOOLEAN DEFAULT true,
    allowed_device_types TEXT[], -- ['ROUTER', 'SWITCH', 'IOT', 'SERVER']

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Registered Devices
CREATE TABLE registered_devices (
    device_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_identifier VARCHAR(255) UNIQUE NOT NULL, -- MAC, serial number, UUID
    device_type VARCHAR(50),
    device_name VARCHAR(255),
    device_description TEXT,

    -- Location & Ownership
    location VARCHAR(255),
    owner_department VARCHAR(100),
    owner_user_id UUID REFERENCES users(user_id),

    -- Network Info
    ip_address VARCHAR(45),
    mac_address VARCHAR(17),
    hostname VARCHAR(255),

    -- Registration
    registration_protocol VARCHAR(20), -- 'SCEP', 'EST', 'MANUAL'
    registered_at TIMESTAMP,
    last_seen TIMESTAMP,

    -- Current Certificate
    current_certificate_id UUID REFERENCES certificates(certificate_id),
    certificate_expiry TIMESTAMP,

    -- Status
    status VARCHAR(50), -- 'ACTIVE', 'SUSPENDED', 'REVOKED', 'EXPIRED'
    is_active BOOLEAN DEFAULT true,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_registered_devices_identifier ON registered_devices(device_identifier);
CREATE INDEX idx_registered_devices_status ON registered_devices(status);

-- Device Enrollment Requests
CREATE TABLE device_enrollment_requests (
    request_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id UUID REFERENCES registered_devices(device_id),
    protocol VARCHAR(20),

    -- Request Data
    csr_pem TEXT,
    device_identifier VARCHAR(255),
    challenge_password VARCHAR(255),

    -- Processing
    status VARCHAR(50), -- 'PENDING', 'APPROVED', 'REJECTED', 'ISSUED'
    submitted_at TIMESTAMP,
    processed_at TIMESTAMP,

    -- Result
    certificate_id UUID REFERENCES certificates(certificate_id),
    rejection_reason TEXT,

    -- Audit
    source_ip VARCHAR(45),
    user_agent TEXT,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_device_enrollment_status ON device_enrollment_requests(status);
```

### 5.4 SCEP Service Implementation

```java
package com.company.ra.service.scep;

import org.jscep.client.Client;
import org.jscep.transaction.EnrollmentTransaction;
import org.springframework.stereotype.Service;

/**
 * SCEP Server Implementation
 * Implements RFC 8894 - Simple Certificate Enrollment Protocol
 */
@Service
public class SCEPService {

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private DeviceEnrollmentRequestRepository enrollmentRepository;

    @Autowired
    private CertificateAuthorityService caService;

    @Autowired
    private DeviceRegistrationConfigRepository configRepository;

    /**
     * SCEP GetCACert operation
     * Returns CA certificate and RA certificate chain
     */
    public byte[] getCACert() {
        // Return CA certificate in PKCS#7 format
        Certificate caCert = caService.getCACertificate();
        return pkcs7Service.createCACertResponse(caCert);
    }

    /**
     * SCEP PKIOperation - Handle enrollment request
     */
    @Transactional
    public byte[] pkiOperation(byte[] pkcsReqData, String sourceIp) {
        try {
            // 1. Parse PKCS#7 envelope
            PKCS7EnvelopedData envelope = PKCS7EnvelopedData.parse(pkcsReqData);

            // 2. Decrypt envelope (using RA private key)
            byte[] decryptedData = envelope.decrypt(raPrivateKey);

            // 3. Parse PKCS#10 CSR
            PKCS10CertificationRequest csr = PKCS10CertificationRequest.parse(decryptedData);

            // 4. Extract device identifier from CSR subject or SAN
            String deviceIdentifier = extractDeviceIdentifier(csr);

            // 5. Validate challenge password (from CSR attributes)
            String challengePassword = extractChallengePassword(csr);
            if (!validateChallengePassword(challengePassword)) {
                return createSCEPFailureResponse("Invalid challenge password");
            }

            // 6. Check if device is pre-registered
            Optional<RegisteredDevice> device = deviceRepository
                .findByDeviceIdentifier(deviceIdentifier);

            if (device.isEmpty()) {
                // Auto-register device if policy allows
                device = Optional.of(autoRegisterDevice(deviceIdentifier, csr, sourceIp));
            }

            // 7. Create enrollment request
            DeviceEnrollmentRequest request = new DeviceEnrollmentRequest();
            request.setDeviceId(device.get().getDeviceId());
            request.setProtocol("SCEP");
            request.setCsrPem(csrService.toPEM(csr));
            request.setDeviceIdentifier(deviceIdentifier);
            request.setChallengePassword(challengePassword);
            request.setStatus(RequestStatus.PENDING);
            request.setSourceIp(sourceIp);
            request.setSubmittedAt(LocalDateTime.now());
            enrollmentRepository.save(request);

            // 8. Auto-approve if policy allows
            DeviceRegistrationConfig config = configRepository.findBySCEPEnabled().get();
            if (config.isAutoApproveDevices()) {
                return processEnrollment(request, csr, device.get());
            } else {
                // Queue for manual approval
                return createSCEPPendingResponse(request.getRequestId());
            }

        } catch (Exception e) {
            log.error("SCEP PKIOperation failed", e);
            return createSCEPFailureResponse(e.getMessage());
        }
    }

    /**
     * Process device enrollment after approval
     */
    private byte[] processEnrollment(DeviceEnrollmentRequest request,
                                    PKCS10CertificationRequest csr,
                                    RegisteredDevice device) {
        try {
            // Get device certificate template
            DeviceRegistrationConfig config = configRepository.findBySCEPEnabled().get();
            CertificateTemplate template = templateRepository
                .findById(config.getTemplateId()).get();

            // Issue certificate
            Certificate certificate = caService.issueCertificate(csr, template);
            certificate.setDeviceId(device.getDeviceId());
            certificateRepository.save(certificate);

            // Update enrollment request
            request.setStatus(RequestStatus.ISSUED);
            request.setCertificateId(certificate.getCertificateId());
            request.setProcessedAt(LocalDateTime.now());
            enrollmentRepository.save(request);

            // Update device
            device.setCurrentCertificateId(certificate.getCertificateId());
            device.setCertificateExpiry(certificate.getNotAfter());
            device.setLastSeen(LocalDateTime.now());
            deviceRepository.save(device);

            // Return SCEP success response with certificate
            return createSCEPSuccessResponse(certificate);

        } catch (Exception e) {
            request.setStatus(RequestStatus.REJECTED);
            request.setRejectionReason(e.getMessage());
            enrollmentRepository.save(request);

            return createSCEPFailureResponse(e.getMessage());
        }
    }

    /**
     * Extract device identifier from CSR
     */
    private String extractDeviceIdentifier(PKCS10CertificationRequest csr) {
        // Try to extract from Subject CN
        X500Name subject = csr.getSubject();
        RDN cnRdn = subject.getRDNs(BCStyle.CN)[0];
        String cn = IETFUtils.valueToString(cnRdn.getFirst().getValue());

        if (cn != null && !cn.isEmpty()) {
            return cn;
        }

        // Try to extract from SAN (if present)
        Extension sanExt = csr.getExtension(Extension.subjectAlternativeName);
        if (sanExt != null) {
            GeneralNames sans = GeneralNames.getInstance(sanExt.getParsedValue());
            for (GeneralName san : sans.getNames()) {
                if (san.getTagNo() == GeneralName.dNSName) {
                    return san.getName().toString();
                }
            }
        }

        throw new IllegalArgumentException("Device identifier not found in CSR");
    }

    /**
     * Extract challenge password from CSR attributes
     */
    private String extractChallengePassword(PKCS10CertificationRequest csr) {
        Attribute[] attributes = csr.getAttributes(
            PKCSObjectIdentifiers.pkcs_9_at_challengePassword);

        if (attributes.length == 0) {
            throw new IllegalArgumentException("Challenge password not found");
        }

        return attributes[0].getAttrValues().getObjectAt(0).toString();
    }

    /**
     * Validate challenge password
     */
    private boolean validateChallengePassword(String password) {
        DeviceRegistrationConfig config = configRepository.findBySCEPEnabled().get();

        // Check against configured password
        if (!password.equals(config.getScepChallengePassword())) {
            return false;
        }

        // Additional validation: check password age, etc.
        return true;
    }

    /**
     * Auto-register device
     */
    private RegisteredDevice autoRegisterDevice(String deviceIdentifier,
                                               PKCS10CertificationRequest csr,
                                               String sourceIp) {
        RegisteredDevice device = new RegisteredDevice();
        device.setDeviceIdentifier(deviceIdentifier);
        device.setDeviceType("UNKNOWN");
        device.setDeviceName(deviceIdentifier);
        device.setRegistrationProtocol("SCEP");
        device.setIpAddress(sourceIp);
        device.setRegisteredAt(LocalDateTime.now());
        device.setLastSeen(LocalDateTime.now());
        device.setStatus("ACTIVE");
        device.setIsActive(true);

        return deviceRepository.save(device);
    }

    /**
     * Create SCEP success response (CertRep with issued certificate)
     */
    private byte[] createSCEPSuccessResponse(Certificate certificate) {
        // Create PKCS#7 SignedData with issued certificate
        return pkcs7Service.createCertRepSuccess(certificate);
    }

    /**
     * Create SCEP failure response
     */
    private byte[] createSCEPFailureResponse(String reason) {
        return pkcs7Service.createCertRepFailure(reason);
    }

    /**
     * Create SCEP pending response
     */
    private byte[] createSCEPPendingResponse(UUID requestId) {
        return pkcs7Service.createCertRepPending(requestId.toString());
    }
}
```

### 5.5 SCEP Controller (HTTP Endpoints)

```java
package com.company.ra.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * SCEP Protocol Endpoints
 * Implements RFC 8894
 */
@RestController
@RequestMapping("/scep")
public class SCEPController {

    @Autowired
    private SCEPService scepService;

    /**
     * SCEP GetCACert operation
     * GET /scep?operation=GetCACert
     */
    @GetMapping
    public ResponseEntity<byte[]> getCACert(@RequestParam String operation) {
        if ("GetCACert".equalsIgnoreCase(operation)) {
            byte[] caCert = scepService.getCACert();

            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Disposition", "attachment; filename=cacert.p7")
                .body(caCert);
        }

        return ResponseEntity.badRequest().build();
    }

    /**
     * SCEP PKIOperation
     * POST /scep?operation=PKIOperation
     * Body: PKCS#7 enveloped PKCS#10 CSR
     */
    @PostMapping
    public ResponseEntity<byte[]> pkiOperation(
            @RequestParam String operation,
            @RequestBody byte[] requestBody,
            HttpServletRequest httpRequest) {

        if ("PKIOperation".equalsIgnoreCase(operation)) {
            String sourceIp = getClientIpAddress(httpRequest);

            byte[] response = scepService.pkiOperation(requestBody, sourceIp);

            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(response);
        }

        return ResponseEntity.badRequest().build();
    }

    /**
     * SCEP GetCRL operation
     */
    @GetMapping("/crl")
    public ResponseEntity<byte[]> getCRL() {
        byte[] crl = scepService.getCRL();

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header("Content-Disposition", "attachment; filename=crl.der")
            .body(crl);
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
```

### 5.6 EST Service Implementation

```java
package com.company.ra.service.est;

import org.springframework.stereotype.Service;

/**
 * EST Server Implementation
 * Implements RFC 7030 - Enrollment over Secure Transport
 */
@Service
public class ESTService {

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private CertificateAuthorityService caService;

    /**
     * EST /cacerts - Get CA certificates
     */
    public byte[] getCACerts() {
        Certificate caCert = caService.getCACertificate();
        return pkcs7Service.createCACertResponse(caCert);
    }

    /**
     * EST /simpleenroll - Simple enrollment
     * Requires TLS client authentication
     */
    @Transactional
    public byte[] simpleEnroll(byte[] csrData, X509Certificate clientCert, String sourceIp) {
        try {
            // 1. Parse PKCS#10 CSR
            PKCS10CertificationRequest csr = PKCS10CertificationRequest.parse(csrData);

            // 2. Authenticate device using TLS client certificate
            String deviceIdentifier = clientCert.getSubjectX500Principal().getName();

            // 3. Find or create device
            RegisteredDevice device = deviceRepository
                .findByDeviceIdentifier(deviceIdentifier)
                .orElseGet(() -> autoRegisterDevice(deviceIdentifier, sourceIp, "EST"));

            // 4. Create enrollment request
            DeviceEnrollmentRequest request = new DeviceEnrollmentRequest();
            request.setDeviceId(device.getDeviceId());
            request.setProtocol("EST");
            request.setCsrPem(csrService.toPEM(csr));
            request.setDeviceIdentifier(deviceIdentifier);
            request.setStatus(RequestStatus.PENDING);
            request.setSourceIp(sourceIp);
            enrollmentRepository.save(request);

            // 5. Issue certificate (EST typically auto-approves)
            Certificate certificate = caService.issueCertificate(csr, getDeviceTemplate());
            certificate.setDeviceId(device.getDeviceId());
            certificateRepository.save(certificate);

            // 6. Update request
            request.setStatus(RequestStatus.ISSUED);
            request.setCertificateId(certificate.getCertificateId());
            enrollmentRepository.save(request);

            // 7. Update device
            device.setCurrentCertificateId(certificate.getCertificateId());
            device.setCertificateExpiry(certificate.getNotAfter());
            deviceRepository.save(device);

            // 8. Return certificate in PKCS#7 format
            return pkcs7Service.createCertResponse(certificate);

        } catch (Exception e) {
            log.error("EST simple enrollment failed", e);
            throw new RuntimeException("Enrollment failed: " + e.getMessage());
        }
    }

    /**
     * EST /simplereenroll - Certificate renewal
     */
    public byte[] simpleReenroll(byte[] csrData, X509Certificate clientCert) {
        // Similar to simpleEnroll but verifies existing certificate
        // and issues renewal
        return simpleEnroll(csrData, clientCert, null);
    }

    /**
     * EST /csrattrs - Get CSR attributes
     * Returns required/optional attributes for CSR
     */
    public byte[] getCSRAttributes() {
        // Return ASN.1 encoded CSR attributes
        CertificateTemplate template = getDeviceTemplate();

        return csrAttributesService.buildCSRAttributes(
            template.getKeyUsage(),
            template.getExtendedKeyUsage(),
            template.getSubjectDnTemplate()
        );
    }

    private CertificateTemplate getDeviceTemplate() {
        DeviceRegistrationConfig config = configRepository.findByESTEnabled().get();
        return templateRepository.findById(config.getTemplateId()).get();
    }

    private RegisteredDevice autoRegisterDevice(String deviceIdentifier,
                                               String sourceIp,
                                               String protocol) {
        RegisteredDevice device = new RegisteredDevice();
        device.setDeviceIdentifier(deviceIdentifier);
        device.setDeviceType("UNKNOWN");
        device.setDeviceName(deviceIdentifier);
        device.setRegistrationProtocol(protocol);
        device.setIpAddress(sourceIp);
        device.setRegisteredAt(LocalDateTime.now());
        device.setStatus("ACTIVE");
        return deviceRepository.save(device);
    }
}
```

### 5.7 EST Controller

```java
package com.company.ra.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.security.cert.X509Certificate;
import java.util.Base64;

/**
 * EST Protocol Endpoints
 * Implements RFC 7030
 */
@RestController
@RequestMapping("/.well-known/est")
public class ESTController {

    @Autowired
    private ESTService estService;

    /**
     * GET /.well-known/est/cacerts
     * Returns CA certificate(s) in PKCS#7 format
     */
    @GetMapping(value = "/cacerts", produces = "application/pkcs7-mime")
    public ResponseEntity<String> getCACerts() {
        byte[] caCerts = estService.getCACerts();

        // EST requires Base64 encoding with specific content type
        String base64Certs = Base64.getEncoder().encodeToString(caCerts);

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/pkcs7-mime"))
            .header("Content-Transfer-Encoding", "base64")
            .body(base64Certs);
    }

    /**
     * POST /.well-known/est/simpleenroll
     * Simple enrollment - requires TLS client authentication
     * Body: Base64-encoded PKCS#10 CSR
     */
    @PostMapping(value = "/simpleenroll",
                 consumes = "application/pkcs10",
                 produces = "application/pkcs7-mime")
    public ResponseEntity<String> simpleEnroll(
            @RequestBody String base64CSR,
            HttpServletRequest request) {

        try {
            // Extract client certificate from TLS session
            X509Certificate[] certs = (X509Certificate[])
                request.getAttribute("javax.servlet.request.X509Certificate");

            if (certs == null || certs.length == 0) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("TLS client authentication required");
            }

            X509Certificate clientCert = certs[0];

            // Decode CSR
            byte[] csrData = Base64.getDecoder().decode(base64CSR);

            // Process enrollment
            String sourceIp = getClientIpAddress(request);
            byte[] certificateResponse = estService.simpleEnroll(csrData, clientCert, sourceIp);

            // Return certificate in Base64
            String base64Cert = Base64.getEncoder().encodeToString(certificateResponse);

            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/pkcs7-mime"))
                .header("Content-Transfer-Encoding", "base64")
                .body(base64Cert);

        } catch (Exception e) {
            log.error("EST simple enrollment failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Enrollment failed: " + e.getMessage());
        }
    }

    /**
     * POST /.well-known/est/simplereenroll
     * Certificate renewal
     */
    @PostMapping(value = "/simplereenroll",
                 consumes = "application/pkcs10",
                 produces = "application/pkcs7-mime")
    public ResponseEntity<String> simpleReenroll(
            @RequestBody String base64CSR,
            HttpServletRequest request) {

        // Similar to simpleEnroll
        return simpleEnroll(base64CSR, request);
    }

    /**
     * GET /.well-known/est/csrattrs
     * Returns CSR attributes (required/optional fields)
     */
    @GetMapping(value = "/csrattrs", produces = "application/csrattrs")
    public ResponseEntity<String> getCSRAttributes() {
        byte[] csrAttrs = estService.getCSRAttributes();

        String base64Attrs = Base64.getEncoder().encodeToString(csrAttrs);

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/csrattrs"))
            .header("Content-Transfer-Encoding", "base64")
            .body(base64Attrs);
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
```

### 5.8 Device Management UI

```typescript
// src/pages/Devices/DeviceList.tsx

import React, { useEffect, useState } from 'react';
import { Table, Button, Tag, Space, Modal, message, Input } from 'antd';
import { PlusOutlined, DeleteOutlined, SyncOutlined } from '@ant-design/icons';
import { deviceApi } from '../../services/api';
import type { RegisteredDevice } from '../../types';

export const DeviceList: React.FC = () => {
    const [devices, setDevices] = useState<RegisteredDevice[]>([]);
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        loadDevices();
    }, []);

    const loadDevices = async () => {
        setLoading(true);
        try {
            const response = await deviceApi.getAllDevices();
            setDevices(response.data);
        } catch (error) {
            message.error('Failed to load devices');
        } finally {
            setLoading(false);
        }
    };

    const revokeDevice = async (deviceId: string) => {
        Modal.confirm({
            title: 'Revoke Device Certificate',
            content: 'This will revoke the device certificate. Continue?',
            okText: 'Revoke',
            okType: 'danger',
            onOk: async () => {
                try {
                    await deviceApi.revokeDevice(deviceId);
                    message.success('Device certificate revoked');
                    loadDevices();
                } catch (error) {
                    message.error('Failed to revoke device');
                }
            },
        });
    };

    const columns = [
        {
            title: 'Device Name',
            dataIndex: 'deviceName',
            key: 'deviceName',
        },
        {
            title: 'Device Identifier',
            dataIndex: 'deviceIdentifier',
            key: 'deviceIdentifier',
        },
        {
            title: 'Device Type',
            dataIndex: 'deviceType',
            key: 'deviceType',
        },
        {
            title: 'Protocol',
            dataIndex: 'registrationProtocol',
            key: 'registrationProtocol',
            render: (protocol: string) => (
                <Tag color={protocol === 'SCEP' ? 'blue' : 'green'}>{protocol}</Tag>
            ),
        },
        {
            title: 'IP Address',
            dataIndex: 'ipAddress',
            key: 'ipAddress',
        },
        {
            title: 'Certificate Expiry',
            dataIndex: 'certificateExpiry',
            key: 'certificateExpiry',
            render: (expiry: string) => new Date(expiry).toLocaleDateString(),
        },
        {
            title: 'Status',
            dataIndex: 'status',
            key: 'status',
            render: (status: string) => {
                const color = status === 'ACTIVE' ? 'green' : 'red';
                return <Tag color={color}>{status}</Tag>;
            },
        },
        {
            title: 'Last Seen',
            dataIndex: 'lastSeen',
            key: 'lastSeen',
            render: (lastSeen: string) => new Date(lastSeen).toLocaleString(),
        },
        {
            title: 'Actions',
            key: 'actions',
            render: (_: any, record: RegisteredDevice) => (
                <Space>
                    <Button
                        type="link"
                        icon={<SyncOutlined />}
                        onClick={() => {/* Renew certificate */}}
                    >
                        Renew
                    </Button>
                    <Button
                        type="link"
                        danger
                        icon={<DeleteOutlined />}
                        onClick={() => revokeDevice(record.deviceId)}
                    >
                        Revoke
                    </Button>
                </Space>
            ),
        },
    ];

    return (
        <div>
            <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between' }}>
                <h1>Registered Devices</h1>
                <Button type="primary" icon={<PlusOutlined />}>
                    Register Device
                </Button>
            </div>

            <Table
                columns={columns}
                dataSource={devices}
                rowKey="deviceId"
                loading={loading}
                pagination={{ pageSize: 20 }}
            />
        </div>
    );
};
```

---

## 6. Architecture & Integration

### 6.1 System Architecture Diagram

```
┌────────────────────────────────────────────────────────────────────────┐
│                       Advanced RA Architecture                          │
├────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌──────────────────┐                                                  │
│  │   End Users      │                                                  │
│  │  (Web Browser)   │                                                  │
│  └─────────┬────────┘                                                  │
│            │                                                            │
│            ▼                                                            │
│  ┌──────────────────┐       ┌──────────────────┐                     │
│  │   RA Web UI      │◄─────►│  REST API        │                     │
│  │  (React/Angular) │       │  (Spring Boot)   │                     │
│  └──────────────────┘       └─────────┬────────┘                     │
│                                        │                               │
│            ┌───────────────────────────┼───────────────────┐          │
│            │                           │                   │          │
│            ▼                           ▼                   ▼          │
│  ┌──────────────────┐       ┌──────────────────┐ ┌─────────────────┐│
│  │ Auto-Enrollment  │       │  JIT Provisioning│ │ Device Auto-Reg ││
│  │    Service       │       │     Service      │ │ (SCEP/EST)      ││
│  │                  │       │                  │ │                 ││
│  │ - Login trigger  │       │ - On-demand cert │ │ - SCEP Server   ││
│  │ - Scheduled jobs │       │ - Fast issuance  │ │ - EST Server    ││
│  │ - Bulk enroll    │       │ - Resource based │ │ - IoT devices   ││
│  └─────────┬────────┘       └─────────┬────────┘ └────────┬────────┘│
│            │                           │                   │          │
│            └───────────────────────────┼───────────────────┘          │
│                                        │                               │
│                                        ▼                               │
│                            ┌──────────────────┐                       │
│                            │  CA Integration  │                       │
│                            │     Service      │                       │
│                            │                  │                       │
│                            │ - EJBCA API      │                       │
│                            │ - Microsoft CA   │                       │
│                            │ - Generic RFC CA │                       │
│                            └─────────┬────────┘                       │
│                                      │                                │
│            ┌─────────────────────────┼──────────────────────┐         │
│            │                         │                      │         │
│            ▼                         ▼                      ▼         │
│  ┌──────────────────┐     ┌──────────────────┐   ┌─────────────────┐│
│  │ Active Directory │     │    Database      │   │  CA (External)  ││
│  │                  │     │  (PostgreSQL)    │   │                 ││
│  │ - User auth      │     │  - Certificates  │   │ - Issue certs   ││
│  │ - Groups         │     │  - Requests      │   │ - Revoke certs  ││
│  │ - Attributes     │     │  - Audit logs    │   │ - Publish CRL   ││
│  └──────────────────┘     └──────────────────┘   └─────────────────┘│
│                                                                        │
│  ┌───────────────────────────────────────────────────────────────────┐│
│  │                      Supporting Services                          ││
│  ├───────────────────────────────────────────────────────────────────┤│
│  │                                                                   ││
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐              ││
│  │  │  Email      │  │  Audit      │  │  Monitoring │              ││
│  │  │  Service    │  │  Service    │  │  (Metrics)  │              ││
│  │  └─────────────┘  └─────────────┘  └─────────────┘              ││
│  │                                                                   ││
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐              ││
│  │  │  Key        │  │  PKCS#12    │  │  Certificate│              ││
│  │  │  Storage    │  │  Service    │  │  Distribution│              ││
│  │  └─────────────┘  └─────────────┘  └─────────────┘              ││
│  │                                                                   ││
│  └───────────────────────────────────────────────────────────────────┘│
│                                                                        │
└────────────────────────────────────────────────────────────────────────┘
```

### 6.2 CA Integration Examples

#### EJBCA Integration

```java
package com.company.ra.integration.ca;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * EJBCA REST API Integration
 */
@Service
public class EJBCAService implements CertificateAuthorityService {

    @Value("${ca.ejbca.url}")
    private String ejbcaUrl;

    @Value("${ca.ejbca.cert-profile}")
    private String certProfile;

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public Certificate issueCertificate(PKCS10CertificationRequest csr,
                                       CertificateTemplate template) {
        // EJBCA REST API call
        String endpoint = ejbcaUrl + "/ejbca/ejbca-rest-api/v1/certificate/pkcs10enroll";

        EJBCAEnrollmentRequest request = new EJBCAEnrollmentRequest();
        request.setCertificateRequest(Base64.getEncoder().encodeToString(csr.getEncoded()));
        request.setCertificateProfileName(certProfile);
        request.setEndEntityProfileName(template.getTemplateName());
        request.setCertificateAuthorityName("ManagementCA");
        request.setUsername(UUID.randomUUID().toString());
        request.setPassword(generatePassword());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + getEJBCAToken());

        HttpEntity<EJBCAEnrollmentRequest> httpRequest =
            new HttpEntity<>(request, headers);

        ResponseEntity<EJBCAEnrollmentResponse> response = restTemplate.postForEntity(
            endpoint,
            httpRequest,
            EJBCAEnrollmentResponse.class
        );

        if (response.getStatusCode().is2xxSuccessful()) {
            EJBCAEnrollmentResponse body = response.getBody();

            // Parse issued certificate
            byte[] certBytes = Base64.getDecoder().decode(body.getCertificate());
            X509Certificate x509Cert = parseCertificate(certBytes);

            // Create Certificate entity
            Certificate certificate = new Certificate();
            certificate.setSerialNumber(x509Cert.getSerialNumber().toString(16));
            certificate.setSubjectDN(x509Cert.getSubjectX500Principal().getName());
            certificate.setIssuerDN(x509Cert.getIssuerX500Principal().getName());
            certificate.setNotBefore(x509Cert.getNotBefore().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDateTime());
            certificate.setNotAfter(x509Cert.getNotAfter().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDateTime());
            certificate.setCertificatePem(body.getCertificate());
            certificate.setStatus(CertificateStatus.ACTIVE);

            return certificate;
        } else {
            throw new RuntimeException("EJBCA certificate issuance failed: " +
                response.getStatusCode());
        }
    }

    @Override
    public void revokeCertificate(String serialNumber, RevocationReason reason) {
        String endpoint = ejbcaUrl + "/ejbca/ejbca-rest-api/v1/certificate/" +
            serialNumber + "/revoke";

        EJBCARevocationRequest request = new EJBCARevocationRequest();
        request.setReason(mapRevocationReason(reason));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + getEJBCAToken());

        HttpEntity<EJBCARevocationRequest> httpRequest =
            new HttpEntity<>(request, headers);

        restTemplate.put(endpoint, httpRequest);
    }

    private String getEJBCAToken() {
        // Implement EJBCA authentication
        return "ejbca-token";
    }

    private String generatePassword() {
        return UUID.randomUUID().toString();
    }

    private X509Certificate parseCertificate(byte[] certBytes) throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return (X509Certificate) cf.generateCertificate(
            new ByteArrayInputStream(certBytes));
    }

    private int mapRevocationReason(RevocationReason reason) {
        switch (reason) {
            case KEY_COMPROMISE: return 1;
            case CA_COMPROMISE: return 2;
            case AFFILIATION_CHANGED: return 3;
            case SUPERSEDED: return 4;
            case CESSATION_OF_OPERATION: return 5;
            case CERTIFICATE_HOLD: return 6;
            case REMOVE_FROM_CRL: return 8;
            case PRIVILEGE_WITHDRAWN: return 9;
            case AA_COMPROMISE: return 10;
            default: return 0; // Unspecified
        }
    }
}
```

#### Microsoft CA Integration

```java
package com.company.ra.integration.ca;

import org.springframework.stereotype.Service;

/**
 * Microsoft CA Integration via certreq utility
 */
@Service
public class MicrosoftCAService implements CertificateAuthorityService {

    @Value("${ca.ms.hostname}")
    private String caHostname;

    @Value("${ca.ms.ca-name}")
    private String caName;

    @Override
    public Certificate issueCertificate(PKCS10CertificationRequest csr,
                                       CertificateTemplate template) {
        try {
            // Save CSR to temporary file
            Path csrFile = Files.createTempFile("csr-", ".req");
            Files.write(csrFile, csrService.toPEM(csr).getBytes());

            // Issue certificate using certreq
            String command = String.format(
                "certreq -submit -config \"%s\\%s\" -attrib \"CertificateTemplate:%s\" \"%s\"",
                caHostname,
                caName,
                template.getTemplateName(),
                csrFile.toString()
            );

            Process process = Runtime.getRuntime().exec(command);
            process.waitFor();

            if (process.exitValue() == 0) {
                // Read issued certificate
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));

                StringBuilder certPem = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    certPem.append(line).append("\n");
                }

                // Parse certificate
                X509Certificate x509Cert = parseCertificate(certPem.toString());

                // Create Certificate entity
                Certificate certificate = new Certificate();
                certificate.setSerialNumber(x509Cert.getSerialNumber().toString(16));
                certificate.setSubjectDN(x509Cert.getSubjectX500Principal().getName());
                certificate.setIssuerDN(x509Cert.getIssuerX500Principal().getName());
                certificate.setNotBefore(x509Cert.getNotBefore().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDateTime());
                certificate.setNotAfter(x509Cert.getNotAfter().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDateTime());
                certificate.setCertificatePem(certPem.toString());
                certificate.setStatus(CertificateStatus.ACTIVE);

                // Clean up
                Files.delete(csrFile);

                return certificate;
            } else {
                throw new RuntimeException("Certificate issuance failed");
            }

        } catch (Exception e) {
            throw new RuntimeException("Microsoft CA integration error", e);
        }
    }

    @Override
    public void revokeCertificate(String serialNumber, RevocationReason reason) {
        try {
            String command = String.format(
                "certutil -revoke -config \"%s\\%s\" %s %d",
                caHostname,
                caName,
                serialNumber,
                mapRevocationReason(reason)
            );

            Process process = Runtime.getRuntime().exec(command);
            process.waitFor();

            if (process.exitValue() != 0) {
                throw new RuntimeException("Certificate revocation failed");
            }

        } catch (Exception e) {
            throw new RuntimeException("Microsoft CA revocation error", e);
        }
    }

    private X509Certificate parseCertificate(String certPem) throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return (X509Certificate) cf.generateCertificate(
            new ByteArrayInputStream(certPem.getBytes()));
    }

    private int mapRevocationReason(RevocationReason reason) {
        // Microsoft CA revocation reason codes
        switch (reason) {
            case UNSPECIFIED: return 0;
            case KEY_COMPROMISE: return 1;
            case CA_COMPROMISE: return 2;
            case AFFILIATION_CHANGED: return 3;
            case SUPERSEDED: return 4;
            case CESSATION_OF_OPERATION: return 5;
            case CERTIFICATE_HOLD: return 6;
            default: return 0;
        }
    }
}
```

### 6.3 Performance Optimization

#### Caching Strategy

```java
package com.company.ra.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class CacheConfiguration {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // Cache AD user groups for 15 minutes
        cacheConfigurations.put("userGroups",
            RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(15)));

        // Cache certificate templates for 1 hour
        cacheConfigurations.put("certificateTemplates",
            RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1)));

        // Cache recently issued certificates for 5 minutes (JIT)
        cacheConfigurations.put("recentCertificates",
            RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5)));

        // Cache device registrations for 30 minutes
        cacheConfigurations.put("deviceRegistrations",
            RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30)));

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(RedisCacheConfiguration.defaultCacheConfig())
            .withInitialCacheConfigurations(cacheConfigurations)
            .build();
    }
}

// Usage in services
@Service
public class ActiveDirectoryService {

    @Cacheable(value = "userGroups", key = "#user.userId")
    public Set<String> getUserGroupsCached(User user) {
        // Expensive LDAP query cached for 15 minutes
        return getUserGroups(user);
    }
}
```

### 6.4 Monitoring & Metrics

```java
package com.company.ra.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class EnrollmentMetrics {

    private final MeterRegistry registry;

    public EnrollmentMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordAutoEnrollment(String policyId, long processingTimeMs, boolean success) {
        registry.counter("ra.auto_enrollment.total",
            "policy", policyId,
            "success", String.valueOf(success)
        ).increment();

        registry.timer("ra.auto_enrollment.processing_time",
            "policy", policyId
        ).record(Duration.ofMillis(processingTimeMs));
    }

    public void recordJITProvisioning(String resourceType, long processingTimeMs,
                                     boolean success) {
        registry.counter("ra.jit_provisioning.total",
            "resource_type", resourceType,
            "success", String.valueOf(success)
        ).increment();

        registry.timer("ra.jit_provisioning.processing_time",
            "resource_type", resourceType
        ).record(Duration.ofMillis(processingTimeMs));

        // Alert if processing time exceeds SLA (10 seconds)
        if (processingTimeMs > 10000) {
            registry.counter("ra.jit_provisioning.sla_violation",
                "resource_type", resourceType
            ).increment();
        }
    }

    public void recordDeviceEnrollment(String protocol, boolean success) {
        registry.counter("ra.device_enrollment.total",
            "protocol", protocol,
            "success", String.valueOf(success)
        ).increment();
    }

    public void recordCertificateIssuance(String templateName, long issuanceTimeMs) {
        registry.counter("ra.certificate.issued",
            "template", templateName
        ).increment();

        registry.timer("ra.certificate.issuance_time",
            "template", templateName
        ).record(Duration.ofMillis(issuanceTimeMs));
    }
}
```

---

## 7. Summary & Best Practices

### 7.1 Implementation Checklist

**Auto-Enrollment:**
- ✅ Configure auto-enrollment policies with AD group mappings
- ✅ Set up scheduled jobs for periodic enrollment checks
- ✅ Implement login-triggered enrollment in authentication flow
- ✅ Configure certificate distribution methods
- ✅ Test auto-renewal workflow
- ✅ Monitor enrollment job failures and retry logic

**Scheduled Enrollment:**
- ✅ Define cron schedules for enrollment policies
- ✅ Implement job scheduling service with concurrency control
- ✅ Set up job status tracking and reporting
- ✅ Configure retry logic for failed enrollments
- ✅ Monitor job execution times and SLA compliance

**Bulk Enrollment:**
- ✅ Implement CSV parsing for bulk user lists
- ✅ Configure thread pool for concurrent processing
- ✅ Implement progress tracking UI
- ✅ Set up error handling and partial failure reporting
- ✅ Test with large user sets (1000+ users)

**JIT Provisioning:**
- ✅ Configure JIT policies for resources (VPN, WiFi, Apps)
- ✅ Implement fast-path certificate issuance (< 10s)
- ✅ Set up certificate caching for performance
- ✅ Integrate with resource access controls
- ✅ Monitor SLA compliance (target < 10 seconds)

**Device Auto-Registration:**
- ✅ Deploy SCEP server endpoints
- ✅ Deploy EST server with TLS client auth
- ✅ Configure device templates and policies
- ✅ Test with actual devices (routers, IoT)
- ✅ Implement device lifecycle management
- ✅ Monitor device enrollment and certificate renewals

### 7.2 Security Best Practices

1. **Key Management:**
   - Store private keys in HSM for high-value certificates
   - Encrypt server-generated keys at rest
   - Use secure random number generator for key generation

2. **Authentication:**
   - Always verify user identity before auto-enrollment
   - Use challenge passwords for SCEP (rotate regularly)
   - Require TLS client authentication for EST
   - Implement rate limiting on enrollment endpoints

3. **Authorization:**
   - Enforce strict AD group-based eligibility
   - Implement certificate quota limits per user/device
   - Audit all auto-enrollment operations
   - Review and approve high-value certificate templates

4. **Network Security:**
   - Use TLS 1.3 for all network communications
   - Implement IP whitelisting for device enrollment
   - Monitor for unusual enrollment patterns
   - Deploy SCEP/EST behind firewall/WAF

### 7.3 Performance Tuning

1. **Caching:**
   - Cache AD group lookups (15-30 minutes)
   - Cache certificate templates (1 hour)
   - Cache JIT-issued certificates (5 minutes)

2. **Concurrency:**
   - Use thread pools for bulk enrollment (10-20 threads)
   - Implement async processing for scheduled jobs
   - Use database connection pooling (50-100 connections)

3. **CA Integration:**
   - Use CA batch enrollment APIs when available
   - Implement retry logic with exponential backoff
   - Monitor CA response times and alert on slowness

4. **Database Optimization:**
   - Index frequently queried columns (user_id, device_identifier, status)
   - Partition large tables (certificates, audit_logs)
   - Archive old enrollment records (> 1 year)

---

**Document Version**: 1.0
**Last Updated**: 2026-01-21
**Status**: Complete Implementation Guide

