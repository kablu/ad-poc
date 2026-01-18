package com.company.ra.dto;

import com.company.ra.entity.CertificateRequest;
import java.util.List;

/**
 * Response DTO for certificate request list
 */
public class CertificateRequestListResponse {

    private List<CertificateRequest> requests;
    private long totalCount;
    private int page;
    private int size;

    public CertificateRequestListResponse() {
    }

    public List<CertificateRequest> getRequests() {
        return requests;
    }

    public void setRequests(List<CertificateRequest> requests) {
        this.requests = requests;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    @Override
    public String toString() {
        return "CertificateRequestListResponse{" +
                "totalCount=" + totalCount +
                ", page=" + page +
                ", size=" + size +
                ", requestsCount=" + (requests != null ? requests.size() : 0) +
                '}';
    }
}
