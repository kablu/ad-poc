package com.company.saml.poc.util;

import org.springframework.stereotype.Component;

import java.io.StringWriter;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Base64;

/**
 * SAML Metadata Generator
 *
 * Generates Service Provider (SP) metadata XML for SAML configuration.
 * This metadata is shared with the Identity Provider (IdP) to establish trust.
 *
 * @author SAML POC Team
 */
@Component
public class SAMLMetadataGenerator {

    private static final String SP_ENTITY_ID = "https://localhost:8443/saml/metadata";
    private static final String ACS_URL = "https://localhost:8443/login/saml2/sso/saml-poc";
    private static final String SLO_URL = "https://localhost:8443/logout/saml2/slo";

    /**
     * Generate SP metadata XML
     *
     * @param certificate X.509 certificate for signing/encryption
     * @return XML metadata as String
     * @throws CertificateEncodingException if certificate encoding fails
     */
    public String generateMetadata(X509Certificate certificate) throws CertificateEncodingException {
        StringWriter writer = new StringWriter();

        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        writer.write("<EntityDescriptor xmlns=\"urn:oasis:names:tc:SAML:2.0:metadata\"\n");
        writer.write("                  entityID=\"" + SP_ENTITY_ID + "\">\n");
        writer.write("\n");

        // SPSSODescriptor
        writer.write("    <SPSSODescriptor\n");
        writer.write("        AuthnRequestsSigned=\"true\"\n");
        writer.write("        WantAssertionsSigned=\"true\"\n");
        writer.write("        protocolSupportEnumeration=\"urn:oasis:names:tc:SAML:2.0:protocol\">\n");
        writer.write("\n");

        // Signing KeyDescriptor
        writer.write("        <KeyDescriptor use=\"signing\">\n");
        writer.write("            <KeyInfo xmlns=\"http://www.w3.org/2000/09/xmldsig#\">\n");
        writer.write("                <X509Data>\n");
        writer.write("                    <X509Certificate>\n");
        writer.write(encodeCertificate(certificate));
        writer.write("                    </X509Certificate>\n");
        writer.write("                </X509Data>\n");
        writer.write("            </KeyInfo>\n");
        writer.write("        </KeyDescriptor>\n");
        writer.write("\n");

        // Encryption KeyDescriptor
        writer.write("        <KeyDescriptor use=\"encryption\">\n");
        writer.write("            <KeyInfo xmlns=\"http://www.w3.org/2000/09/xmldsig#\">\n");
        writer.write("                <X509Data>\n");
        writer.write("                    <X509Certificate>\n");
        writer.write(encodeCertificate(certificate));
        writer.write("                    </X509Certificate>\n");
        writer.write("                </X509Data>\n");
        writer.write("            </KeyInfo>\n");
        writer.write("        </KeyDescriptor>\n");
        writer.write("\n");

        // SingleLogoutService
        writer.write("        <SingleLogoutService\n");
        writer.write("            Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST\"\n");
        writer.write("            Location=\"" + SLO_URL + "\"/>\n");
        writer.write("\n");

        // NameIDFormat
        writer.write("        <NameIDFormat>urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress</NameIDFormat>\n");
        writer.write("        <NameIDFormat>urn:oasis:names:tc:SAML:2.0:nameid-format:persistent</NameIDFormat>\n");
        writer.write("\n");

        // AssertionConsumerService
        writer.write("        <AssertionConsumerService\n");
        writer.write("            Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST\"\n");
        writer.write("            Location=\"" + ACS_URL + "\"\n");
        writer.write("            index=\"0\"\n");
        writer.write("            isDefault=\"true\"/>\n");
        writer.write("\n");

        writer.write("    </SPSSODescriptor>\n");
        writer.write("</EntityDescriptor>\n");

        return writer.toString();
    }

    /**
     * Encode X.509 certificate to Base64
     */
    private String encodeCertificate(X509Certificate certificate) throws CertificateEncodingException {
        byte[] encoded = certificate.getEncoded();
        String base64 = Base64.getEncoder().encodeToString(encoded);

        // Format certificate with line breaks every 64 characters
        StringBuilder formatted = new StringBuilder();
        int index = 0;
        while (index < base64.length()) {
            formatted.append("                        ");
            int endIndex = Math.min(index + 64, base64.length());
            formatted.append(base64, index, endIndex);
            formatted.append("\n");
            index = endIndex;
        }

        return formatted.toString();
    }

    /**
     * Generate simplified metadata without certificate
     */
    public String generateSimpleMetadata() {
        StringWriter writer = new StringWriter();

        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        writer.write("<EntityDescriptor xmlns=\"urn:oasis:names:tc:SAML:2.0:metadata\"\n");
        writer.write("                  entityID=\"" + SP_ENTITY_ID + "\">\n");
        writer.write("    <SPSSODescriptor\n");
        writer.write("        AuthnRequestsSigned=\"false\"\n");
        writer.write("        WantAssertionsSigned=\"true\"\n");
        writer.write("        protocolSupportEnumeration=\"urn:oasis:names:tc:SAML:2.0:protocol\">\n");
        writer.write("        <NameIDFormat>urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress</NameIDFormat>\n");
        writer.write("        <AssertionConsumerService\n");
        writer.write("            Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST\"\n");
        writer.write("            Location=\"" + ACS_URL + "\"\n");
        writer.write("            index=\"0\" isDefault=\"true\"/>\n");
        writer.write("    </SPSSODescriptor>\n");
        writer.write("</EntityDescriptor>\n");

        return writer.toString();
    }
}
