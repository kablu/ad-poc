package com.company.saml.poc.controller;

import com.company.saml.poc.model.SAMLUser;
import com.company.saml.poc.service.SAMLUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Dashboard Controller
 *
 * Handles authenticated user dashboard with role-based routing
 *
 * @author SAML POC Team
 */
@Controller
public class DashboardController {

    @Autowired
    private SAMLUserService samlUserService;

    /**
     * Main dashboard - redirects based on user role
     */
    @GetMapping("/dashboard")
    public String dashboard(
            @AuthenticationPrincipal Saml2AuthenticatedPrincipal principal,
            Model model) {

        // Extract user information from SAML assertion
        SAMLUser user = SAMLUser.fromSAMLAssertion(principal);

        model.addAttribute("user", user);
        model.addAttribute("username", user.getUsername());
        model.addAttribute("email", user.getEmail());
        model.addAttribute("displayName", user.getDisplayName());
        model.addAttribute("department", user.getDepartment());
        model.addAttribute("roles", user.getRoles());
        model.addAttribute("primaryRole", user.getPrimaryRole());

        // Redirect based on primary role
        String primaryRole = user.getPrimaryRole();
        if ("RA_ADMIN".equals(primaryRole)) {
            return "redirect:/admin/dashboard";
        } else if ("RA_OFFICER".equals(primaryRole)) {
            return "redirect:/officer/dashboard";
        } else if ("RA_OPERATOR".equals(primaryRole)) {
            return "redirect:/operator/dashboard";
        } else if ("AUDITOR".equals(primaryRole)) {
            return "redirect:/auditor/dashboard";
        } else {
            return "redirect:/user/dashboard";
        }
    }

    /**
     * User dashboard (End Entity)
     */
    @GetMapping("/user/dashboard")
    public String userDashboard(Model model) {
        SAMLUser user = samlUserService.getCurrentUser();
        model.addAttribute("user", user);
        model.addAttribute("title", "User Dashboard");
        return "dashboard/user";
    }

    /**
     * Operator dashboard
     */
    @GetMapping("/operator/dashboard")
    public String operatorDashboard(Model model) {
        SAMLUser user = samlUserService.getCurrentUser();
        model.addAttribute("user", user);
        model.addAttribute("title", "Operator Dashboard");
        return "dashboard/operator";
    }

    /**
     * Officer dashboard
     */
    @GetMapping("/officer/dashboard")
    public String officerDashboard(Model model) {
        SAMLUser user = samlUserService.getCurrentUser();
        model.addAttribute("user", user);
        model.addAttribute("title", "RA Officer Dashboard");
        return "dashboard/officer";
    }

    /**
     * Admin dashboard
     */
    @GetMapping("/admin/dashboard")
    public String adminDashboard(Model model) {
        SAMLUser user = samlUserService.getCurrentUser();
        model.addAttribute("user", user);
        model.addAttribute("title", "RA Administrator Dashboard");
        return "dashboard/admin";
    }

    /**
     * Auditor dashboard
     */
    @GetMapping("/auditor/dashboard")
    public String auditorDashboard(Model model) {
        SAMLUser user = samlUserService.getCurrentUser();
        model.addAttribute("user", user);
        model.addAttribute("title", "Auditor Dashboard");
        return "dashboard/auditor";
    }
}
