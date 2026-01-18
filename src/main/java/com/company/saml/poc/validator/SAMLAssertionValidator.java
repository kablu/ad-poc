package com.company.saml.poc.validator;

import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Conditions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.saml2.core.Saml2Error;
import org.springframework.security.saml2.core.Saml2ErrorCodes;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * SAML Assertion Validator
 *
 * Validates SAML assertions for security and integrity including:
 * - Assertion ID uniqueness (replay attack prevention)
 * - Issuer verification
 * - Time validity (NotBefore/NotOnOrAfter)
 * - Audience restriction
 * - Subject confirmation
 * - Custom business rules
 *
 * @author SAML POC Team
 */
@Component
public class SAMLAssertionValidator {

    @Value("${saml.sp.entity-id:https://localhost:8443/saml/metadata}")
    private String expectedAudience;

    @Value("${saml.idp.entity-id:https://idp.example.com}")
    private String expectedIssuer;

    // In-memory store for used assertion IDs (in production, use Redis or database)
    private final Set<String> usedAssertionIds = new HashSet<>();

    /**
     * Validate SAML Assertion for security and integrity
     *
     * @param assertion SAML assertion to validate
     * @throws Saml2AuthenticationException if validation fails
     */
    public void validateAssertion(Assertion assertion) {
        // 1. Validate Assertion ID (must be unique to prevent replay attacks)
        validateAssertionId(assertion);

        // 2. Validate Issuer
        validateIssuer(assertion);

        // 3. Validate Conditions (time validity)
        validateConditions(assertion);

        // 4. Validate Audience Restriction
        validateAudience(assertion);

        // 5. Validate Subject Confirmation
        validateSubjectConfirmation(assertion);

        // 6. Custom business logic validation
        validateCustomRules(assertion);
    }

    /**
     * Validate Assertion ID is unique (prevent replay attacks)
     */
    private void validateAssertionId(Assertion assertion) {
        String assertionId = assertion.getID();

        if (assertionId == null || assertionId.isEmpty()) {
            throw new Saml2AuthenticationException(
                new Saml2Error(Saml2ErrorCodes.INVALID_ASSERTION,
                    "Assertion ID is missing"));
        }

        // Check if assertion has already been used
        synchronized (usedAssertionIds) {
            if (usedAssertionIds.contains(assertionId)) {
                throw new Saml2AuthenticationException(
                    new Saml2Error(Saml2ErrorCodes.INVALID_ASSERTION,
                        "Assertion has already been used (replay attack detected)"));
            }
            usedAssertionIds.add(assertionId);
        }
    }

    /**
     * Validate Issuer matches expected IdP
     */
    private void validateIssuer(Assertion assertion) {
        if (assertion.getIssuer() == null) {
            throw new Saml2AuthenticationException(
                new Saml2Error(Saml2ErrorCodes.INVALID_ISSUER,
                    "Assertion issuer is missing"));
        }

        String issuer = assertion.getIssuer().getValue();

        if (!expectedIssuer.equals(issuer)) {
            throw new Saml2AuthenticationException(
                new Saml2Error(Saml2ErrorCodes.INVALID_ISSUER,
                    String.format("Assertion issuer '%s' does not match expected IdP '%s'",
                        issuer, expectedIssuer)));
        }
    }

    /**
     * Validate Conditions (NotBefore/NotOnOrAfter timestamps)
     */
    private void validateConditions(Assertion assertion) {
        Conditions conditions = assertion.getConditions();

        if (conditions == null) {
            throw new Saml2AuthenticationException(
                new Saml2Error(Saml2ErrorCodes.INVALID_ASSERTION,
                    "Assertion conditions are missing"));
        }

        Instant now = Instant.now();
        Instant notBefore = conditions.getNotBefore();
        Instant notOnOrAfter = conditions.getNotOnOrAfter();

        // Check if assertion is not yet valid
        if (notBefore != null && now.isBefore(notBefore)) {
            throw new Saml2AuthenticationException(
                new Saml2Error(Saml2ErrorCodes.INVALID_ASSERTION,
                    String.format("Assertion is not yet valid. Current time: %s, NotBefore: %s",
                        now, notBefore)));
        }

        // Check if assertion has expired
        if (notOnOrAfter != null && now.isAfter(notOnOrAfter)) {
            throw new Saml2AuthenticationException(
                new Saml2Error(Saml2ErrorCodes.INVALID_ASSERTION,
                    String.format("Assertion has expired. Current time: %s, NotOnOrAfter: %s",
                        now, notOnOrAfter)));
        }
    }

    /**
     * Validate Audience restriction matches this SP
     */
    private void validateAudience(Assertion assertion) {
        Conditions conditions = assertion.getConditions();

        if (conditions.getAudienceRestrictions().isEmpty()) {
            throw new Saml2AuthenticationException(
                new Saml2Error(Saml2ErrorCodes.INVALID_ASSERTION,
                    "Assertion does not contain audience restriction"));
        }

        boolean audienceMatch = conditions.getAudienceRestrictions().stream()
            .flatMap(restriction -> restriction.getAudiences().stream())
            .anyMatch(audience -> expectedAudience.equals(audience.getURI()));

        if (!audienceMatch) {
            throw new Saml2AuthenticationException(
                new Saml2Error(Saml2ErrorCodes.INVALID_ASSERTION,
                    String.format("Assertion audience does not match this service provider. Expected: %s",
                        expectedAudience)));
        }
    }

    /**
     * Validate Subject Confirmation
     */
    private void validateSubjectConfirmation(Assertion assertion) {
        if (assertion.getSubject() == null ||
            assertion.getSubject().getSubjectConfirmations().isEmpty()) {
            throw new Saml2AuthenticationException(
                new Saml2Error(Saml2ErrorCodes.INVALID_ASSERTION,
                    "Assertion subject confirmation is missing"));
        }

        // Validate bearer confirmation method
        boolean hasValidConfirmation = assertion.getSubject().getSubjectConfirmations().stream()
            .anyMatch(confirmation ->
                "urn:oasis:names:tc:SAML:2.0:cm:bearer".equals(confirmation.getMethod()));

        if (!hasValidConfirmation) {
            throw new Saml2AuthenticationException(
                new Saml2Error(Saml2ErrorCodes.INVALID_ASSERTION,
                    "Assertion must use bearer subject confirmation method"));
        }
    }

    /**
     * Custom business rules validation
     */
    private void validateCustomRules(Assertion assertion) {
        // Add custom validation logic here
        // For example:
        // - Validate that user has required attributes (email, firstName, etc.)
        // - Check if user is from allowed department
        // - Verify user has at least one role
        // - Custom security policies
    }

    /**
     * Clear used assertion IDs (should be called periodically in production)
     * In production, use Redis with TTL or database with expiration
     */
    public void clearExpiredAssertionIds() {
        // This is a simplified implementation
        // In production, implement with proper TTL based on assertion validity period
        synchronized (usedAssertionIds) {
            usedAssertionIds.clear();
        }
    }
}
