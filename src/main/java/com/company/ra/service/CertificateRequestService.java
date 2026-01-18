package com.company.ra.service;

import com.company.ra.dto.ADUserAttributes;
import com.company.ra.dto.SubjectDN;
import com.company.ra.entity.CertificateRequest;
import com.company.ra.repository.CertificateRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing certificate requests
 */
@Service
public class CertificateRequestService {

    private static final Logger logger = LoggerFactory.getLogger(CertificateRequestService.class);

    @Autowired
    private CertificateRequestRepository certificateRequestRepository;

    @Autowired
    private CSRValidationService csrValidationService;

    /**
     * Create a new certificate request
     *
     * @param username Username
     * @param csrPem CSR in PEM format
     * @param certificateType Certificate type
     * @param subjectDN Subject DN
     * @param adUserAttributes AD user attributes
     * @return Created certificate request
     */
    @Transactional
    public CertificateRequest createRequest(String username, String csrPem, String certificateType,
                                           SubjectDN subjectDN, ADUserAttributes adUserAttributes) {
        try {
            String requestId = "REQ-" + UUID.randomUUID().toString();

            CertificateRequest certRequest = new CertificateRequest();
            certRequest.setRequestId(requestId);
            certRequest.setUsername(username);
            certRequest.setCsrPem(csrPem);
            certRequest.setCertificateType(certificateType);
            certRequest.setSubjectDN(subjectDN.getRawDN());
            certRequest.setStatus("PENDING");
            certRequest.setSubmittedAt(Instant.now());

            // Calculate and store public key hash
            try {
                var pkcs10 = csrValidationService.parsePKCS10(csrPem);
                var publicKey = new org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter()
                    .getPublicKey(pkcs10.getSubjectPublicKeyInfo());
                String publicKeyHash = csrValidationService.calculatePublicKeyHash(publicKey);
                certRequest.setPublicKeyHash(publicKeyHash);
            } catch (Exception e) {
                logger.error("Failed to calculate public key hash", e);
            }

            CertificateRequest saved = certificateRequestRepository.save(certRequest);
            logger.info("Certificate request created: {}", requestId);

            return saved;
        } catch (Exception e) {
            logger.error("Failed to create certificate request for user: {}", username, e);
            throw new RuntimeException("Failed to create certificate request", e);
        }
    }

    /**
     * Get certificate request by request ID
     *
     * @param requestId Request ID
     * @return Certificate request or null
     */
    public CertificateRequest getRequest(String requestId) {
        return certificateRequestRepository.findByRequestId(requestId).orElse(null);
    }

    /**
     * Get certificate request by certificate ID
     *
     * @param certificateId Certificate ID
     * @return Certificate request or null
     */
    public CertificateRequest getRequestByCertificateId(String certificateId) {
        return certificateRequestRepository.findByCertificateSerialNumber(certificateId).orElse(null);
    }

    /**
     * Approve certificate request
     *
     * @param requestId Request ID
     * @param approvedBy Username of approver
     * @return Updated certificate request
     */
    @Transactional
    public CertificateRequest approveRequest(String requestId, String approvedBy) {
        CertificateRequest certRequest = getRequest(requestId);

        if (certRequest == null) {
            throw new IllegalArgumentException("Certificate request not found: " + requestId);
        }

        if (!"PENDING".equals(certRequest.getStatus())) {
            throw new IllegalStateException("Certificate request is not in PENDING state: " + requestId);
        }

        certRequest.setStatus("APPROVED");
        certRequest.setApprovedAt(Instant.now());
        certRequest.setApprovedBy(approvedBy);

        CertificateRequest saved = certificateRequestRepository.save(certRequest);
        logger.info("Certificate request approved: {} by {}", requestId, approvedBy);

        return saved;
    }

    /**
     * Reject certificate request
     *
     * @param requestId Request ID
     * @param rejectedBy Username of rejector
     * @param reason Rejection reason
     * @return Updated certificate request
     */
    @Transactional
    public CertificateRequest rejectRequest(String requestId, String rejectedBy, String reason) {
        CertificateRequest certRequest = getRequest(requestId);

        if (certRequest == null) {
            throw new IllegalArgumentException("Certificate request not found: " + requestId);
        }

        if (!"PENDING".equals(certRequest.getStatus())) {
            throw new IllegalStateException("Certificate request is not in PENDING state: " + requestId);
        }

        certRequest.setStatus("REJECTED");
        certRequest.setRejectionReason(reason);

        CertificateRequest saved = certificateRequestRepository.save(certRequest);
        logger.info("Certificate request rejected: {} by {}, reason: {}", requestId, rejectedBy, reason);

        return saved;
    }

