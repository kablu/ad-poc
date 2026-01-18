package com.company.ra.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for authentication challenge
 */
public class ChallengeRequest {

    @NotBlank(message = "Username is required")
    private String username;

    public ChallengeRequest() {
    }

    public ChallengeRequest(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String toString() {
        return "ChallengeRequest{" +
                "username='" + username + '\'' +
                '}';
    }
}
