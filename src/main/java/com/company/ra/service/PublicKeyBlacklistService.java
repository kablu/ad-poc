package com.company.ra.service;

import com.company.ra.entity.PublicKeyBlacklist;
import com.company.ra.repository.PublicKeyBlacklistRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Service for managing public key blacklist
 */
@Service
public class PublicKeyBlacklistService {

    private static final Logger logger = LoggerFactory.getLogger(PublicKeyBlacklistService.class);

    @Autowired
    private PublicKeyBlacklistRepository publicKeyBlacklistRepository;

    /**
     * Check if public key hash is blacklisted
     *
     * @param publicKeyHash SHA-256 hash of public key (Base64-encoded)
     * @return true if blacklisted
     */
    public boolean isBlacklisted(String publicKeyHash) {
        return publicKeyBlacklistRepository.existsByPublicKeyHash(publicKeyHash);
    }

    /**
     * Add public key hash to blacklist
     *
     * @param publicKeyHash SHA-256 hash of public key (Base64-encoded)
     * @param reason Reason for blacklisting
     * @param addedBy Username who added the entry
     */
    @Transactional
    public void addToBlacklist(String publicKeyHash, String reason, String addedBy) {
        if (isBlacklisted(publicKeyHash)) {
            logger.warn("Public key hash already blacklisted: {}", publicKeyHash);
            return;
        }

        PublicKeyBlacklist entry = new PublicKeyBlacklist();
        entry.setPublicKeyHash(publicKeyHash);
        entry.setReason(reason);
        entry.setAddedBy(addedBy);
        entry.setAddedAt(Instant.now());

        publicKeyBlacklistRepository.save(entry);
        logger.info("Public key hash added to blacklist: {} by {}, reason: {}",
            publicKeyHash, addedBy, reason);
    }

    /**
     * Remove public key hash from blacklist
     *
     * @param publicKeyHash SHA-256 hash of public key (Base64-encoded)
     */
    @Transactional
    public void removeFromBlacklist(String publicKeyHash) {
        publicKeyBlacklistRepository.deleteByPublicKeyHash(publicKeyHash);
        logger.info("Public key hash removed from blacklist: {}", publicKeyHash);
    }
}
