package com.company.ra.dto;

/**
 * Response DTO for certificate revocation
 */
public class RevocationResponse {

    private boolean success = true;
    private String message;
    private long revokedAt;
    private String error;

    public RevocationResponse() {
    }

    public static RevocationResponse error(String message) {
        RevocationResponse response = new RevocationResponse();
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(long revokedAt) {
        this.revokedAt = revokedAt;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    @Override
    public String toString() {
        return "RevocationResponse{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", revokedAt=" + revokedAt +
                ", error='" + error + '\'' +
                '}';
    }
}
