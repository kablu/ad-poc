package com.company.ra.service;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.company.ra.entity.AuditLog;
import com.company.ra.repository.AuditLogRepository;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Service for audit logging
 */
@Service
public class AuditLogService {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogService.class);

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired(required = false)
    private HttpServletRequest httpServletRequest;

    /**
     * Log CSR submission
     *
     * @param username Username
     * @param requestId Request ID
     * @param certificateType Certificate type
     * @param status Status (Submitted, etc.)
     */
    @Transactional
    public void logCSRSubmission(String username, String requestId, String certificateType, String status) {
        try {
            AuditLog log = new AuditLog();
            log.setTimestamp(Instant.now());
            log.setUsername(username);
            log.setAction("CSR_SUBMISSION");
            log.setResourceType("CERTIFICATE_REQUEST");
            log.setResourceId(requestId);
            log.setStatus("SUCCESS");
            log.setDetails("Certificate type: " + certificateType + ", Status: " + status);

            enrichWithRequestInfo(log);

            auditLogRepository.save(log);
            logger.debug("Audit log created for CSR submission: {}", requestId);
        } catch (Exception e) {
            logger.error("Failed to create audit log for CSR submission", e);
        }
    }

    /**
     * Log failed CSR submission
     *
     * @param username Username
     * @param reason Failure reason
     */
    @Transactional
    public void logFailedCSRSubmission(String username, String reason) {
        try {
            AuditLog log = new AuditLog();
            log.setTimestamp(Instant.now());
            log.setUsername(username);
            log.setAction("CSR_SUBMISSION");
            log.setResourceType("CERTIFICATE_REQUEST");
            log.setStatus("FAILED");
            log.setDetails("Failure reason: " + reason);

            enrichWithRequestInfo(log);

            auditLogRepository.save(log);
            logger.debug("Audit log created for failed CSR submission: {}", username);
        } catch (Exception e) {
            logger.error("Failed to create audit log for failed CSR submission", e);
        }
    }

    /**
     * Log certificate request approval
     *
     * @param requestId Request ID
     * @param approvedBy Username of approver
     * @param comments Optional comments
     */
    @Transactional
    public void logRequestApproval(String requestId, String approvedBy, String comments) {
        try {
            AuditLog log = new AuditLog();
            log.setTimestamp(Instant.now());
            log.setUsername(approvedBy);
            log.setAction("REQUEST_APPROVAL");
            log.setResourceType("CERTIFICATE_REQUEST");
            log.setResourceId(requestId);
            log.setStatus("SUCCESS");
            log.setDetails("Comments: " + comments);

            enrichWithRequestInfo(log);

            auditLogRepository.save(log);
            logger.debug("Audit log created for request approval: {}", requestId);
        } catch (Exception e) {
            logger.error("Failed to create audit log for request approval", e);
        }
    }

    /**
     * Log certificate request rejection
     *
     * @param requestId Request ID
     * @param rejectedBy Username of rejector
     * @param reason Rejection reason
     */
    @Transactional
    public void logRequestRejection(String requestId, String rejectedBy, String reason) {
        try {
            AuditLog log = new AuditLog();
            log.setTimestamp(Instant.now());
            log.setUsername(rejectedBy);
            log.setAction("REQUEST_REJECTION");
            log.setResourceType("CERTIFICATE_REQUEST");
            log.setResourceId(requestId);
            log.setStatus("SUCCESS");
            log.setDetails("Rejection reason: " + reason);

            enrichWithRequestInfo(log);

            auditLogRepository.save(log);
            logger.debug("Audit log created for request rejection: {}", requestId);
        } catch (Exception e) {
            logger.error("Failed to create audit log for request rejection", e);
        }
    }

    /**
     * Log certificate download
     *
     * @param username Username
     * @param requestId Request ID
     */
    @Transactional
    public void logCertificateDownload(String username, String requestId) {
        try {
            AuditLog log = new AuditLog();
            log.setTimestamp(Instant.now());
            log.setUsername(username);
            log.setAction("CERTIFICATE_DOWNLOAD");
            log.setResourceType("CERTIFICATE");
            log.setResourceId(requestId);
            log.setStatus("SUCCESS");

            enrichWithRequestInfo(log);

            auditLogRepository.save(log);
            logger.debug("Audit log created for certificate download: {}", requestId);
        } catch (Exception e) {
            logger.error("Failed to create audit log for certificate download", e);
        }
    }

    /**
     * Log certificate revocation
     *
     * @param username Username
     * @param certificateId Certificate ID
     * @param reason Revocation reason
     */
    @Transactional
    public void logCertificateRevocation(String username, String certificateId, String reason) {
        try {
            AuditLog log = new AuditLog();
            log.setTimestamp(Instant.now());
            log.setUsername(username);
            log.setAction("CERTIFICATE_REVOCATION");
            log.setResourceType("CERTIFICATE");
            log.setResourceId(certificateId);
            log.setStatus("SUCCESS");
            log.setDetails("Revocation reason: " + reason);

            enrichWithRequestInfo(log);

            auditLogRepository.save(log);
            logger.debug("Audit log created for certificate revocation: {}", certificateId);
        } catch (Exception e) {
            logger.error("Failed to create audit log for certificate revocation", e);
        }
    }

    /**
     * Log authentication attempt
     *
     * @param username Username
     * @param success Success status
     * @param details Additional details
     */
    @Transactional
    public void logAuthenticationAttempt(String username, boolean success, String details) {
        try {
            AuditLog log = new AuditLog();
            log.setTimestamp(Instant.now());
            log.setUsername(username);
            log.setAction("AUTHENTICATION");
            log.setStatus(success ? "SUCCESS" : "FAILED");
            log.setDetails(details);

            enrichWithRequestInfo(log);

            auditLogRepository.save(log);
            logger.debug("Audit log created for authentication attempt: {}, success: {}",
                username, success);
        } catch (Exception e) {
            logger.error("Failed to create audit log for authentication attempt", e);
        }
    }

    /**
     * Log generic action
     *
     * @param username Username
     * @param action Action name
     * @param resourceType Resource type
     * @param resourceId Resource ID
     * @param status Status
     * @param details Details
     */
    @Transactional
    public void logAction(String username, String action, String resourceType,
                         String resourceId, String status, String details) {
        try {
            AuditLog log = new AuditLog();
            log.setTimestamp(Instant.now());
            log.setUsername(username);
            log.setAction(action);
            log.setResourceType(resourceType);
            log.setResourceId(resourceId);
            log.setStatus(status);
            log.setDetails(details);

            enrichWithRequestInfo(log);

            auditLogRepository.save(log);
            logger.debug("Audit log created: action={}, username={}, status={}",
                action, username, status);
        } catch (Exception e) {
            logger.error("Failed to create audit log", e);
        }
    }

    /**
     * Enrich audit log with HTTP request information
     *
     * @param log Audit log entity
     */
    private void enrichWithRequestInfo(AuditLog log) {
        if (httpServletRequest != null) {
            try {
                // Get IP address
                String ipAddress = httpServletRequest.getRemoteAddr();
                String forwardedFor = httpServletRequest.getHeader("X-Forwarded-For");
                if (forwardedFor != null && !forwardedFor.isEmpty()) {
                    ipAddress = forwardedFor.split(",")[0].trim();
                }
                log.setIpAddress(ipAddress);

                // Get User-Agent
                String userAgent = httpServletRequest.getHeader("User-Agent");
                if (userAgent != null && userAgent.length() > 500) {
                    userAgent = userAgent.substring(0, 500);
                }
                log.setUserAgent(userAgent);
            } catch (Exception e) {
                logger.warn("Failed to enrich audit log with request info", e);
            }
        }
    }
}