    /**
     * Mark certificate request as issued
     *
     * @param requestId Request ID
     * @param certificateSerialNumber Certificate serial number
     * @param certificatePem Certificate in PEM format
     * @return Updated certificate request
     */
    @Transactional
    public CertificateRequest markAsIssued(String requestId, String certificateSerialNumber, String certificatePem) {
        CertificateRequest certRequest = getRequest(requestId);

        if (certRequest == null) {
            throw new IllegalArgumentException("Certificate request not found: " + requestId);
        }

        certRequest.setStatus("ISSUED");
        certRequest.setIssuedAt(Instant.now());
        certRequest.setCertificateSerialNumber(certificateSerialNumber);
        certRequest.setCertificatePem(certificatePem);

        CertificateRequest saved = certificateRequestRepository.save(certRequest);
        logger.info("Certificate request marked as issued: {}, serial: {}", requestId, certificateSerialNumber);

        return saved;
    }

    /**
     * Mark certificate as revoked
     *
     * @param requestId Request ID
     * @param reason Revocation reason
     * @return Updated certificate request
     */
    @Transactional
    public CertificateRequest markAsRevoked(String requestId, String reason) {
        CertificateRequest certRequest = getRequest(requestId);

        if (certRequest == null) {
            throw new IllegalArgumentException("Certificate request not found: " + requestId);
        }

        certRequest.setStatus("REVOKED");
        certRequest.setRevokedAt(Instant.now());
        certRequest.setRejectionReason(reason);

        CertificateRequest saved = certificateRequestRepository.save(certRequest);
        logger.info("Certificate marked as revoked: {}, reason: {}", requestId, reason);

        return saved;
    }

    /**
     * Check if user is authorized for certificate type
     *
     * @param username Username
     * @param certificateType Certificate type
     * @return true if authorized
     */
    public boolean isAuthorizedForCertificateType(String username, String certificateType) {
        // TODO: Implement authorization logic based on user roles and certificate type policies
        // For now, allow all authenticated users to request user certificates
        switch (certificateType.toUpperCase()) {
            case "USER_AUTHENTICATION":
            case "EMAIL_SIGNING":
            case "DOCUMENT_SIGNING":
                return true;
            case "CODE_SIGNING":
            case "SERVER_AUTHENTICATION":
                // Require special permissions
                return false; // TODO: Check user roles
            default:
                return false;
        }
    }

    /**
     * Check if certificate request is eligible for auto-approval
     *
     * @param certRequest Certificate request
     * @param adUserAttributes AD user attributes
     * @return true if eligible for auto-approval
     */
    public boolean isEligibleForAutoApproval(CertificateRequest certRequest, ADUserAttributes adUserAttributes) {
        // Auto-approval eligibility criteria:
        // 1. User has valid AD account
        // 2. Certificate type allows auto-approval
        // 3. User belongs to appropriate AD groups
        // 4. No previous failed authentication attempts

        String certificateType = certRequest.getCertificateType();

        switch (certificateType.toUpperCase()) {
            case "USER_AUTHENTICATION":
            case "EMAIL_SIGNING":
                // Auto-approve for end users
                return adUserAttributes.getRoles().contains("END_ENTITY");
            case "DOCUMENT_SIGNING":
                // Auto-approve if user is in document signing group
                return adUserAttributes.getAdGroups().stream()
                    .anyMatch(group -> group.contains("Document-Signing"));
            case "CODE_SIGNING":
            case "SERVER_AUTHENTICATION":
                // Always require manual approval
                return false;
            default:
                return false;
        }
    }

    /**
     * List certificate requests with filtering and pagination
     *
     * @param status Optional status filter
     * @param page Page number
     * @param size Page size
     * @return List of certificate requests
     */
    public List<CertificateRequest> listRequests(String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "submittedAt"));

        if (status != null && !status.isEmpty()) {
            return certificateRequestRepository.findByStatus(status, pageable);
        } else {
            return certificateRequestRepository.findAll(pageable).getContent();
        }
    }

    /**
     * Count certificate requests with optional status filter
     *
     * @param status Optional status filter
     * @return Count of requests
     */
    public long countRequests(String status) {
        if (status != null && !status.isEmpty()) {
            return certificateRequestRepository.countByStatus(status);
        } else {
            return certificateRequestRepository.count();
        }
    }

    /**
     * List certificate requests by username
     *
     * @param username Username
     * @return List of certificate requests
     */
    public List<CertificateRequest> listRequestsByUsername(String username) {
        return certificateRequestRepository.findByUsername(username);
    }
}
