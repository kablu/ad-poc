package com.company.ra.dto;

/**
 * Response DTO for authentication challenge
 */
public class ChallengeResponse {

    private boolean success = true;
    private String challengeId;
    private String challenge;
    private String salt;
    private long expiresAt;
    private String error;

    public ChallengeResponse() {
    }

    public static ChallengeResponse error(String message) {
        ChallengeResponse response = new ChallengeResponse();
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

    public String getChallengeId() {
        return challengeId;
    }

    public void setChallengeId(String challengeId) {
        this.challengeId = challengeId;
    }

    public String getChallenge() {
        return challenge;
    }

    public void setChallenge(String challenge) {
        this.challenge = challenge;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(long expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    @Override
    public String toString() {
        return "ChallengeResponse{" +
                "success=" + success +
                ", challengeId='" + challengeId + '\'' +
                ", expiresAt=" + expiresAt +
                ", error='" + error + '\'' +
                '}';
    }
}
