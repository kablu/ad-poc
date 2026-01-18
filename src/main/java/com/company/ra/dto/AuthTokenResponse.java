package com.company.ra.dto;

import java.util.List;

/**
 * Response DTO containing JWT token after successful authentication
 */
public class AuthTokenResponse {

    private boolean success = true;
    private String token;
    private String tokenType;
    private long expiresIn;
    private String username;
    private List<String> roles;
    private String error;

    public AuthTokenResponse() {
    }

    public static AuthTokenResponse error(String message) {
        AuthTokenResponse response = new AuthTokenResponse();
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

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    @Override
    public String toString() {
        return "AuthTokenResponse{" +
                "success=" + success +
                ", tokenType='" + tokenType + '\'' +
                ", expiresIn=" + expiresIn +
                ", username='" + username + '\'' +
                ", roles=" + roles +
                ", error='" + error + '\'' +
                '}';
    }
}
