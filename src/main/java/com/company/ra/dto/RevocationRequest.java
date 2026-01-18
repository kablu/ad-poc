package com.company.ra.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for certificate revocation
 */
public class RevocationRequest {

    @NotBlank(message = "Revocation reason is required")
    private String reason;

    private String comments;

    public RevocationRequest() {
    }

    public RevocationRequest(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    @Override
    public String toString() {
        return "RevocationRequest{" +
                "reason='" + reason + '\'' +
                ", comments='" + comments + '\'' +
                '}';
    }
}
