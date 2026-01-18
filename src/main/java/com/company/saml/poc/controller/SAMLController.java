package com.company.saml.poc.controller;

import com.company.saml.poc.model.SAMLUser;
import com.company.saml.poc.service.SAMLUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

/**
 * SAML Controller
 *
 * Handles SAML-specific endpoints including:
 * - User info display
 * - SAML attributes inspection
 * - Metadata endpoint (handled by Spring Security automatically)
 *
 * @author SAML POC Team
 */
@Controller
@RequestMapping("/saml")
public class SAMLController {

    @Autowired
    private SAMLUserService samlUserService;

    /**
     * Display user info from SAML assertion
     */
    @GetMapping("/user-info")
    public String userInfo(
            @AuthenticationPrincipal Saml2AuthenticatedPrincipal principal,
            Model model) {

        if (principal != null) {
            SAMLUser user = SAMLUser.fromSAMLAssertion(principal);

            model.addAttribute("user", user);
            model.addAttribute("username", principal.getName());
            model.addAttribute("attributes", principal.getAttributes());
            model.addAttribute("sessionIndexes", principal.getSessionIndexes());
            model.addAttribute("registrationId", principal.getRelyingPartyRegistrationId());

            // Extract specific attributes
            model.addAttribute("email", user.getEmail());
            model.addAttribute("firstName", user.getFirstName());
            model.addAttribute("lastName", user.getLastName());
            model.addAttribute("department", user.getDepartment());
            model.addAttribute("displayName", user.getDisplayName());
            model.addAttribute("roles", user.getRoles());
            model.addAttribute("primaryRole", user.getPrimaryRole());
        }

        model.addAttribute("title", "SAML User Information");
        return "saml/user-info";
    }

    /**
     * REST endpoint to get user info as JSON
     */
    @GetMapping("/api/user-info")
    @ResponseBody
    public Map<String, Object> userInfoApi(
            @AuthenticationPrincipal Saml2AuthenticatedPrincipal principal) {

        Map<String, Object> response = new HashMap<>();

        if (principal != null) {
            SAMLUser user = SAMLUser.fromSAMLAssertion(principal);

            response.put("username", user.getUsername());
            response.put("email", user.getEmail());
            response.put("firstName", user.getFirstName());
            response.put("lastName", user.getLastName());
            response.put("displayName", user.getDisplayName());
            response.put("department", user.getDepartment());
            response.put("roles", user.getRoles());
            response.put("primaryRole", user.getPrimaryRole());
            response.put("sessionIndex", user.getSessionIndex());
            response.put("allAttributes", principal.getAttributes());
        }

        return response;
    }

    /**
     * Display all SAML attributes (for debugging)
     */
    @GetMapping("/attributes")
    public String attributes(
            @AuthenticationPrincipal Saml2AuthenticatedPrincipal principal,
            Model model) {

        if (principal != null) {
            model.addAttribute("attributes", principal.getAttributes());
            model.addAttribute("nameId", principal.getName());
            model.addAttribute("sessionIndexes", principal.getSessionIndexes());
        }

        model.addAttribute("title", "SAML Attributes");
        return "saml/attributes";
    }

    /**
     * SAML Metadata endpoint info
     * Note: Actual metadata is served by Spring Security at:
     * /saml2/service-provider-metadata/{registrationId}
     */
    @GetMapping("/metadata-info")
    public String metadataInfo(Model model) {
        model.addAttribute("title", "SAML Metadata");
        model.addAttribute("metadataUrl", "/saml2/service-provider-metadata/saml-poc");
        return "saml/metadata-info";
    }

    /**
     * Test endpoint to check SAML authentication status
     */
    @GetMapping("/status")
    @ResponseBody
    public Map<String, Object> status() {
        Map<String, Object> status = new HashMap<>();
        status.put("authenticated", samlUserService.isAuthenticated());
        status.put("user", samlUserService.getCurrentUser());
        status.put("primaryRole", samlUserService.getPrimaryRole());
        status.put("isAdmin", samlUserService.isAdmin());
        status.put("isOfficer", samlUserService.isOfficer());
        status.put("isOperator", samlUserService.isOperator());
        return status;
    }
}
