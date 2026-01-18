# SAML Implementation in Java - Complete Examples

## Table of Contents
1. [Maven Dependencies](#maven-dependencies)
2. [Spring Boot SAML Service Provider Example](#spring-boot-saml-service-provider-example)
3. [SAML Configuration](#saml-configuration)
4. [SAML Response Validation](#saml-response-validation)
5. [SAML Metadata Generation](#saml-metadata-generation)
6. [Complete Working Example](#complete-working-example)

---

## Maven Dependencies

```xml
<!-- pom.xml -->
<project>
    <properties>
        <spring.boot.version>3.2.1</spring.boot.version>
        <spring.security.saml.version>2.0.0.M31</spring.security.saml.version>
    </properties>

    <dependencies>
        <!-- Spring Boot Starter -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Spring Security -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>

        <!-- Spring Security SAML 2.0 Extension -->
        <dependency>
            <groupId>org.springframework.security.extensions</groupId>
            <artifactId>spring-security-saml2-core</artifactId>
            <version>1.0.10.RELEASE</version>
        </dependency>

        <!-- OpenSAML (XML processing) -->
        <dependency>
            <groupId>org.opensaml</groupId>
            <artifactId>opensaml-core</artifactId>
            <version>4.3.0</version>
        </dependency>

        <dependency>
            <groupId>org.opensaml</groupId>
            <artifactId>opensaml-saml-api</artifactId>
            <version>4.3.0</version>
        </dependency>

        <dependency>
            <groupId>org.opensaml</groupId>
            <artifactId>opensaml-saml-impl</artifactId>
            <version>4.3.0</version>
        </dependency>

        <!-- XML Security -->
        <dependency>
            <groupId>org.apache.santuario</groupId>
            <artifactId>xmlsec</artifactId>
            <version>3.0.3</version>
        </dependency>

        <!-- Thymeleaf (for UI) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-thymeleaf</artifactId>
        </dependency>
    </dependencies>
</project>
```

---

## Spring Boot SAML Service Provider Example

### 1. SAML Configuration Class

```java
package com.example.ra.saml.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.provider.service.authentication.OpenSaml4AuthenticationProvider;
import org.springframework.security.saml2.provider.service.registration.InMemoryRelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrations;
import org.springframework.security.web.SecurityFilterChain;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

@Configuration
@EnableWebSecurity
public class SAMLSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/saml/**", "/login", "/error").permitAll()
                .anyRequest().authenticated()
            )
            .saml2Login(saml2 -> saml2
                .loginPage("/saml/login")
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl("/login?error")
            )
            .saml2Logout(logout -> logout
                .logoutUrl("/saml/logout")
                .logoutSuccessUrl("/login?logout")
            );

        return http.build();
    }

    @Bean
    public RelyingPartyRegistrationRepository relyingPartyRegistrationRepository() {
        // Option 1: Load from IdP metadata URL
        RelyingPartyRegistration registration = RelyingPartyRegistrations
            .fromMetadataLocation("https://idp.example.com/metadata")
            .registrationId("company-idp")
            .entityId("https://ra.company.com/saml/metadata") // SP Entity ID
            .assertionConsumerServiceLocation("https://ra.company.com/saml/acs") // ACS URL
            .singleLogoutServiceLocation("https://ra.company.com/saml/logout")
            .signingX509Credentials(credentials -> credentials.add(
                signingCredential()
            ))
            .decryptionX509Credentials(credentials -> credentials.add(
                decryptionCredential()
            ))
            .build();

        return new InMemoryRelyingPartyRegistrationRepository(registration);
    }

    private Saml2X509Credential signingCredential() {
        try {
            KeyStore keyStore = KeyStore.getInstance("JKS");
            InputStream inputStream = new ClassPathResource("saml-keystore.jks").getInputStream();
            keyStore.load(inputStream, "keystorePassword".toCharArray());

            PrivateKey privateKey = (PrivateKey) keyStore.getKey("saml-signing",
                "keyPassword".toCharArray());
            X509Certificate certificate = (X509Certificate) keyStore.getCertificate("saml-signing");

            return Saml2X509Credential.signing(privateKey, certificate);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load signing credential", e);
        }
    }

    private Saml2X509Credential decryptionCredential() {
        try {
            KeyStore keyStore = KeyStore.getInstance("JKS");
            InputStream inputStream = new ClassPathResource("saml-keystore.jks").getInputStream();
            keyStore.load(inputStream, "keystorePassword".toCharArray());

            PrivateKey privateKey = (PrivateKey) keyStore.getKey("saml-encryption",
                "keyPassword".toCharArray());
            X509Certificate certificate = (X509Certificate) keyStore.getCertificate("saml-encryption");

            return Saml2X509Credential.decryption(privateKey, certificate);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load decryption credential", e);
        }
    }

    @Bean
    public OpenSaml4AuthenticationProvider authenticationProvider() {
        OpenSaml4AuthenticationProvider authenticationProvider = new OpenSaml4AuthenticationProvider();

        // Custom assertion validation
        authenticationProvider.setAssertionValidator(assertionToken -> {
            // Custom validation logic
            return assertionToken;
        });

        return authenticationProvider;
    }
}
```

### 2. SAML Controller

```java
package com.example.ra.saml.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@Controller
@RequestMapping("/saml")
public class SAMLController {

    @GetMapping("/login")
    public String login() {
        return "saml-login";
    }

    @GetMapping("/metadata")
    public String metadata(Model model) {
        // Spring Security automatically generates metadata at /saml2/service-provider-metadata/{registrationId}
        return "redirect:/saml2/service-provider-metadata/company-idp";
    }

    @GetMapping("/user-info")
    public String userInfo(@AuthenticationPrincipal Saml2AuthenticatedPrincipal principal,
                          Model model) {
        if (principal != null) {
            model.addAttribute("username", principal.getName());
            model.addAttribute("attributes", principal.getAttributes());
            model.addAttribute("sessionIndexes", principal.getSessionIndexes());
            model.addAttribute("registrationId", principal.getRelyingPartyRegistrationId());

            // Extract specific attributes from SAML assertion
            String email = principal.getFirstAttribute("email");
            String firstName = principal.getFirstAttribute("firstName");
            String lastName = principal.getFirstAttribute("lastName");
            String department = principal.getFirstAttribute("department");

            model.addAttribute("email", email);
            model.addAttribute("firstName", firstName);
            model.addAttribute("lastName", lastName);
            model.addAttribute("department", department);
        }
        return "user-info";
    }
}
```

### 3. Dashboard Controller

```java
package com.example.ra.saml.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal Saml2AuthenticatedPrincipal principal,
                           Model model) {
        // Extract user information from SAML assertion
        String username = principal.getName();
        String email = principal.getFirstAttribute("email");
        String displayName = principal.getFirstAttribute("displayName");
        String department = principal.getFirstAttribute("department");

        // Extract role from SAML assertion
        String role = principal.getFirstAttribute("role");

        model.addAttribute("username", username);
        model.addAttribute("email", email);
        model.addAttribute("displayName", displayName);
        model.addAttribute("department", department);
        model.addAttribute("role", role);

        // Redirect based on role
        if ("RA_ADMIN".equals(role)) {
            return "admin-dashboard";
        } else if ("RA_OFFICER".equals(role)) {
            return "officer-dashboard";
        } else {
            return "user-dashboard";
        }
    }
}
```

---

## SAML Response Validation

### Custom SAML Assertion Validator

```java
package com.example.ra.saml.validator;

import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Conditions;
import org.springframework.security.saml2.core.Saml2Error;
import org.springframework.security.saml2.core.Saml2ErrorCodes;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class SAMLAssertionValidator {

    private static final String EXPECTED_AUDIENCE = "https://ra.company.com";

    /**
     * Validate SAML Assertion for security and integrity
     */
    public void validateAssertion(Assertion assertion) {
        // 1. Validate Assertion ID (must be unique)
        validateAssertionId(assertion);

        // 2. Validate Issuer
        validateIssuer(assertion);

        // 3. Validate Conditions (time validity)
        validateConditions(assertion);

        // 4. Validate Audience Restriction
        validateAudience(assertion);

        // 5. Validate Subject Confirmation
        validateSubjectConfirmation(assertion);

        // 6. Validate Signature (handled by Spring Security SAML)
        // Digital signature verification is automatic

        // 7. Custom business logic validation
        validateCustomRules(assertion);
    }

    private void validateAssertionId(Assertion assertion) {
        String assertionId = assertion.getID();
        if (assertionId == null || assertionId.isEmpty()) {
            throw new Saml2AuthenticationException(
                new Saml2Error(Saml2ErrorCodes.INVALID_ASSERTION,
                    "Assertion ID is missing"));
        }

        // Check for assertion replay (should be stored and checked against cache)
        if (isAssertionReplayed(assertionId)) {
            throw new Saml2AuthenticationException(
                new Saml2Error(Saml2ErrorCodes.INVALID_ASSERTION,
                    "Assertion has already been used (replay attack)"));
        }
    }

    private void validateIssuer(Assertion assertion) {
        String issuer = assertion.getIssuer().getValue();
        String expectedIssuer = "https://idp.example.com";

        if (!expectedIssuer.equals(issuer)) {
            throw new Saml2AuthenticationException(
                new Saml2Error(Saml2ErrorCodes.INVALID_ISSUER,
                    "Assertion issuer does not match expected IdP"));
        }
    }

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
                    "Assertion is not yet valid (notBefore constraint violated)"));
        }

        // Check if assertion has expired
        if (notOnOrAfter != null && now.isAfter(notOnOrAfter)) {
            throw new Saml2AuthenticationException(
                new Saml2Error(Saml2ErrorCodes.INVALID_ASSERTION,
                    "Assertion has expired (notOnOrAfter constraint violated)"));
        }
    }

    private void validateAudience(Assertion assertion) {
        Conditions conditions = assertion.getConditions();
        if (conditions.getAudienceRestrictions().isEmpty()) {
            throw new Saml2AuthenticationException(
                new Saml2Error(Saml2ErrorCodes.INVALID_ASSERTION,
                    "Assertion does not contain audience restriction"));
        }

        boolean audienceMatch = conditions.getAudienceRestrictions().stream()
            .flatMap(restriction -> restriction.getAudiences().stream())
            .anyMatch(audience -> EXPECTED_AUDIENCE.equals(audience.getURI()));

        if (!audienceMatch) {
            throw new Saml2AuthenticationException(
                new Saml2Error(Saml2ErrorCodes.INVALID_ASSERTION,
                    "Assertion audience does not match this service provider"));
        }
    }

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
                    "Assertion must use bearer subject confirmation"));
        }
    }

    private void validateCustomRules(Assertion assertion) {
        // Example: Validate that user has required attributes
        List<String> requiredAttributes = List.of("email", "firstName", "lastName", "department");

        // Custom validation logic based on business requirements
        // For example: Check if user is from allowed department, role, etc.
    }

    private boolean isAssertionReplayed(String assertionId) {
        // In production, check against a cache (Redis, in-memory) of used assertion IDs
        // Assertions should be stored with TTL matching their validity period
        return false; // Placeholder
    }
}
```

---

## SAML Metadata Generation

### Service Provider Metadata Generator

```java
package com.example.ra.saml.metadata;

import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.Marshaller;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.saml.saml2.metadata.*;
import org.opensaml.xmlsec.signature.KeyInfo;
import org.opensaml.xmlsec.signature.X509Certificate;
import org.opensaml.xmlsec.signature.X509Data;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.security.cert.CertificateEncodingException;
import java.util.Base64;

@Component
public class SPMetadataGenerator {

    private static final String SP_ENTITY_ID = "https://ra.company.com";
    private static final String ACS_URL = "https://ra.company.com/saml/acs";
    private static final String SLO_URL = "https://ra.company.com/saml/logout";

    public String generateMetadata(java.security.cert.X509Certificate certificate)
            throws MarshallingException, CertificateEncodingException {

        EntityDescriptor entityDescriptor = buildEntityDescriptor();
        SPSSODescriptor spDescriptor = buildSPSSODescriptor(certificate);

        entityDescriptor.getRoleDescriptors().add(spDescriptor);

        return marshallMetadata(entityDescriptor);
    }

    private EntityDescriptor buildEntityDescriptor() {
        EntityDescriptor descriptor = (EntityDescriptor) XMLObjectProviderRegistrySupport
            .getBuilderFactory()
            .getBuilder(EntityDescriptor.DEFAULT_ELEMENT_NAME)
            .buildObject(EntityDescriptor.DEFAULT_ELEMENT_NAME);

        descriptor.setEntityID(SP_ENTITY_ID);
        return descriptor;
    }

    private SPSSODescriptor buildSPSSODescriptor(java.security.cert.X509Certificate certificate)
            throws CertificateEncodingException {

        SPSSODescriptor spDescriptor = (SPSSODescriptor) XMLObjectProviderRegistrySupport
            .getBuilderFactory()
            .getBuilder(SPSSODescriptor.DEFAULT_ELEMENT_NAME)
            .buildObject(SPSSODescriptor.DEFAULT_ELEMENT_NAME);

        spDescriptor.setAuthnRequestsSigned(true);
        spDescriptor.setWantAssertionsSigned(true);
        spDescriptor.addSupportedProtocol("urn:oasis:names:tc:SAML:2.0:protocol");

        // Add KeyDescriptor for signing
        spDescriptor.getKeyDescriptors().add(buildKeyDescriptor(certificate, "signing"));

        // Add KeyDescriptor for encryption
        spDescriptor.getKeyDescriptors().add(buildKeyDescriptor(certificate, "encryption"));

        // Add Assertion Consumer Service
        spDescriptor.getAssertionConsumerServices().add(buildAssertionConsumerService());

        // Add Single Logout Service
        spDescriptor.getSingleLogoutServices().add(buildSingleLogoutService());

        // Add NameID formats
        spDescriptor.getNameIDFormats().add(buildNameIDFormat("email"));
        spDescriptor.getNameIDFormats().add(buildNameIDFormat("persistent"));

        return spDescriptor;
    }

    private KeyDescriptor buildKeyDescriptor(java.security.cert.X509Certificate certificate,
                                            String use) throws CertificateEncodingException {

        KeyDescriptor keyDescriptor = (KeyDescriptor) XMLObjectProviderRegistrySupport
            .getBuilderFactory()
            .getBuilder(KeyDescriptor.DEFAULT_ELEMENT_NAME)
            .buildObject(KeyDescriptor.DEFAULT_ELEMENT_NAME);

        keyDescriptor.setUse(UsageType.valueOf(use.toUpperCase()));

        KeyInfo keyInfo = (KeyInfo) XMLObjectProviderRegistrySupport
            .getBuilderFactory()
            .getBuilder(KeyInfo.DEFAULT_ELEMENT_NAME)
            .buildObject(KeyInfo.DEFAULT_ELEMENT_NAME);

        X509Data x509Data = (X509Data) XMLObjectProviderRegistrySupport
            .getBuilderFactory()
            .getBuilder(X509Data.DEFAULT_ELEMENT_NAME)
            .buildObject(X509Data.DEFAULT_ELEMENT_NAME);

        X509Certificate x509Certificate = (X509Certificate) XMLObjectProviderRegistrySupport
            .getBuilderFactory()
            .getBuilder(X509Certificate.DEFAULT_ELEMENT_NAME)
            .buildObject(X509Certificate.DEFAULT_ELEMENT_NAME);

        String encodedCert = Base64.getEncoder().encodeToString(certificate.getEncoded());
        x509Certificate.setValue(encodedCert);

        x509Data.getX509Certificates().add(x509Certificate);
        keyInfo.getX509Datas().add(x509Data);
        keyDescriptor.setKeyInfo(keyInfo);

        return keyDescriptor;
    }

    private AssertionConsumerService buildAssertionConsumerService() {
        AssertionConsumerService acs = (AssertionConsumerService) XMLObjectProviderRegistrySupport
            .getBuilderFactory()
            .getBuilder(AssertionConsumerService.DEFAULT_ELEMENT_NAME)
            .buildObject(AssertionConsumerService.DEFAULT_ELEMENT_NAME);

        acs.setLocation(ACS_URL);
        acs.setBinding("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST");
        acs.setIndex(0);
        acs.setIsDefault(true);

        return acs;
    }

    private SingleLogoutService buildSingleLogoutService() {
        SingleLogoutService slo = (SingleLogoutService) XMLObjectProviderRegistrySupport
            .getBuilderFactory()
            .getBuilder(SingleLogoutService.DEFAULT_ELEMENT_NAME)
            .buildObject(SingleLogoutService.DEFAULT_ELEMENT_NAME);

        slo.setLocation(SLO_URL);
        slo.setBinding("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST");

        return slo;
    }

    private NameIDFormat buildNameIDFormat(String format) {
        NameIDFormat nameIDFormat = (NameIDFormat) XMLObjectProviderRegistrySupport
            .getBuilderFactory()
            .getBuilder(NameIDFormat.DEFAULT_ELEMENT_NAME)
            .buildObject(NameIDFormat.DEFAULT_ELEMENT_NAME);

        if ("email".equals(format)) {
            nameIDFormat.setURI("urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress");
        } else if ("persistent".equals(format)) {
            nameIDFormat.setURI("urn:oasis:names:tc:SAML:2.0:nameid-format:persistent");
        }

        return nameIDFormat;
    }

    private String marshallMetadata(EntityDescriptor entityDescriptor) throws MarshallingException {
        try {
            Marshaller marshaller = XMLObjectProviderRegistrySupport.getMarshallerFactory()
                .getMarshaller(entityDescriptor);

            Element element = marshaller.marshall(entityDescriptor);

            StringWriter writer = new StringWriter();
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.transform(new DOMSource(element), new StreamResult(writer));

            return writer.toString();
        } catch (Exception e) {
            throw new MarshallingException("Failed to marshall SP metadata", e);
        }
    }
}
```

---

## Complete Working Example

### Application.properties

```properties
# Application Configuration
spring.application.name=RA-Web-SAML
server.port=8443
server.ssl.enabled=true
server.ssl.key-store=classpath:saml-keystore.jks
server.ssl.key-store-password=keystorePassword
server.ssl.key-alias=saml-signing

# SAML Configuration
saml.sp.entity-id=https://ra.company.com
saml.sp.acs-url=https://ra.company.com/saml/acs
saml.sp.slo-url=https://ra.company.com/saml/logout

# Identity Provider Configuration
saml.idp.metadata-url=https://idp.example.com/metadata
saml.idp.entity-id=https://idp.example.com

# Security
spring.security.filter.dispatcher-types=REQUEST,ERROR,ASYNC,FORWARD
```

### Main Application

```java
package com.example.ra;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RAWebSAMLApplication {

    public static void main(String[] args) {
        SpringApplication.run(RAWebSAMLApplication.class, args);
    }
}
```

### User Model (Populated from SAML Assertion)

```java
package com.example.ra.saml.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SAMLUser {
    private String userId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String displayName;
    private String department;
    private List<String> roles;
    private String sessionIndex;

    public static SAMLUser fromSAMLAssertion(
            org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal principal) {

        SAMLUser user = new SAMLUser();
        user.setUsername(principal.getName());
        user.setEmail(principal.getFirstAttribute("email"));
        user.setFirstName(principal.getFirstAttribute("firstName"));
        user.setLastName(principal.getFirstAttribute("lastName"));
        user.setDisplayName(principal.getFirstAttribute("displayName"));
        user.setDepartment(principal.getFirstAttribute("department"));

        // Extract roles from SAML attribute
        List<Object> roleAttributes = principal.getAttribute("role");
        if (roleAttributes != null) {
            user.setRoles(roleAttributes.stream()
                .map(Object::toString)
                .toList());
        }

        if (!principal.getSessionIndexes().isEmpty()) {
            user.setSessionIndex(principal.getSessionIndexes().iterator().next());
        }

        return user;
    }
}
```

### Service for User Management

```java
package com.example.ra.saml.service;

import com.example.ra.saml.model.SAMLUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.stereotype.Service;

@Service
public class SAMLUserService {

    /**
     * Get currently authenticated SAML user
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
     * Check if user has specific role
     */
    public boolean hasRole(String role) {
        SAMLUser user = getCurrentUser();
        return user != null && user.getRoles().contains(role);
    }

    /**
     * Get user's primary role for RA system
     */
    public String getPrimaryRole() {
        SAMLUser user = getCurrentUser();
        if (user == null || user.getRoles().isEmpty()) {
            return "END_ENTITY";
        }

        // Priority order: ADMIN > OFFICER > OPERATOR > AUDITOR > END_ENTITY
        if (user.getRoles().contains("RA_ADMIN")) {
            return "RA_ADMIN";
        } else if (user.getRoles().contains("RA_OFFICER")) {
            return "RA_OFFICER";
        } else if (user.getRoles().contains("RA_OPERATOR")) {
            return "RA_OPERATOR";
        } else if (user.getRoles().contains("AUDITOR")) {
            return "AUDITOR";
        }

        return "END_ENTITY";
    }
}
```

### Testing the SAML Integration

```java
package com.example.ra.saml;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.saml2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class SAMLIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testAccessProtectedResourceWithSAML() throws Exception {
        mockMvc.perform(get("/dashboard")
            .with(saml2Login()
                .attributes(attrs -> {
                    attrs.put("email", "user@example.com");
                    attrs.put("firstName", "John");
                    attrs.put("lastName", "Doe");
                    attrs.put("role", "RA_OFFICER");
                })
            ))
            .andExpect(status().isOk());
    }
}
```

---

## Key Takeaways

1. **Spring Security SAML** simplifies SAML 2.0 integration in Java applications
2. **Certificate Management** is critical for signing and encryption
3. **Assertion Validation** must be thorough to prevent security vulnerabilities
4. **Metadata Exchange** between SP and IdP is essential for configuration
5. **Attribute Mapping** from SAML assertions to application roles is crucial
6. **Testing** should include both unit tests and integration tests with mock SAML responses

For production deployments, always:
- Use HTTPS/TLS
- Validate all SAML assertions rigorously
- Implement assertion replay detection
- Rotate certificates regularly
- Monitor SAML authentication failures
- Implement comprehensive audit logging
