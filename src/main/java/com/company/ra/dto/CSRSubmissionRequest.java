package com.company.ra.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for PKCS#10 CSR submission
 */
public class CSRSubmissionRequest {

    @NotBlank(message = "CSR PEM data is required")
    private String csrPem;

    @NotBlank(message = "Certificate type is required")
    private String certificateType;

    private String comments;

    public CSRSubmissionRequest() {
    }

    public CSRSubmissionRequest(String csrPem, String certificateType) {
        this.csrPem = csrPem;
        this.certificateType = certificateType;
    }

    public String getCsrPem() {
        return csrPem;
    }

    public void setCsrPem(String csrPem) {
        this.csrPem = csrPem;
    }

    public String getCertificateType() {
        return certificateType;
    }

    public void setCertificateType(String certificateType) {
        this.certificateType = certificateType;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    @Override
    public String toString() {
        return "CSRSubmissionRequest{" +
                "certificateType='" + certificateType + '\'' +
                ", comments='" + comments + '\'' +
                '}';
    }
}
