package com.company.ra.controller;

import com.company.ra.dto.*;
import com.company.ra.entity.CertificateRequest;
import com.company.ra.entity.User;
import com.company.ra.service.*;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.List;

/**
 * REST Controller for certificate request operations
 * Handles PKCS#10 CSR submission, validation, and certificate issuance
 */
@RestController
@RequestMapping("/api/v1/certificates")
public class CertificateRequestController {

    private static final Logger logger = LoggerFactory.getLogger(CertificateRequestController.class);

    @Autowired
    private CSRValidationService csrValidationService;

    @Autowired
    private CertificateRequestService certificateRequestService;

    @Autowired
    private ActiveDirectoryService activeDirectoryService;

    @Autowired
    private CAIntegrationService caIntegrationService;

    @Autowired
    private AuditLogService auditLogService;

    /**
     * Submit PKCS#10 Certificate Signing Request
     *
     * @param request CSR submission request
     * @param userDetails Authenticated user details
     * @return Certificate request response with request ID and status
     */
    @PostMapping("/requests")
    @PreAuthorize("hasAnyRole('END_ENTITY', 'RA_OPERATOR', 'RA_OFFICER', 'RA_ADMIN')")
    public ResponseEntity<CertificateRequestResponse> submitCSR(
            @RequestBody CSRSubmissionRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            String username = userDetails.getUsername();
            logger.info("CSR submission request from user: {}", username);

            // Step 1: Validate request format
            if (request.getCsrPem() == null || request.getCsrPem().trim().isEmpty()) {
                logger.warn("Empty CSR submitted by user: {}", username);
                return ResponseEntity.badRequest()
                    .body(CertificateRequestResponse.error("CSR data is required"));
            }

            if (request.getCertificateType() == null) {
                logger.warn("Certificate type not specified by user: {}", username);
                return ResponseEntity.badRequest()
                    .body(CertificateRequestResponse.error("Certificate type is required"));
            }

            // Step 2: Parse PKCS#10 CSR
            PKCS10CertificationRequest pkcs10CSR;
            try {
                pkcs10CSR = csrValidationService.parsePKCS10(request.getCsrPem());
            } catch (Exception e) {
                logger.error("Failed to parse PKCS#10 CSR from user: {}", username, e);
                return ResponseEntity.badRequest()
                    .body(CertificateRequestResponse.error("Invalid PKCS#10 CSR format"));
            }

            // Step 3: Verify CSR signature (Proof of Possession)
            if (!csrValidationService.verifySignature(pkcs10CSR)) {
                logger.warn("CSR signature verification failed for user: {}", username);
                auditLogService.logFailedCSRSubmission(username, "Invalid signature");
                return ResponseEntity.badRequest()
                    .body(CertificateRequestResponse.error("CSR signature verification failed"));
            }

            // Step 4: Extract Subject DN from CSR
            var subjectDN = csrValidationService.extractSubjectDN(pkcs10CSR);
            logger.info("CSR Subject DN: {}", subjectDN);

            // Step 5: Retrieve user's AD attributes
            var adUserAttributes = activeDirectoryService.getUserDetails(username);
            if (adUserAttributes == null) {
                logger.error("Failed to retrieve AD attributes for user: {}", username);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CertificateRequestResponse.error("Failed to retrieve user details"));
            }

            // Step 6: Validate Subject DN matches AD attributes
            var dnValidation = csrValidationService.validateSubjectDN(subjectDN, adUserAttributes);
            if (!dnValidation.isValid()) {
                logger.warn("Subject DN validation failed for user: {}, reasons: {}",
                    username, dnValidation.getErrors());
                auditLogService.logFailedCSRSubmission(username,
                    "Subject DN mismatch: " + String.join(", ", dnValidation.getErrors()));
                return ResponseEntity.badRequest()
                    .body(CertificateRequestResponse.error(
                        "Subject DN validation failed: " + String.join(", ", dnValidation.getErrors())));
            }

            // Step 7: Validate key algorithm and size
            var keyValidation = csrValidationService.validateKeyParameters(pkcs10CSR, request.getCertificateType());
            if (!keyValidation.isValid()) {
                logger.warn("Key parameters validation failed for user: {}, reasons: {}",
                    username, keyValidation.getErrors());
                auditLogService.logFailedCSRSubmission(username,
                    "Invalid key parameters: " + String.join(", ", keyValidation.getErrors()));
                return ResponseEntity.badRequest()
                    .body(CertificateRequestResponse.error(
                        "Key validation failed: " + String.join(", ", keyValidation.getErrors())));
            }

            // Step 8: Check for duplicate public key
            if (csrValidationService.isPublicKeyBlacklisted(pkcs10CSR)) {
                logger.warn("Duplicate or blacklisted public key detected for user: {}", username);
                auditLogService.logFailedCSRSubmission(username, "Duplicate or blacklisted public key");
                return ResponseEntity.badRequest()
                    .body(CertificateRequestResponse.error(
                        "This public key has already been used or is blacklisted"));
            }

