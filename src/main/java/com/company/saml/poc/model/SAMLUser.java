package com.company.saml.poc.model;

import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * SAML User Model
 *
 * Represents a user authenticated via SAML with attributes extracted
 * from the SAML assertion.
 *
 * @author SAML POC Team
 */
public class SAMLUser implements Serializable {

    private static final long serialVersionUID = 1L;

    private String userId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String displayName;
    private String department;
    private List<String> roles;
    private String sessionIndex;
    private String nameId;
    private String registrationId;

    public SAMLUser() {
        this.roles = new ArrayList<>();
    }

    /**
     * Create SAMLUser from SAML Assertion Principal
     */
    public static SAMLUser fromSAMLAssertion(Saml2AuthenticatedPrincipal principal) {
        SAMLUser user = new SAMLUser();

        // Set basic attributes
        user.setUsername(principal.getName());
        user.setNameId(principal.getName());
        user.setRegistrationId(principal.getRelyingPartyRegistrationId());

        // Extract attributes from SAML assertion
        user.setEmail(getAttributeValue(principal, "email", "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress"));
        user.setFirstName(getAttributeValue(principal, "firstName", "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/givenname"));
        user.setLastName(getAttributeValue(principal, "lastName", "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname"));
        user.setDisplayName(getAttributeValue(principal, "displayName", "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name"));
        user.setDepartment(getAttributeValue(principal, "department", "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/department"));

        // Extract roles
        List<String> roles = new ArrayList<>();
        List<Object> roleAttributes = principal.getAttribute("role");
        if (roleAttributes == null) {
            roleAttributes = principal.getAttribute("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role");
        }
        if (roleAttributes == null) {
            roleAttributes = principal.getAttribute("http://schemas.microsoft.com/ws/2008/06/identity/claims/role");
        }

        if (roleAttributes != null) {
            for (Object roleObj : roleAttributes) {
                roles.add(roleObj.toString());
            }
        }
        user.setRoles(roles);

        // Extract session index
        if (!principal.getSessionIndexes().isEmpty()) {
            user.setSessionIndex(principal.getSessionIndexes().iterator().next());
        }

        return user;
    }

    /**
     * Get attribute value from SAML assertion with fallback to different claim URIs
     */
    private static String getAttributeValue(Saml2AuthenticatedPrincipal principal, String... attributeNames) {
        for (String attributeName : attributeNames) {
            String value = principal.getFirstAttribute(attributeName);
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }
        return null;
    }

    /**
     * Get primary role based on priority
     */
    public String getPrimaryRole() {
        if (roles == null || roles.isEmpty()) {
            return "END_ENTITY";
        }

        // Priority order: ADMIN > OFFICER > OPERATOR > AUDITOR > END_ENTITY
        if (roles.contains("RA_ADMIN") || roles.contains("ROLE_RA_ADMIN")) {
            return "RA_ADMIN";
        } else if (roles.contains("RA_OFFICER") || roles.contains("ROLE_RA_OFFICER")) {
            return "RA_OFFICER";
        } else if (roles.contains("RA_OPERATOR") || roles.contains("ROLE_RA_OPERATOR")) {
            return "RA_OPERATOR";
        } else if (roles.contains("AUDITOR") || roles.contains("ROLE_AUDITOR")) {
            return "AUDITOR";
        }

        return "END_ENTITY";
    }

    /**
     * Check if user has specific role
     */
    public boolean hasRole(String role) {
        if (roles == null) {
            return false;
        }
        return roles.contains(role) || roles.contains("ROLE_" + role);
    }

    // Getters and Setters

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public String getSessionIndex() {
        return sessionIndex;
    }

    public void setSessionIndex(String sessionIndex) {
        this.sessionIndex = sessionIndex;
    }

    public String getNameId() {
        return nameId;
    }

    public void setNameId(String nameId) {
        this.nameId = nameId;
    }

    public String getRegistrationId() {
        return registrationId;
    }

    public void setRegistrationId(String registrationId) {
        this.registrationId = registrationId;
    }

    @Override
    public String toString() {
        return "SAMLUser{" +
                "userId='" + userId + '\'' +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", displayName='" + displayName + '\'' +
                ", department='" + department + '\'' +
                ", roles=" + roles +
                ", primaryRole='" + getPrimaryRole() + '\'' +
                '}';
    }
}
