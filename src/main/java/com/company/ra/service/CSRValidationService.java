package com.company.ra.service;

import com.company.ra.dto.ADUserAttributes;
import com.company.ra.dto.SubjectDN;
import com.company.ra.dto.ValidationResult;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.operator.ContentVerifierProvider;
import org.bouncycastle.operator.jcajce.JcaContentVerifierProviderBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringReader;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Service for validating PKCS#10 Certificate Signing Requests
 */
@Service
public class CSRValidationService {

    private static final Logger logger = LoggerFactory.getLogger(CSRValidationService.class);

    @Autowired
    private PublicKeyBlacklistService publicKeyBlacklistService;

    /**
     * Parse PKCS#10 CSR from PEM format
     *
     * @param csrPem PEM-encoded CSR
     * @return PKCS10CertificationRequest object
     * @throws Exception if parsing fails
     */
    public PKCS10CertificationRequest parsePKCS10(String csrPem) throws Exception {
        try (PEMParser pemParser = new PEMParser(new StringReader(csrPem))) {
            Object parsedObject = pemParser.readObject();

            if (parsedObject instanceof PKCS10CertificationRequest) {
                return (PKCS10CertificationRequest) parsedObject;
            } else {
                throw new IllegalArgumentException("Invalid PKCS#10 CSR format");
            }
        } catch (IOException e) {
            logger.error("Failed to parse PKCS#10 CSR", e);
            throw new Exception("Failed to parse PKCS#10 CSR: " + e.getMessage(), e);
        }
    }

    /**
     * Verify CSR signature (Proof of Possession)
     *
     * @param csr PKCS#10 CSR
     * @return true if signature is valid
     */
    public boolean verifySignature(PKCS10CertificationRequest csr) {
        try {
            ContentVerifierProvider verifierProvider = new JcaContentVerifierProviderBuilder()
                .build(csr.getSubjectPublicKeyInfo());

            boolean isValid = csr.isSignatureValid(verifierProvider);

            if (isValid) {
                logger.debug("CSR signature verification successful");
            } else {
                logger.warn("CSR signature verification failed");
            }

            return isValid;
        } catch (Exception e) {
            logger.error("Error verifying CSR signature", e);
            return false;
        }
    }

    /**
     * Extract Subject DN from CSR
     *
     * @param csr PKCS#10 CSR
     * @return SubjectDN object
     */
    public SubjectDN extractSubjectDN(PKCS10CertificationRequest csr) {
        X500Name x500Name = csr.getSubject();

        SubjectDN subjectDN = new SubjectDN();
        subjectDN.setRawDN(x500Name.toString());

        RDN[] rdns = x500Name.getRDNs(BCStyle.CN);
        if (rdns.length > 0) {
            subjectDN.setCommonName(IETFUtils.valueToString(rdns[0].getFirst().getValue()));
        }

        rdns = x500Name.getRDNs(BCStyle.EmailAddress);
        if (rdns.length > 0) {
            subjectDN.setEmail(IETFUtils.valueToString(rdns[0].getFirst().getValue()));
        }

        rdns = x500Name.getRDNs(BCStyle.OU);
        if (rdns.length > 0) {
            subjectDN.setOrganizationalUnit(IETFUtils.valueToString(rdns[0].getFirst().getValue()));
        }

        rdns = x500Name.getRDNs(BCStyle.O);
        if (rdns.length > 0) {
            subjectDN.setOrganization(IETFUtils.valueToString(rdns[0].getFirst().getValue()));
        }

        rdns = x500Name.getRDNs(BCStyle.C);
        if (rdns.length > 0) {
            subjectDN.setCountry(IETFUtils.valueToString(rdns[0].getFirst().getValue()));
        }

        logger.debug("Extracted Subject DN: {}", subjectDN);
        return subjectDN;
    }

    /**
     * Validate Subject DN against AD user attributes
     *
     * @param subjectDN Subject DN from CSR
     * @param adUserAttributes User attributes from Active Directory
     * @return ValidationResult with validation status and errors
     */
    public ValidationResult validateSubjectDN(SubjectDN subjectDN, ADUserAttributes adUserAttributes) {
        List<String> errors = new ArrayList<>();

        // Validate Common Name (CN)
        if (subjectDN.getCommonName() == null || subjectDN.getCommonName().trim().isEmpty()) {
            errors.add("Common Name (CN) is required");
        } else if (!subjectDN.getCommonName().equals(adUserAttributes.getCommonName())) {
            errors.add("Common Name (CN) does not match AD profile: expected '"
                + adUserAttributes.getCommonName() + "', got '" + subjectDN.getCommonName() + "'");
        }

        // Validate Email
        if (subjectDN.getEmail() != null && !subjectDN.getEmail().isEmpty()) {
            if (!subjectDN.getEmail().equals(adUserAttributes.getEmail())) {
                errors.add("Email does not match AD profile: expected '"
                    + adUserAttributes.getEmail() + "', got '" + subjectDN.getEmail() + "'");
            }
        }

        // Validate Organizational Unit (OU) if present
        if (subjectDN.getOrganizationalUnit() != null && !subjectDN.getOrganizationalUnit().isEmpty()) {
            if (!subjectDN.getOrganizationalUnit().equals(adUserAttributes.getOrganizationalUnit())) {
                errors.add("Organizational Unit (OU) does not match AD profile: expected '"
                    + adUserAttributes.getOrganizationalUnit() + "', got '"
                    + subjectDN.getOrganizationalUnit() + "'");
            }
        }

        // Validate Organization (O) if present
        if (subjectDN.getOrganization() != null && !subjectDN.getOrganization().isEmpty()) {
            if (!subjectDN.getOrganization().equals(adUserAttributes.getOrganization())) {
                errors.add("Organization (O) does not match AD profile: expected '"
                    + adUserAttributes.getOrganization() + "', got '" + subjectDN.getOrganization() + "'");
            }
        }

        ValidationResult result = new ValidationResult();
        result.setValid(errors.isEmpty());
        result.setErrors(errors);

        if (result.isValid()) {
            logger.info("Subject DN validation successful for user: {}", adUserAttributes.getUsername());
        } else {
            logger.warn("Subject DN validation failed for user: {}, errors: {}",
                adUserAttributes.getUsername(), errors);
        }

        return result;
    }

