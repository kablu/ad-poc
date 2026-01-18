package com.company.ra.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for authentication with encrypted response
 */
public class AuthRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Challenge ID is required")
    private String challengeId;

    @NotBlank(message = "Encrypted response is required")
    private String encryptedResponse;

    public AuthRequest() {
    }

    public AuthRequest(String username, String challengeId, String encryptedResponse) {
        this.username = username;
        this.challengeId = challengeId;
        this.encryptedResponse = encryptedResponse;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getChallengeId() {
        return challengeId;
    }

    public void setChallengeId(String challengeId) {
        this.challengeId = challengeId;
    }

    public String getEncryptedResponse() {
        return encryptedResponse;
    }

    public void setEncryptedResponse(String encryptedResponse) {
        this.encryptedResponse = encryptedResponse;
    }

    @Override
    public String toString() {
        return "AuthRequest{" +
                "username='" + username + '\'' +
                ", challengeId='" + challengeId + '\'' +
                '}';
    }
}
