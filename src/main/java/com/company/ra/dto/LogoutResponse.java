package com.company.ra.dto;

/**
 * Response DTO for logout
 */
public class LogoutResponse {

    private boolean success = true;
    private String message;
    private String error;

    public LogoutResponse() {
    }

    public static LogoutResponse error(String message) {
        LogoutResponse response = new LogoutResponse();
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

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    @Override
    public String toString() {
        return "LogoutResponse{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", error='" + error + '\'' +
                '}';
    }
}
