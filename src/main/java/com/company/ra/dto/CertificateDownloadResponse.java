package com.company.ra.dto;

/**
 * Response DTO for certificate download
 */
public class CertificateDownloadResponse {

    private boolean success = true;
    private String requestId;
    private String certificatePem;
    private String serialNumber;
    private String error;

    public CertificateDownloadResponse() {
    }

    public static CertificateDownloadResponse error(String message) {
        CertificateDownloadResponse response = new CertificateDownloadResponse();
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

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getCertificatePem() {
        return certificatePem;
    }

    public void setCertificatePem(String certificatePem) {
        this.certificatePem = certificatePem;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    @Override
    public String toString() {
        return "CertificateDownloadResponse{" +
                "success=" + success +
                ", requestId='" + requestId + '\'' +
                ", serialNumber='" + serialNumber + '\'' +
                ", error='" + error + '\'' +
                '}';
    }
}
