package com.company.saml.poc;

import com.company.saml.poc.model.SAMLUser;
import com.company.saml.poc.service.SAMLUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.saml2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * SAML Authentication Integration Tests
 *
 * Tests SAML authentication flow including:
 * - SP-initiated SSO
 * - User attribute extraction
 * - Role-based access control
 * - Dashboard routing
 *
 * @author SAML POC Team
 */
@SpringBootTest
@AutoConfigureMockMvc
public class SAMLAuthenticationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SAMLUserService samlUserService;

    /**
     * Test access to protected resource without authentication
     */
    @Test
    public void testAccessProtectedResourceWithoutAuth() throws Exception {
        mockMvc.perform(get("/dashboard"))
            .andExpect(status().is3xxRedirection());
    }

    /**
     * Test SAML authentication with RA Officer role
     */
    @Test
    public void testSAMLAuthenticationWithOfficerRole() throws Exception {
        mockMvc.perform(get("/dashboard")
            .with(saml2Login()
                .attributes(attrs -> {
                    attrs.put("email", "officer@example.com");
                    attrs.put("firstName", "John");
                    attrs.put("lastName", "Officer");
                    attrs.put("displayName", "John Officer");
                    attrs.put("department", "Security");
                    attrs.put("role", "RA_OFFICER");
                })
            ))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/officer/dashboard"));
    }

    /**
     * Test SAML authentication with Admin role
     */
    @Test
    public void testSAMLAuthenticationWithAdminRole() throws Exception {
        mockMvc.perform(get("/dashboard")
            .with(saml2Login()
                .attributes(attrs -> {
                    attrs.put("email", "admin@example.com");
                    attrs.put("firstName", "Admin");
                    attrs.put("lastName", "User");
                    attrs.put("displayName", "Admin User");
                    attrs.put("department", "IT");
                    attrs.put("role", "RA_ADMIN");
                })
            ))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/dashboard"));
    }

    /**
     * Test SAML authentication with End Entity role
     */
    @Test
    public void testSAMLAuthenticationWithEndEntityRole() throws Exception {
        mockMvc.perform(get("/dashboard")
            .with(saml2Login()
                .attributes(attrs -> {
                    attrs.put("email", "user@example.com");
                    attrs.put("firstName", "Regular");
                    attrs.put("lastName", "User");
                    attrs.put("displayName", "Regular User");
                    attrs.put("department", "Engineering");
                })
            ))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/user/dashboard"));
    }

    /**
     * Test user info extraction from SAML assertion
     */
    @Test
    public void testUserInfoExtraction() throws Exception {
        mockMvc.perform(get("/saml/user-info")
            .with(saml2Login()
                .attributes(attrs -> {
                    attrs.put("email", "test@example.com");
                    attrs.put("firstName", "Test");
                    attrs.put("lastName", "User");
                    attrs.put("displayName", "Test User");
                    attrs.put("department", "QA");
                    attrs.put("role", "RA_OPERATOR");
                })
            ))
            .andExpect(status().isOk())
            .andExpect(model().attributeExists("user"))
            .andExpect(model().attribute("email", "test@example.com"))
            .andExpect(model().attribute("firstName", "Test"))
            .andExpect(model().attribute("lastName", "User"))
            .andExpect(model().attribute("department", "QA"));
    }

    /**
     * Test role-based access control - Admin endpoint
     */
    @Test
    public void testAdminEndpointWithOfficerRole() throws Exception {
        mockMvc.perform(get("/admin/dashboard")
            .with(saml2Login()
                .authorities(authorities -> authorities.add(() -> "ROLE_RA_OFFICER"))
            ))
            .andExpect(status().isForbidden());
    }

    /**
     * Test role-based access control - Admin with Admin role
     */
    @Test
    public void testAdminEndpointWithAdminRole() throws Exception {
        mockMvc.perform(get("/admin/dashboard")
            .with(saml2Login()
                .authorities(authorities -> authorities.add(() -> "ROLE_RA_ADMIN"))
                .attributes(attrs -> {
                    attrs.put("email", "admin@example.com");
                    attrs.put("displayName", "Admin User");
                    attrs.put("role", "RA_ADMIN");
                })
            ))
            .andExpect(status().isOk());
    }

    /**
     * Test API endpoint for user info
     */
    @Test
    public void testUserInfoApiEndpoint() throws Exception {
        mockMvc.perform(get("/saml/api/user-info")
            .with(saml2Login()
                .attributes(attrs -> {
                    attrs.put("email", "api@example.com");
                    attrs.put("firstName", "API");
                    attrs.put("lastName", "User");
                    attrs.put("displayName", "API User");
                    attrs.put("department", "Development");
                    attrs.put("role", "RA_OPERATOR");
                })
            ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("api@example.com"))
            .andExpect(jsonPath("$.firstName").value("API"))
            .andExpect(jsonPath("$.primaryRole").value("RA_OPERATOR"));
    }

    /**
     * Test SAML User model primary role determination
     */
    @Test
    public void testSAMLUserPrimaryRole() {
        SAMLUser adminUser = new SAMLUser();
        adminUser.setRoles(java.util.Arrays.asList("RA_ADMIN", "RA_OFFICER"));
        assertEquals("RA_ADMIN", adminUser.getPrimaryRole());

        SAMLUser officerUser = new SAMLUser();
        officerUser.setRoles(java.util.Arrays.asList("RA_OFFICER", "RA_OPERATOR"));
        assertEquals("RA_OFFICER", officerUser.getPrimaryRole());

        SAMLUser operatorUser = new SAMLUser();
        operatorUser.setRoles(java.util.Arrays.asList("RA_OPERATOR"));
        assertEquals("RA_OPERATOR", operatorUser.getPrimaryRole());

        SAMLUser endEntityUser = new SAMLUser();
        endEntityUser.setRoles(java.util.Arrays.asList());
        assertEquals("END_ENTITY", endEntityUser.getPrimaryRole());
    }

    /**
     * Test SAML User has role check
     */
    @Test
    public void testSAMLUserHasRole() {
        SAMLUser user = new SAMLUser();
        user.setRoles(java.util.Arrays.asList("RA_OFFICER", "RA_OPERATOR"));

        assertTrue(user.hasRole("RA_OFFICER"));
        assertTrue(user.hasRole("RA_OPERATOR"));
        assertFalse(user.hasRole("RA_ADMIN"));
        assertFalse(user.hasRole("AUDITOR"));
    }

    /**
     * Test authentication status endpoint
     */
    @Test
    public void testAuthenticationStatusEndpoint() throws Exception {
        mockMvc.perform(get("/saml/status")
            .with(saml2Login()
                .attributes(attrs -> {
                    attrs.put("email", "status@example.com");
                    attrs.put("role", "RA_OFFICER");
                })
            ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.authenticated").value(true))
            .andExpect(jsonPath("$.primaryRole").exists());
    }

    /**
     * Test public home page access
     */
    @Test
    public void testPublicHomePageAccess() throws Exception {
        mockMvc.perform(get("/home"))
            .andExpect(status().isOk());
    }

    /**
     * Test login page access
     */
    @Test
    public void testLoginPageAccess() throws Exception {
        mockMvc.perform(get("/login"))
            .andExpect(status().isOk());
    }
}
