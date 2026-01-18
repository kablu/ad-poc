package com.company.ra.dto;

import java.time.Instant;

/**
 * Response DTO after CSR submission
 */
public class CertificateRequestResponse {

    private boolean success = true;
    private String requestId;
    private String status;
    private String subjectDN;
    private Instant submittedAt;
    private boolean autoApproved;
    private String error;

    public CertificateRequestResponse() {
    }

    public static CertificateRequestResponse error(String message) {
        CertificateRequestResponse response = new CertificateRequestResponse();
        response.setSuccess(false);
        response.setError(message);
        return response;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSubjectDN() {
        return subjectDN;
    }

    public void setSubjectDN(String subjectDN) {
        this.subjectDN = subjectDN;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }

    public boolean isAutoApproved() {
        return autoApproved;
    }

    public void setAutoApproved(boolean autoApproved) {
        this.autoApproved = autoApproved;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    @Override
    public String toString() {
        return "CertificateRequestResponse{" +
                "success=" + success +
                ", requestId='" + requestId + '\'' +
                ", status='" + status + '\'' +
                ", subjectDN='" + subjectDN + '\'' +
                ", submittedAt=" + submittedAt +
                ", autoApproved=" + autoApproved +
                ", error='" + error + '\'' +
                '}';
    }
}
