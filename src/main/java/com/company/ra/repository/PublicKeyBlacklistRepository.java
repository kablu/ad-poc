package com.company.ra.repository;

import com.company.ra.entity.PublicKeyBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for PublicKeyBlacklist entity
 */
@Repository
public interface PublicKeyBlacklistRepository extends JpaRepository<PublicKeyBlacklist, Long> {

    /**
     * Check if public key hash exists in blacklist
     *
     * @param publicKeyHash Public key hash
     * @return true if blacklisted
     */
    boolean existsByPublicKeyHash(String publicKeyHash);

    /**
     * Find blacklist entry by public key hash
     *
     * @param publicKeyHash Public key hash
     * @return Optional PublicKeyBlacklist
     */
    Optional<PublicKeyBlacklist> findByPublicKeyHash(String publicKeyHash);

    /**
     * Delete blacklist entry by public key hash
     *
     * @param publicKeyHash Public key hash
     */
    void deleteByPublicKeyHash(String publicKeyHash);
}
