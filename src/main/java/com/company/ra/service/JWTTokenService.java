package com.company.ra.service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.company.ra.dto.ADUserAttributes;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;

/**
 * Service for generating and validating JWT tokens
 */
@Service
public class JWTTokenService {

    private static final Logger logger = LoggerFactory.getLogger(JWTTokenService.class);

    @Value("${jwt.secret:change-this-secret-key-in-production-min-256-bits}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400}") // Default: 24 hours in seconds
    private long jwtExpirationSeconds;

    @Value("${jwt.issuer:RA-Service}")
    private String jwtIssuer;

    private Key signingKey;

    @PostConstruct
    public void init() {
        // Generate signing key from secret
        signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        logger.info("JWT Token Service initialized with expiration: {} seconds", jwtExpirationSeconds);
    }

    /**
     * Generate JWT token for authenticated user
     *
     * @param userDetails User details from AD
     * @return JWT token string
     */
    public String generateToken(ADUserAttributes userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", userDetails.getUsername());
        claims.put("commonName", userDetails.getCommonName());
        claims.put("email", userDetails.getEmail());
        claims.put("roles", userDetails.getRoles());

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + (jwtExpirationSeconds * 1000));

        String token = Jwts.builder()
            .setClaims(claims)
            .setSubject(userDetails.getUsername())
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .setIssuer(jwtIssuer)
            .signWith(signingKey, SignatureAlgorithm.HS256)
            .compact();

        logger.debug("JWT token generated for user: {}", userDetails.getUsername());
        return token;
    }

    /**
     * Validate JWT token
     *
     * @param token JWT token
     * @return true if valid
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            logger.debug("JWT token expired: {}", e.getMessage());
            return false;
        } catch (UnsupportedJwtException e) {
            logger.warn("Unsupported JWT token: {}", e.getMessage());
            return false;
        } catch (MalformedJwtException e) {
            logger.warn("Malformed JWT token: {}", e.getMessage());
            return false;
        } catch (SignatureException e) {
            logger.warn("Invalid JWT signature: {}", e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            logger.warn("JWT claims string is empty: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extract username from JWT token
     *
     * @param token JWT token
     * @return Username or null
     */
    public String extractUsername(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
            return claims.getSubject();
        } catch (Exception e) {
            logger.error("Error extracting username from JWT token", e);
            return null;
        }
    }

    /**
     * Extract roles from JWT token
     *
     * @param token JWT token
     * @return List of roles or empty list
     */
    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
            return (List<String>) claims.get("roles");
        } catch (Exception e) {
            logger.error("Error extracting roles from JWT token", e);
            return List.of();
        }
    }

    /**
     * Extract all claims from JWT token
     *
     * @param token JWT token
     * @return Claims or null
     */
    public Claims extractAllClaims(String token) {
        try {
            return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
        } catch (Exception e) {
            logger.error("Error extracting claims from JWT token", e);
            return null;
        }
    }

    /**
     * Get token expiry time in seconds
     *
     * @return Expiry time in seconds
     */
    public long getTokenExpirySeconds() {
        return jwtExpirationSeconds;
    }

    /**
     * Invalidate token (add to blacklist if implemented)
     *
     * @param token JWT token
     */
    public void invalidateToken(String token) {
        // TODO: Implement token blacklist if needed
        // For now, tokens will remain valid until expiry
        logger.debug("Token invalidation requested (not implemented)");
    }

    /**
     * Check if token is expired
     *
     * @param token JWT token
     * @return true if expired
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = extractAllClaims(token);
            if (claims != null) {
                Date expiration = claims.getExpiration();
                return expiration.before(new Date());
            }
            return true;
        } catch (Exception e) {
            return true;
        }
    }
}