            // Step 9: Check authorization for certificate type
            if (!certificateRequestService.isAuthorizedForCertificateType(
                    username, request.getCertificateType())) {
                logger.warn("User {} not authorized for certificate type: {}",
                    username, request.getCertificateType());
                auditLogService.logFailedCSRSubmission(username,
                    "Not authorized for certificate type: " + request.getCertificateType());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(CertificateRequestResponse.error(
                        "Not authorized for certificate type: " + request.getCertificateType()));
            }

            // Step 10: Create certificate request entity
            CertificateRequest certRequest = certificateRequestService.createRequest(
                username,
                request.getCsrPem(),
                request.getCertificateType(),
                subjectDN,
                adUserAttributes
            );

            // Audit log
            auditLogService.logCSRSubmission(username, certRequest.getRequestId(),
                request.getCertificateType(), "Submitted");

            // Step 11: Determine auto-approval eligibility
            boolean autoApprove = certificateRequestService.isEligibleForAutoApproval(
                certRequest, adUserAttributes);

            if (autoApprove) {
                // Auto-approve and forward to CA
                certRequest = certificateRequestService.approveRequest(
                    certRequest.getRequestId(), "SYSTEM_AUTO_APPROVAL");

                // Submit to CA
                caIntegrationService.submitToCA(certRequest);

                auditLogService.logRequestApproval(certRequest.getRequestId(),
                    "SYSTEM_AUTO_APPROVAL", "Auto-approved");

                logger.info("Certificate request {} auto-approved and submitted to CA",
                    certRequest.getRequestId());
            } else {
                logger.info("Certificate request {} pending manual approval",
                    certRequest.getRequestId());
            }

