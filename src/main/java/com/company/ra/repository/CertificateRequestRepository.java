package com.company.ra.repository;

import com.company.ra.entity.CertificateRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for CertificateRequest entity
 */
@Repository
public interface CertificateRequestRepository extends JpaRepository<CertificateRequest, Long> {

    /**
     * Find certificate request by request ID
     *
     * @param requestId Request ID
     * @return Optional CertificateRequest
     */
    Optional<CertificateRequest> findByRequestId(String requestId);

    /**
     * Find certificate request by certificate serial number
     *
     * @param certificateSerialNumber Certificate serial number
     * @return Optional CertificateRequest
     */
    Optional<CertificateRequest> findByCertificateSerialNumber(String certificateSerialNumber);

    /**
     * Find certificate requests by username
     *
     * @param username Username
     * @return List of CertificateRequests
     */
    List<CertificateRequest> findByUsername(String username);

    /**
     * Find certificate requests by status
     *
     * @param status Status
     * @param pageable Pagination info
     * @return List of CertificateRequests
     */
    List<CertificateRequest> findByStatus(String status, Pageable pageable);

    /**
     * Count certificate requests by status
     *
     * @param status Status
     * @return Count
     */
    long countByStatus(String status);

    /**
     * Find certificate requests by username and status
     *
     * @param username Username
     * @param status Status
     * @return List of CertificateRequests
     */
    List<CertificateRequest> findByUsernameAndStatus(String username, String status);

    /**
     * Check if public key hash exists
     *
     * @param publicKeyHash Public key hash
     * @return true if exists
     */
    boolean existsByPublicKeyHash(String publicKeyHash);
}
