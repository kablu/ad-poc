package com.company.saml.poc.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.provider.service.authentication.OpenSaml5AuthenticationProvider;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication;
import org.springframework.security.saml2.provider.service.registration.InMemoryRelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrations;
import org.springframework.security.web.SecurityFilterChain;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 * SAML Security Configuration
 *
 * Configures Spring Security for SAML 2.0 authentication including:
 * - Service Provider (SP) registration
 * - Identity Provider (IdP) integration
 * - Security filter chain with SAML endpoints
 * - Certificate management for signing and encryption
 *
 * @author SAML POC Team
 */
@Configuration
@EnableWebSecurity
public class SAMLSecurityConfig {

    @Value("${saml.idp.metadata-url:https://idp.example.com/metadata}")
    private String idpMetadataUrl;

    @Value("${saml.sp.entity-id:https://localhost:8443/saml/metadata}")
    private String spEntityId;

    @Value("${saml.sp.acs-url:https://localhost:8443/login/saml2/sso/saml-poc}")
    private String acsUrl;

    @Value("${saml.keystore.location:classpath:saml-keystore.jks}")
    private String keystoreLocation;

    @Value("${saml.keystore.password:changeit}")
    private String keystorePassword;

    @Value("${saml.keystore.alias:saml-signing}")
    private String keystoreAlias;

    @Value("${saml.keystore.key-password:changeit}")
    private String keyPassword;

    /**
     * Configure HTTP Security with SAML authentication
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(
                    "/",
                    "/home",
                    "/login",
                    "/error",
                    "/css/**",
                    "/js/**",
                    "/images/**"
                ).permitAll()
                .requestMatchers("/admin/**").hasRole("RA_ADMIN")
                .requestMatchers("/officer/**").hasAnyRole("RA_ADMIN", "RA_OFFICER")
                .requestMatchers("/operator/**").hasAnyRole("RA_ADMIN", "RA_OFFICER", "RA_OPERATOR")
                .anyRequest().authenticated()
            )
            .saml2Login(saml2 -> saml2
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl("/login?error=true")
            )
            .saml2Logout(logout -> logout
                .logoutUrl("/logout")
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout=true")
            )
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/saml/**")
            );

        return http.build();
    }

    /**
     * Configure Relying Party (Service Provider) Registration
     *
     * This registers the SP with the IdP configuration including:
     * - IdP metadata location
     * - SP entity ID and endpoints
     * - Signing and encryption credentials
     */
    @Bean
    public RelyingPartyRegistrationRepository relyingPartyRegistrationRepository() {
        RelyingPartyRegistration registration = RelyingPartyRegistrations
            .fromMetadataLocation(idpMetadataUrl)
            .registrationId("saml-poc")
            .entityId(spEntityId)
            .assertionConsumerServiceLocation(acsUrl)
            .singleLogoutServiceLocation("{baseUrl}/logout/saml2/slo")
            .signingX509Credentials(credentials -> credentials.add(signingCredential()))
            .decryptionX509Credentials(credentials -> credentials.add(decryptionCredential()))
            .build();

        return new InMemoryRelyingPartyRegistrationRepository(registration);
    }

    /**
     * Load signing credential from keystore
     */
    private Saml2X509Credential signingCredential() {
        try {
            KeyStore keyStore = KeyStore.getInstance("JKS");
            InputStream inputStream = new ClassPathResource("saml-keystore.jks").getInputStream();
            keyStore.load(inputStream, keystorePassword.toCharArray());

            PrivateKey privateKey = (PrivateKey) keyStore.getKey(
                keystoreAlias,
                keyPassword.toCharArray()
            );
            X509Certificate certificate = (X509Certificate) keyStore.getCertificate(keystoreAlias);

            return Saml2X509Credential.signing(privateKey, certificate);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load SAML signing credential", e);
        }
    }

    /**
     * Load decryption credential from keystore
     */
    private Saml2X509Credential decryptionCredential() {
        try {
            KeyStore keyStore = KeyStore.getInstance("JKS");
            InputStream inputStream = new ClassPathResource("saml-keystore.jks").getInputStream();
            keyStore.load(inputStream, keystorePassword.toCharArray());

            PrivateKey privateKey = (PrivateKey) keyStore.getKey(
                keystoreAlias,
                keyPassword.toCharArray()
            );
            X509Certificate certificate = (X509Certificate) keyStore.getCertificate(keystoreAlias);

            return Saml2X509Credential.decryption(privateKey, certificate);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load SAML decryption credential", e);
        }
    }

    /**
     * Configure SAML Authentication Provider with custom assertion validation
     *
     * Note: In Spring Security 7.x with OpenSAML 5, custom assertion validation
     * should be done through ResponseAuthenticationConverter instead of
     * setAssertionValidator which has a different signature.
     */
    @Bean
    public OpenSaml5AuthenticationProvider authenticationProvider() {
        OpenSaml5AuthenticationProvider authenticationProvider = new OpenSaml5AuthenticationProvider();

        // Custom assertion validation can be added via ResponseAuthenticationConverter
        // For now, using default configuration
        // To add custom validation, use:
        // authenticationProvider.setResponseAuthenticationConverter(customConverter);

        return authenticationProvider;
    }
}
