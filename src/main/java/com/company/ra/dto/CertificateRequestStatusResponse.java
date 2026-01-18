package com.company.ra.dto;

import java.time.Instant;

/**
 * Response DTO for certificate request status query
 */
public class CertificateRequestStatusResponse {

    private String requestId;
    private String status;
    private String subjectDN;
    private String username;
    private String certificateType;
    private Instant submittedAt;
    private Instant approvedAt;
    private String approvedBy;
    private Instant issuedAt;
    private String rejectionReason;

    public CertificateRequestStatusResponse() {
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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getCertificateType() {
        return certificateType;
    }

    public void setCertificateType(String certificateType) {
        this.certificateType = certificateType;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(Instant approvedAt) {
        this.approvedAt = approvedAt;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(Instant issuedAt) {
        this.issuedAt = issuedAt;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    @Override
    public String toString() {
        return "CertificateRequestStatusResponse{" +
                "requestId='" + requestId + '\'' +
                ", status='" + status + '\'' +
                ", subjectDN='" + subjectDN + '\'' +
                ", username='" + username + '\'' +
                ", certificateType='" + certificateType + '\'' +
                ", submittedAt=" + submittedAt +
                ", approvedAt=" + approvedAt +
                ", approvedBy='" + approvedBy + '\'' +
                ", issuedAt=" + issuedAt +
                ", rejectionReason='" + rejectionReason + '\'' +
                '}';
    }
}
