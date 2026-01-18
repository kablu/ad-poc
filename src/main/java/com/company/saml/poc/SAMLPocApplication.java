package com.company.saml.poc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * SAML POC Application - Demonstrates SAML 2.0 Authentication
 *
 * This application demonstrates:
 * - SAML Service Provider (SP) implementation
 * - Integration with Identity Provider (IdP)
 * - User authentication via SAML assertions
 * - Role-based access control from SAML attributes
 * - Single Sign-On (SSO) functionality
 * - Single Logout (SLO) support
 *
 * @author SAML POC Team
 * @version 1.0
 */
@SpringBootApplication
public class SAMLPocApplication {

    public static void main(String[] args) {
        SpringApplication.run(SAMLPocApplication.class, args);
        System.out.println("==============================================");
        System.out.println("SAML POC Application Started Successfully!");
        System.out.println("==============================================");
        System.out.println("Access the application at: https://localhost:8443");
        System.out.println("SAML Metadata: https://localhost:8443/saml/metadata");
        System.out.println("==============================================");
    }
}
