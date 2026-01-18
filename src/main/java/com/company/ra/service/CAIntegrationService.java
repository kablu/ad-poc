package com.company.ra.service;

import com.company.ra.entity.CertificateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for integrating with Certificate Authority (CA)
 */
@Service
public class CAIntegrationService {

    private static final Logger logger = LoggerFactory.getLogger(CAIntegrationService.class);

    @Value("${ca.api.baseUrl:https://ca.company.com/api/v1}")
    private String caBaseUrl;

    @Value("${ca.api.username}")
    private String caUsername;

    @Value("${ca.api.password}")
    private String caPassword;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private CertificateRequestService certificateRequestService;

    /**
     * Submit CSR to CA for certificate issuance
     *
     * @param certRequest Certificate request
     * @return true if submission successful
     */
    public boolean submitToCA(CertificateRequest certRequest) {
        try {
            logger.info("Submitting certificate request to CA: {}", certRequest.getRequestId());

            String url = caBaseUrl + "/certificates/issue";

            // Prepare request payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("requestId", certRequest.getRequestId());
            payload.put("csrPem", certRequest.getCsrPem());
            payload.put("subjectDN", certRequest.getSubjectDN());
            payload.put("certificateType", certRequest.getCertificateType());
            payload.put("username", certRequest.getUsername());

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBasicAuth(caUsername, caPassword);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            // Call CA API
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.CREATED) {
                Map<String, Object> responseBody = response.getBody();

                if (responseBody != null && responseBody.containsKey("certificateSerialNumber")) {
                    String serialNumber = (String) responseBody.get("certificateSerialNumber");
                    String certificatePem = (String) responseBody.get("certificatePem");

                    // Update certificate request with issued certificate
                    certificateRequestService.markAsIssued(
                        certRequest.getRequestId(),
                        serialNumber,
                        certificatePem
                    );

                    logger.info("Certificate issued by CA: {}, serial: {}",
                        certRequest.getRequestId(), serialNumber);
                    return true;
                } else {
                    logger.warn("CA response missing certificate data: {}", responseBody);
                    return false;
                }
            } else {
                logger.error("CA returned error status: {}", response.getStatusCode());
                return false;
            }

        } catch (Exception e) {
            logger.error("Failed to submit certificate request to CA: {}",
                certRequest.getRequestId(), e);
            return false;
        }
    }

    /**
     * Retrieve issued certificate from CA
     *
     * @param requestId Certificate request ID
     * @return Certificate in PEM format or null
     */
    public String retrieveCertificate(String requestId) {
        try {
            logger.info("Retrieving certificate from CA: {}", requestId);

            String url = caBaseUrl + "/certificates/" + requestId;

            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth(caUsername, caPassword);

            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                request,
                Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> responseBody = response.getBody();
                if (responseBody != null && responseBody.containsKey("certificatePem")) {
                    return (String) responseBody.get("certificatePem");
                }
            }

            logger.warn("Failed to retrieve certificate from CA: {}", requestId);
            return null;

        } catch (Exception e) {
            logger.error("Error retrieving certificate from CA: {}", requestId, e);
            return null;
        }
    }

    /**
     * Revoke certificate at CA
     *
     * @param serialNumber Certificate serial number
     * @param reason Revocation reason
     * @param revokedBy Username who initiated revocation
     * @return true if revocation successful
     */
    public boolean revokeCertificate(String serialNumber, String reason, String revokedBy) {
        try {
            logger.info("Revoking certificate at CA: {}, reason: {}", serialNumber, reason);

            String url = caBaseUrl + "/certificates/" + serialNumber + "/revoke";

            Map<String, Object> payload = new HashMap<>();
            payload.put("reason", reason);
            payload.put("revokedBy", revokedBy);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBasicAuth(caUsername, caPassword);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                logger.info("Certificate revoked successfully at CA: {}", serialNumber);
                return true;
            } else {
                logger.error("CA returned error status for revocation: {}", response.getStatusCode());
                return false;
            }

        } catch (Exception e) {
            logger.error("Failed to revoke certificate at CA: {}", serialNumber, e);
            return false;
        }
    }

    /**
     * Check certificate status at CA
     *
     * @param serialNumber Certificate serial number
     * @return Status string (VALID, REVOKED, EXPIRED, etc.) or null
     */
    public String checkCertificateStatus(String serialNumber) {
        try {
            String url = caBaseUrl + "/certificates/" + serialNumber + "/status";

            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth(caUsername, caPassword);

            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                request,
                Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> responseBody = response.getBody();
                if (responseBody != null && responseBody.containsKey("status")) {
                    return (String) responseBody.get("status");
                }
            }

            return null;

        } catch (Exception e) {
            logger.error("Error checking certificate status at CA: {}", serialNumber, e);
            return null;
        }
    }
}
