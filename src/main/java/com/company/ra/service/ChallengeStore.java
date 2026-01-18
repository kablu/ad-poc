package com.company.ra.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Service for storing and managing authentication challenges
 * Uses in-memory storage with automatic expiration
 */
@Service
public class ChallengeStore {

    private static final Logger logger = LoggerFactory.getLogger(ChallengeStore.class);

    private final Map<String, StoredChallenge> challenges = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();

    public ChallengeStore() {
        // Schedule cleanup task to run every minute
        cleanupExecutor.scheduleAtFixedRate(this::cleanupExpiredChallenges, 1, 1, TimeUnit.MINUTES);
        logger.info("ChallengeStore initialized with automatic cleanup");
    }

    /**
     * Store a challenge
     *
     * @param challengeId Unique challenge ID
     * @param challenge Challenge bytes
     * @param salt Salt bytes
     * @param expiryDuration Duration until expiry
     * @param username Username associated with challenge
     */
    public void store(String challengeId, byte[] challenge, byte[] salt,
                     Duration expiryDuration, String username) {
        Instant expiresAt = Instant.now().plus(expiryDuration);
        StoredChallenge storedChallenge = new StoredChallenge(
            challengeId,
            challenge,
            salt,
            username,
            expiresAt
        );
        challenges.put(challengeId, storedChallenge);
        logger.debug("Challenge stored: {}, expires at: {}", challengeId, expiresAt);
    }

    /**
     * Retrieve and validate a challenge
     *
     * @param challengeId Challenge ID
     * @return StoredChallenge or null if not found or expired
     */
    public StoredChallenge retrieve(String challengeId) {
        StoredChallenge storedChallenge = challenges.get(challengeId);

        if (storedChallenge == null) {
            logger.debug("Challenge not found: {}", challengeId);
            return null;
        }

        if (storedChallenge.isExpired()) {
            logger.debug("Challenge expired: {}", challengeId);
            challenges.remove(challengeId);
            return null;
        }

        logger.debug("Challenge retrieved: {}", challengeId);
        return storedChallenge;
    }

    /**
     * Invalidate a challenge (after use or failed authentication)
     *
     * @param challengeId Challenge ID
     */
    public void invalidate(String challengeId) {
        StoredChallenge removed = challenges.remove(challengeId);
        if (removed != null) {
            logger.debug("Challenge invalidated: {}", challengeId);
        }
    }

    /**
     * Clean up expired challenges
     */
    private void cleanupExpiredChallenges() {
        int removed = 0;
        Instant now = Instant.now();

        for (Map.Entry<String, StoredChallenge> entry : challenges.entrySet()) {
            if (entry.getValue().getExpiresAt().isBefore(now)) {
                challenges.remove(entry.getKey());
                removed++;
            }
        }

        if (removed > 0) {
            logger.debug("Cleaned up {} expired challenges", removed);
        }
    }

    /**
     * Get current number of stored challenges
     *
     * @return Number of challenges
     */
    public int size() {
        return challenges.size();
    }

    /**
     * Clear all challenges (for testing)
     */
    public void clear() {
        challenges.clear();
        logger.debug("All challenges cleared");
    }

    /**
     * Shutdown cleanup executor
     */
    public void shutdown() {
        cleanupExecutor.shutdown();
        logger.info("ChallengeStore shutdown");
    }

    /**
     * Inner class representing a stored challenge
     */
    public static class StoredChallenge {
        private final String challengeId;
        private final byte[] challenge;
        private final byte[] salt;
        private final String username;
        private final Instant expiresAt;

        public StoredChallenge(String challengeId, byte[] challenge, byte[] salt,
                             String username, Instant expiresAt) {
            this.challengeId = challengeId;
            this.challenge = challenge;
            this.salt = salt;
            this.username = username;
            this.expiresAt = expiresAt;
        }

        public String getChallengeId() {
            return challengeId;
        }

        public byte[] getChallenge() {
            return challenge;
        }

        public byte[] getSalt() {
            return salt;
        }

        public String getUsername() {
            return username;
        }

        public Instant getExpiresAt() {
            return expiresAt;
        }

        public boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