    /**
     * Validate key algorithm and size based on certificate type
     *
     * @param csr PKCS#10 CSR
     * @param certificateType Certificate type
     * @return ValidationResult with validation status and errors
     */
    public ValidationResult validateKeyParameters(PKCS10CertificationRequest csr, String certificateType) {
        List<String> errors = new ArrayList<>();

        try {
            PublicKey publicKey = new JcaPEMKeyConverter().getPublicKey(csr.getSubjectPublicKeyInfo());
            String algorithm = publicKey.getAlgorithm();

            logger.debug("Public key algorithm: {}", algorithm);

            // Validate algorithm
            if ("RSA".equals(algorithm)) {
                int keySize = getKeySize(publicKey);
                logger.debug("RSA key size: {} bits", keySize);

                // Minimum key size requirements by certificate type
                int minKeySize = getMinimumKeySize(certificateType);

                if (keySize < minKeySize) {
                    errors.add("RSA key size too small: minimum " + minKeySize
                        + " bits required for certificate type '" + certificateType
                        + "', got " + keySize + " bits");
                }
            } else if ("EC".equals(algorithm) || "ECDSA".equals(algorithm)) {
                // ECC key validation
                int keySize = getKeySize(publicKey);
                logger.debug("EC key size: {} bits", keySize);

                if (keySize < 256) {
                    errors.add("EC key size too small: minimum 256 bits required, got " + keySize + " bits");
                }
            } else {
                errors.add("Unsupported key algorithm: " + algorithm + " (only RSA and EC are supported)");
            }

        } catch (Exception e) {
            logger.error("Error validating key parameters", e);
            errors.add("Failed to validate key parameters: " + e.getMessage());
        }

        ValidationResult result = new ValidationResult();
        result.setValid(errors.isEmpty());
        result.setErrors(errors);

        return result;
    }

    /**
     * Check if public key is blacklisted (already used or compromised)
     *
     * @param csr PKCS#10 CSR
     * @return true if public key is blacklisted
     */
    public boolean isPublicKeyBlacklisted(PKCS10CertificationRequest csr) {
        try {
            PublicKey publicKey = new JcaPEMKeyConverter().getPublicKey(csr.getSubjectPublicKeyInfo());
            String publicKeyHash = calculatePublicKeyHash(publicKey);

            boolean isBlacklisted = publicKeyBlacklistService.isBlacklisted(publicKeyHash);

            if (isBlacklisted) {
                logger.warn("Public key is blacklisted: {}", publicKeyHash);
            }

            return isBlacklisted;
        } catch (Exception e) {
            logger.error("Error checking public key blacklist", e);
            return true; // Fail secure
        }
    }

    /**
     * Calculate SHA-256 hash of public key
     *
     * @param publicKey Public key
     * @return Base64-encoded hash
     */
    public String calculatePublicKeyHash(PublicKey publicKey) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(publicKey.getEncoded());
        return Base64.getEncoder().encodeToString(hash);
    }

    /**
     * Get minimum key size for certificate type
     *
     * @param certificateType Certificate type
     * @return Minimum key size in bits
     */
    private int getMinimumKeySize(String certificateType) {
        switch (certificateType.toUpperCase()) {
            case "USER_AUTHENTICATION":
            case "EMAIL_SIGNING":
                return 2048;
            case "CODE_SIGNING":
            case "SERVER_AUTHENTICATION":
                return 3072;
            case "DOCUMENT_SIGNING":
                return 2048;
            default:
                return 2048;
        }
    }

    /**
     * Get key size from public key
     *
     * @param publicKey Public key
     * @return Key size in bits
     */
    private int getKeySize(PublicKey publicKey) {
        String algorithm = publicKey.getAlgorithm();

        if ("RSA".equals(algorithm)) {
            try {
                java.security.interfaces.RSAPublicKey rsaKey =
                    (java.security.interfaces.RSAPublicKey) publicKey;
                return rsaKey.getModulus().bitLength();
            } catch (ClassCastException e) {
                logger.error("Failed to cast to RSAPublicKey", e);
                return 0;
            }
        } else if ("EC".equals(algorithm) || "ECDSA".equals(algorithm)) {
            try {
                java.security.interfaces.ECPublicKey ecKey =
                    (java.security.interfaces.ECPublicKey) publicKey;
                return ecKey.getParams().getOrder().bitLength();
            } catch (ClassCastException e) {
                logger.error("Failed to cast to ECPublicKey", e);
                return 0;
            }
        }

        return 0;
    }
}
