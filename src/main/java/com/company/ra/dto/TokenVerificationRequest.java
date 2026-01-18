package com.company.ra.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for JWT token verification
 */
public class TokenVerificationRequest {

    @NotBlank(message = "Token is required")
    private String token;

    public TokenVerificationRequest() {
    }

    public TokenVerificationRequest(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public String toString() {
        return "TokenVerificationRequest{token='[REDACTED]'}";
    }
}
