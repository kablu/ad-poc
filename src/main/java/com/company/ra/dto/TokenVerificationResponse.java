package com.company.ra.dto;

/**
 * Response DTO for JWT token verification
 */
public class TokenVerificationResponse {

    private boolean valid;
    private String username;
    private String error;

    public TokenVerificationResponse() {
    }

    public static TokenVerificationResponse invalid(String message) {
        TokenVerificationResponse response = new TokenVerificationResponse();
        response.setValid(false);
        response.setError(message);
        return response;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    @Override
    public String toString() {
        return "TokenVerificationResponse{" +
                "valid=" + valid +
                ", username='" + username + '\'' +
                ", error='" + error + '\'' +
                '}';
    }
}
