package com.company.saml.poc.service;

import com.company.saml.poc.model.SAMLUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * SAML User Service
 *
 * Service layer for managing SAML authenticated users including:
 * - Retrieving current authenticated user
 * - Extracting user attributes from SAML assertion
 * - Role-based access control checks
 *
 * @author SAML POC Team
 */
@Service
public class SAMLUserService {

    /**
     * Get currently authenticated SAML user
     *
     * @return SAMLUser if authenticated, null otherwise
     */
    public SAMLUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null &&
            authentication.getPrincipal() instanceof Saml2AuthenticatedPrincipal) {

            Saml2AuthenticatedPrincipal principal =
                (Saml2AuthenticatedPrincipal) authentication.getPrincipal();

            return SAMLUser.fromSAMLAssertion(principal);
        }

        return null;
    }

    /**
     * Check if user is authenticated via SAML
     *
     * @return true if SAML authenticated, false otherwise
     */
    public boolean isAuthenticated() {
        return getCurrentUser() != null;
    }

    /**
     * Check if current user has specific role
     *
     * @param role Role to check (e.g., "RA_ADMIN", "RA_OFFICER")
     * @return true if user has role, false otherwise
     */
    public boolean hasRole(String role) {
        SAMLUser user = getCurrentUser();
        return user != null && user.hasRole(role);
    }

    /**
     * Get user's primary role for RA system
     *
     * @return Primary role (RA_ADMIN, RA_OFFICER, RA_OPERATOR, AUDITOR, END_ENTITY)
     */
    public String getPrimaryRole() {
        SAMLUser user = getCurrentUser();
        if (user == null) {
            return null;
        }
        return user.getPrimaryRole();
    }

    /**
     * Get all SAML attributes for current user
     *
     * @return Map of attribute names to list of values
     */
    public Map<String, List<Object>> getUserAttributes() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null &&
            authentication.getPrincipal() instanceof Saml2AuthenticatedPrincipal) {

            Saml2AuthenticatedPrincipal principal =
                (Saml2AuthenticatedPrincipal) authentication.getPrincipal();

            return principal.getAttributes();
        }

        return null;
    }

    /**
     * Get specific SAML attribute value
     *
     * @param attributeName Name of the attribute
     * @return Attribute value or null
     */
    public String getAttribute(String attributeName) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null &&
            authentication.getPrincipal() instanceof Saml2AuthenticatedPrincipal) {

            Saml2AuthenticatedPrincipal principal =
                (Saml2AuthenticatedPrincipal) authentication.getPrincipal();

            return principal.getFirstAttribute(attributeName);
        }

        return null;
    }

    /**
     * Get SAML session index for logout
     *
     * @return Session index or null
     */
    public String getSessionIndex() {
        SAMLUser user = getCurrentUser();
        return user != null ? user.getSessionIndex() : null;
    }

    /**
     * Check if user has admin privileges
     *
     * @return true if user is RA_ADMIN
     */
    public boolean isAdmin() {
        return hasRole("RA_ADMIN");
    }

    /**
     * Check if user has officer privileges
     *
     * @return true if user is RA_OFFICER or higher
     */
    public boolean isOfficer() {
        return hasRole("RA_ADMIN") || hasRole("RA_OFFICER");
    }

    /**
     * Check if user has operator privileges
     *
     * @return true if user is RA_OPERATOR or higher
     */
    public boolean isOperator() {
        return hasRole("RA_ADMIN") || hasRole("RA_OFFICER") || hasRole("RA_OPERATOR");
    }

    /**
     * Get user's email from SAML assertion
     *
     * @return User email or null
     */
    public String getUserEmail() {
        SAMLUser user = getCurrentUser();
        return user != null ? user.getEmail() : null;
    }

    /**
     * Get user's display name from SAML assertion
     *
     * @return User display name or null
     */
    public String getUserDisplayName() {
        SAMLUser user = getCurrentUser();
        return user != null ? user.getDisplayName() : null;
    }
}
