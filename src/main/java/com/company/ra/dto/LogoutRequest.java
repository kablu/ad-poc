package com.company.ra.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for logout
 */
public class LogoutRequest {

    @NotBlank(message = "Token is required")
    private String token;

    public LogoutRequest() {
    }

    public LogoutRequest(String token) {
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
        return "LogoutRequest{token='[REDACTED]'}";
    }
}