            // Create response
            CertificateRequestResponse response = new CertificateRequestResponse();
            response.setSuccess(true);
            response.setRequestId(certRequest.getRequestId());
            response.setStatus(certRequest.getStatus());
            response.setSubjectDN(subjectDN.toString());
            response.setSubmittedAt(certRequest.getSubmittedAt());
            response.setAutoApproved(autoApprove);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            logger.error("Error processing CSR submission from user: {}",
                userDetails.getUsername(), e);
            auditLogService.logFailedCSRSubmission(userDetails.getUsername(),
                "Internal error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(CertificateRequestResponse.error("Failed to process CSR"));
        }
    }

    /**
     * Get certificate request status
     *
     * @param requestId Certificate request ID
     * @param userDetails Authenticated user details
     * @return Certificate request details
     */
    @GetMapping("/requests/{requestId}")
    @PreAuthorize("hasAnyRole('END_ENTITY', 'RA_OPERATOR', 'RA_OFFICER', 'RA_ADMIN', 'AUDITOR')")
    public ResponseEntity<CertificateRequestStatusResponse> getRequestStatus(
            @PathVariable String requestId,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            String username = userDetails.getUsername();
            logger.info("Request status query for requestId: {} by user: {}", requestId, username);

            CertificateRequest certRequest = certificateRequestService.getRequest(requestId);

            if (certRequest == null) {
                logger.warn("Request ID not found: {}", requestId);
                return ResponseEntity.notFound().build();
            }

            // Check authorization - users can only view their own requests unless they're RA staff
            if (!certRequest.getUsername().equals(username)
                && !hasRoleAnyOf(userDetails, "RA_OPERATOR", "RA_OFFICER", "RA_ADMIN", "AUDITOR")) {
                logger.warn("User {} attempted to access request {} owned by {}",
                    username, requestId, certRequest.getUsername());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            CertificateRequestStatusResponse response = new CertificateRequestStatusResponse();
            response.setRequestId(certRequest.getRequestId());
            response.setStatus(certRequest.getStatus());
            response.setSubjectDN(certRequest.getSubjectDN());
            response.setUsername(certRequest.getUsername());
            response.setCertificateType(certRequest.getCertificateType());
            response.setSubmittedAt(certRequest.getSubmittedAt());
            response.setApprovedAt(certRequest.getApprovedAt());
            response.setApprovedBy(certRequest.getApprovedBy());
            response.setIssuedAt(certRequest.getIssuedAt());
            response.setRejectionReason(certRequest.getRejectionReason());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error retrieving request status for requestId: {}", requestId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Download issued certificate
     *
     * @param requestId Certificate request ID
     * @param userDetails Authenticated user details
     * @return Certificate data in PEM format
     */
    @GetMapping("/requests/{requestId}/certificate")
    @PreAuthorize("hasAnyRole('END_ENTITY', 'RA_OPERATOR', 'RA_OFFICER', 'RA_ADMIN')")
    public ResponseEntity<CertificateDownloadResponse> downloadCertificate(
            @PathVariable String requestId,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            String username = userDetails.getUsername();
            logger.info("Certificate download request for requestId: {} by user: {}", requestId, username);

            CertificateRequest certRequest = certificateRequestService.getRequest(requestId);

            if (certRequest == null) {
                return ResponseEntity.notFound().build();
            }

            // Check authorization
            if (!certRequest.getUsername().equals(username)
                && !hasRoleAnyOf(userDetails, "RA_OPERATOR", "RA_OFFICER", "RA_ADMIN")) {
                logger.warn("User {} attempted to download certificate for request {} owned by {}",
                    username, requestId, certRequest.getUsername());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // Check if certificate is issued
            if (!"ISSUED".equals(certRequest.getStatus())) {
                logger.warn("Certificate not yet issued for requestId: {}, status: {}",
                    requestId, certRequest.getStatus());
                return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(CertificateDownloadResponse.error("Certificate not yet issued"));
            }

            // Retrieve certificate from CA
            String certificatePem = caIntegrationService.retrieveCertificate(requestId);

            if (certificatePem == null) {
                logger.error("Failed to retrieve certificate from CA for requestId: {}", requestId);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CertificateDownloadResponse.error("Failed to retrieve certificate"));
            }

            CertificateDownloadResponse response = new CertificateDownloadResponse();
            response.setSuccess(true);
            response.setRequestId(requestId);
            response.setCertificatePem(certificatePem);
            response.setSerialNumber(certRequest.getCertificateSerialNumber());

            auditLogService.logCertificateDownload(username, requestId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error downloading certificate for requestId: {}", requestId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(CertificateDownloadResponse.error("Failed to download certificate"));
        }
    }

    /**
     * Revoke certificate
     *
     * @param certificateId Certificate ID
     * @param request Revocation request with reason
     * @param userDetails Authenticated user details
     * @return Revocation response
     */
    @PostMapping("/{certificateId}/revoke")
    @PreAuthorize("hasAnyRole('RA_OFFICER', 'RA_ADMIN')")
    public ResponseEntity<RevocationResponse> revokeCertificate(
            @PathVariable String certificateId,
            @RequestBody RevocationRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            String username = userDetails.getUsername();
            logger.info("Certificate revocation request for certificateId: {} by user: {}",
                certificateId, username);

            CertificateRequest certRequest = certificateRequestService.getRequestByCertificateId(certificateId);

            if (certRequest == null) {
                return ResponseEntity.notFound().build();
            }

            if (!"ISSUED".equals(certRequest.getStatus())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(RevocationResponse.error("Certificate is not in issued state"));
            }

            // Submit revocation request to CA
            boolean revoked = caIntegrationService.revokeCertificate(
                certRequest.getCertificateSerialNumber(),
                request.getReason(),
                username
            );

            if (revoked) {
                certificateRequestService.markAsRevoked(certRequest.getRequestId(), request.getReason());
                auditLogService.logCertificateRevocation(username, certificateId, request.getReason());

                RevocationResponse response = new RevocationResponse();
                response.setSuccess(true);
                response.setMessage("Certificate revoked successfully");
                response.setRevokedAt(System.currentTimeMillis());

                return ResponseEntity.ok(response);
            } else {
                logger.error("Failed to revoke certificate at CA for certificateId: {}", certificateId);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(RevocationResponse.error("Failed to revoke certificate at CA"));
            }

        } catch (Exception e) {
            logger.error("Error revoking certificate for certificateId: {}", certificateId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(RevocationResponse.error("Failed to revoke certificate"));
        }
    }

    /**
     * List certificate requests (with filtering and pagination)
     *
     * @param status Optional status filter
     * @param userDetails Authenticated user details
     * @return List of certificate requests
     */
    @GetMapping("/requests")
    @PreAuthorize("hasAnyRole('RA_OPERATOR', 'RA_OFFICER', 'RA_ADMIN', 'AUDITOR')")
    public ResponseEntity<CertificateRequestListResponse> listRequests(
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "50") int size,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            String username = userDetails.getUsername();
            logger.info("Certificate requests list query by user: {}, status filter: {}", username, status);

            List<CertificateRequest> requests = certificateRequestService.listRequests(status, page, size);
            long totalCount = certificateRequestService.countRequests(status);

            CertificateRequestListResponse response = new CertificateRequestListResponse();
            response.setRequests(requests);
            response.setTotalCount(totalCount);
            response.setPage(page);
            response.setSize(size);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error listing certificate requests", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Helper method to check if user has any of the specified roles
     */
    private boolean hasRoleAnyOf(UserDetails userDetails, String... roles) {
        return userDetails.getAuthorities().stream()
            .anyMatch(authority -> {
                String role = authority.getAuthority();
                for (String r : roles) {
                    if (role.equals("ROLE_" + r)) {
                        return true;
                    }
                }
                return false;
            });
    }
}
