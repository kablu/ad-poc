package com.company.ra.controller;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.company.ra.dto.AuthRequest;
import com.company.ra.dto.AuthTokenResponse;
import com.company.ra.dto.ChallengeRequest;
import com.company.ra.dto.ChallengeResponse;
import com.company.ra.dto.LogoutRequest;
import com.company.ra.dto.LogoutResponse;
import com.company.ra.dto.TokenVerificationRequest;
import com.company.ra.dto.TokenVerificationResponse;
import com.company.ra.service.ActiveDirectoryService;
import com.company.ra.service.ChallengeStore;
import com.company.ra.service.JWTTokenService;

/**
 * REST Controller for authentication operations
 * Implements challenge-response authentication with Active Directory
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationController {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationController.class);
    private static final int CHALLENGE_SIZE = 32;
    private static final int SALT_SIZE = 16;
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final int PBKDF2_ITERATIONS = 10000;
    private static final int KEY_LENGTH = 256;
    private static final Duration CHALLENGE_EXPIRY = Duration.ofMinutes(5);
    private static final Duration TIMESTAMP_TOLERANCE = Duration.ofMinutes(5);

    @Autowired
    private ChallengeStore challengeStore;

    @Autowired
    private ActiveDirectoryService activeDirectoryService;

    @Autowired
    private JWTTokenService jwtTokenService;

    /**
     * Step 1: Request authentication challenge
     *
     * @param request Challenge request containing username
     * @return Challenge response with challenge, salt, and challenge ID
     */
    @PostMapping("/challenge")
    public ResponseEntity<ChallengeResponse> requestChallenge(@RequestBody ChallengeRequest request) {
        try {
            logger.info("Challenge requested for username: {}", request.getUsername());

            // Validate username format
            if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ChallengeResponse.error("Username is required"));
            }

            // Generate random challenge (nonce)
            byte[] challenge = new byte[CHALLENGE_SIZE];
            SecureRandom secureRandom = SecureRandom.getInstanceStrong();
            secureRandom.nextBytes(challenge);

            // Generate salt for PBKDF2 key derivation
            byte[] salt = new byte[SALT_SIZE];
            secureRandom.nextBytes(salt);

            // Generate unique challenge ID
            String challengeId = UUID.randomUUID().toString();

            // Store challenge temporarily with expiration
            challengeStore.store(
                challengeId,
                challenge,
                salt,
                CHALLENGE_EXPIRY,
                request.getUsername()
            );

            // Create response
            ChallengeResponse response = new ChallengeResponse();
            response.setChallengeId(challengeId);
            response.setChallenge(Base64.getEncoder().encodeToString(challenge));
            response.setSalt(Base64.getEncoder().encodeToString(salt));
            response.setExpiresAt(Instant.now().plus(CHALLENGE_EXPIRY).toEpochMilli());

            logger.info("Challenge generated successfully for username: {}", request.getUsername());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error generating challenge for username: {}", request.getUsername(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ChallengeResponse.error("Failed to generate challenge"));
        }
    }

    /**
     * Step 3: Verify encrypted response and authenticate against AD
     *
     * @param request Authentication request with encrypted response
     * @return JWT token if authentication successful
     */
    @PostMapping("/login")
    public ResponseEntity<AuthTokenResponse> authenticate(@RequestBody AuthRequest request) {
        try {
            logger.info("Authentication attempt for username: {}", request.getUsername());

            // Validate request
            if (request.getUsername() == null || request.getChallengeId() == null
                || request.getEncryptedResponse() == null) {
                logger.warn("Invalid authentication request - missing required fields");
                return ResponseEntity.badRequest()
                    .body(AuthTokenResponse.error("Missing required fields"));
            }

            // Retrieve stored challenge
            ChallengeStore.StoredChallenge storedChallenge =
                challengeStore.retrieve(request.getChallengeId());

            if (storedChallenge == null) {
                logger.warn("Invalid or expired challenge ID: {}", request.getChallengeId());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AuthTokenResponse.error("Invalid or expired challenge"));
            }

            // Verify username matches
            if (!storedChallenge.getUsername().equals(request.getUsername())) {
                logger.warn("Username mismatch for challenge ID: {}", request.getChallengeId());
                challengeStore.invalidate(request.getChallengeId());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AuthTokenResponse.error("Invalid credentials"));
            }

            // Step 4: Authenticate against Active Directory
            boolean adAuthenticated = activeDirectoryService.authenticate(
                request.getUsername(),
                request.getEncryptedResponse(),
                storedChallenge.getChallenge(),
                storedChallenge.getSalt()
            );

            if (!adAuthenticated) {
                logger.warn("Active Directory authentication failed for username: {}",
                    request.getUsername());
                challengeStore.invalidate(request.getChallengeId());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AuthTokenResponse.error("Invalid credentials"));
            }

            // Invalidate challenge after successful use
            challengeStore.invalidate(request.getChallengeId());

            // Retrieve user details from AD
            var userDetails = activeDirectoryService.getUserDetails(request.getUsername());
            if (userDetails == null) {
                logger.error("Failed to retrieve user details from AD for: {}",
                    request.getUsername());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AuthTokenResponse.error("Failed to retrieve user details"));
            }

            // Step 5: Generate JWT token
            String jwtToken = jwtTokenService.generateToken(userDetails);

            // Create successful response
            AuthTokenResponse response = new AuthTokenResponse();
            response.setSuccess(true);
            response.setToken(jwtToken);
            response.setTokenType("Bearer");
            response.setExpiresIn(jwtTokenService.getTokenExpirySeconds());
            response.setUsername(request.getUsername());
            response.setRoles(userDetails.getRoles());

            logger.info("Authentication successful for username: {}", request.getUsername());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Authentication error for username: {}", request.getUsername(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(AuthTokenResponse.error("Authentication failed"));
        }
    }

    /**
     * Verify JWT token validity
     *
     * @param token JWT token to verify
     * @return Token verification response
     */
    @PostMapping("/verify")
    public ResponseEntity<TokenVerificationResponse> verifyToken(@RequestBody TokenVerificationRequest request) {
        try {
            String token = request.getToken();

            if (token == null || token.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(TokenVerificationResponse.invalid("Token is required"));
            }

            boolean isValid = jwtTokenService.validateToken(token);

            if (isValid) {
                String username = jwtTokenService.extractUsername(token);
                TokenVerificationResponse response = new TokenVerificationResponse();
                response.setValid(true);
                response.setUsername(username);
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.ok(TokenVerificationResponse.invalid("Invalid token"));
            }

        } catch (Exception e) {
            logger.error("Token verification error", e);
            return ResponseEntity.ok(TokenVerificationResponse.invalid("Invalid token"));
        }
    }

    /**
     * Logout endpoint - invalidates token
     *
     * @param request Logout request
     * @return Logout response
     */
    @PostMapping("/logout")
    public ResponseEntity<LogoutResponse> logout(@RequestBody LogoutRequest request) {
        try {
            // Add token to blacklist if token blacklisting is implemented
            jwtTokenService.invalidateToken(request.getToken());

            LogoutResponse response = new LogoutResponse();
            response.setSuccess(true);
            response.setMessage("Logged out successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Logout error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(LogoutResponse.error("Logout failed"));
        }
    }
}
