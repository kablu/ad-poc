package com.corp.ra.keystore;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.ContentVerifierProvider;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaContentVerifierProviderBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;

import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * KeyStore Manager for RA Web Application
 *
 * Provides comprehensive KeyStore management functionality including:
 * - KeyStore creation and initialization
 * - Private key and certificate storage using setKeyEntry()
 * - Certificate retrieval and management
 * - PKCS#10 CSR creation and validation
 * - X.509 certificate generation
 * - KeyStore import/export operations
 *
 * @author RA Development Team
 * @version 1.0
 * @since 2026-01-16
 */
public class KeyStoreManager {

    // Default settings
    private static final String KEYSTORE_TYPE = "PKCS12";
    private static final String PROVIDER = "BC";
    private static final String KEY_ALGORITHM = "RSA";
    private static final int KEY_SIZE = 2048;
    private static final String SIGNATURE_ALGORITHM = "SHA256WithRSA";

    static {
        // Register Bouncy Castle as security provider
        if (Security.getProvider(PROVIDER) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    /**
     * Supported KeyStore types for different use cases
     */
    public enum KeyStoreType {
        /**
         * PKCS#12 - Industry standard, cross-platform compatible
         * File extensions: .p12, .pfx
         * Use case: General certificate storage, user keystores
         * Provider: BC (Bouncy Castle)
         */
        PKCS12("PKCS12", "BC", ".p12"),

        /**
         * PKCS#11 - Hardware Security Module (HSM) interface
         * Use case: Hardware token, smart card, HSM integration
         * Provider: SunPKCS11
         * Note: Requires PKCS#11 configuration file
         */
        PKCS11("PKCS11", "SunPKCS11", null),

        /**
         * JKS - Java KeyStore (legacy)
         * File extension: .jks
         * Use case: Legacy Java applications
         * Provider: SUN
         */
        JKS("JKS", "SUN", ".jks"),

        /**
         * JCEKS - Java Cryptography Extension KeyStore
         * File extension: .jceks
         * Use case: Stronger key protection than JKS
         * Provider: SunJCE
         */
        JCEKS("JCEKS", "SunJCE", ".jceks"),

        /**
         * BKS - Bouncy Castle KeyStore
         * File extension: .bks
         * Use case: Android applications, BC-specific features
         * Provider: BC
         */
        BKS("BKS", "BC", ".bks");

        private final String type;
        private final String provider;
        private final String fileExtension;

        KeyStoreType(String type, String provider, String fileExtension) {
            this.type = type;
            this.provider = provider;
            this.fileExtension = fileExtension;
        }

        public String getType() { return type; }
        public String getProvider() { return provider; }
        public String getFileExtension() { return fileExtension; }
    }

    // ========================================================================
    // KeyStore Operations
    // ========================================================================

    /**
     * Create a new empty KeyStore using default type (PKCS12)
     *
     * @return Initialized empty KeyStore
     * @throws Exception if KeyStore cannot be created
     */
    public KeyStore createKeyStore() throws Exception {
        return createKeyStore(KeyStoreType.PKCS12);
    }

    /**
     * Create a new empty KeyStore with specified type
     *
     * @param type KeyStore type (PKCS12, PKCS11, JKS, JCEKS, BKS)
     * @return Initialized empty KeyStore
     * @throws Exception if KeyStore cannot be created
     */
    public KeyStore createKeyStore(KeyStoreType type) throws Exception {
        KeyStore keyStore = KeyStore.getInstance(type.getType(), type.getProvider());
        keyStore.load(null, null); // Initialize empty
        return keyStore;
    }

    /**
     * Load KeyStore from file using default type (PKCS12)
     *
     * @param keystorePath Path to KeyStore file
     * @param password KeyStore password
     * @return Loaded KeyStore
     * @throws Exception if KeyStore cannot be loaded
     */
    public KeyStore loadKeyStore(String keystorePath, char[] password) throws Exception {
        return loadKeyStore(keystorePath, password, KeyStoreType.PKCS12);
    }

    /**
     * Load KeyStore from file with specified type
     *
     * @param keystorePath Path to KeyStore file
     * @param password KeyStore password
     * @param type KeyStore type
     * @return Loaded KeyStore
     * @throws Exception if KeyStore cannot be loaded
     */
    public KeyStore loadKeyStore(String keystorePath, char[] password, KeyStoreType type)
            throws Exception {
        KeyStore keyStore = KeyStore.getInstance(type.getType(), type.getProvider());
        try (FileInputStream fis = new FileInputStream(keystorePath)) {
            keyStore.load(fis, password);
        }
        return keyStore;
    }

    /**
     * Load PKCS#11 KeyStore (HSM/Smart Card)
     * Requires PKCS#11 configuration file
     *
     * Example configuration file (pkcs11.cfg):
     * <pre>
     * name = SmartCard
     * library = /usr/lib/pkcs11/opensc-pkcs11.so
     * slot = 0
     * </pre>
     *
     * @param pkcs11ConfigPath Path to PKCS#11 configuration file
     * @param pin PIN/password for PKCS#11 token
     * @return Loaded PKCS#11 KeyStore
     * @throws Exception if KeyStore cannot be loaded
     */
    public KeyStore loadPKCS11KeyStore(String pkcs11ConfigPath, char[] pin) throws Exception {
        // Note: PKCS#11 provider configuration using reflection to avoid
        // direct dependency on internal sun.security.pkcs11.SunPKCS11 class

        Provider pkcs11Provider = null;

        // Try to get existing SunPKCS11 provider
        pkcs11Provider = Security.getProvider("SunPKCS11");

        if (pkcs11Provider == null) {
            // Configure new PKCS#11 provider using Provider.configure()
            // This is the Java 9+ recommended approach
            Provider baseProvider = Security.getProvider("SunPKCS11");

            if (baseProvider != null) {
                // Configure provider with config file
                pkcs11Provider = baseProvider.configure(pkcs11ConfigPath);
                Security.addProvider(pkcs11Provider);
            } else {
                // Fallback: Try to instantiate using reflection (for Java 8 compatibility)
                try {
                    @SuppressWarnings("unchecked")
                    Class<Provider> pkcs11Class = (Class<Provider>) Class.forName(
                        "sun.security.pkcs11.SunPKCS11");
                    java.lang.reflect.Constructor<Provider> constructor =
                        pkcs11Class.getConstructor(String.class);
                    pkcs11Provider = constructor.newInstance(pkcs11ConfigPath);
                    Security.addProvider(pkcs11Provider);
                } catch (Exception e) {
                    throw new Exception(
                        "PKCS#11 provider not available. " +
                        "Ensure SunPKCS11 provider is configured in java.security file. " +
                        "Error: " + e.getMessage(), e);
                }
            }
        }

        // Load KeyStore
        KeyStore keyStore = KeyStore.getInstance("PKCS11", pkcs11Provider);
        keyStore.load(null, pin);
        return keyStore;
    }

    /**
     * Save KeyStore to file with secure permissions
     *
     * @param keyStore KeyStore to save
     * @param keystorePath Path where to save
     * @param password Password to protect KeyStore
     * @throws Exception if KeyStore cannot be saved
     */
    public void saveKeyStore(KeyStore keyStore, String keystorePath, char[] password)
            throws Exception {
        Path path = Paths.get(keystorePath);

        // Save KeyStore
        try (FileOutputStream fos = new FileOutputStream(keystorePath)) {
            keyStore.store(fos, password);
        }

        // Set secure file permissions (Unix/Linux)
        if (path.getFileSystem().supportedFileAttributeViews().contains("posix")) {
            Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rw-------");
            Files.setPosixFilePermissions(path, perms);
        }
    }

    // ========================================================================
    // Key Pair and Certificate Generation
    // ========================================================================

    /**
     * Generate RSA key pair using Bouncy Castle provider
     *
     * @return Generated KeyPair (public + private keys)
     * @throws NoSuchAlgorithmException if RSA algorithm not available
     * @throws NoSuchProviderException if BC provider not available
     */
    public KeyPair generateKeyPair() throws NoSuchAlgorithmException, NoSuchProviderException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(KEY_ALGORITHM, PROVIDER);
        keyGen.initialize(KEY_SIZE, new SecureRandom());
        return keyGen.generateKeyPair();
    }

    /**
     * Generate key pair with custom key size
     *
     * @param keySize Key size in bits (2048, 3072, 4096)
     * @return Generated KeyPair
     * @throws NoSuchAlgorithmException if algorithm not available
     * @throws NoSuchProviderException if provider not available
     */
    public KeyPair generateKeyPair(int keySize) throws NoSuchAlgorithmException,
            NoSuchProviderException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(KEY_ALGORITHM, PROVIDER);
        keyGen.initialize(keySize, new SecureRandom());
        return keyGen.generateKeyPair();
    }

    /**
     * Create PKCS#10 Certificate Signing Request (CSR)
     *
     * @param keyPair Key pair for CSR
     * @param subjectDN Subject Distinguished Name (e.g., "CN=John Doe, E=john@corp.local")
     * @return PKCS#10 CSR
     * @throws Exception if CSR creation fails
     */
    public PKCS10CertificationRequest createCSR(KeyPair keyPair, String subjectDN)
            throws Exception {
        X500Name subject = new X500Name(subjectDN);
        SubjectPublicKeyInfo publicKeyInfo = SubjectPublicKeyInfo.getInstance(
            keyPair.getPublic().getEncoded());

        PKCS10CertificationRequestBuilder csrBuilder =
            new PKCS10CertificationRequestBuilder(subject, publicKeyInfo);

        // Sign CSR with private key (Proof of Possession)
        ContentSigner signer = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM)
            .setProvider(PROVIDER)
            .build(keyPair.getPrivate());

        return csrBuilder.build(signer);
    }

    /**
     * Verify CSR signature (validates Proof of Possession)
     *
     * @param csr Certificate Signing Request to verify
     * @return true if signature is valid, false otherwise
     */
    public boolean verifyCSRSignature(PKCS10CertificationRequest csr) {
        try {
            ContentVerifierProvider verifier =
                new JcaContentVerifierProviderBuilder()
                    .setProvider(PROVIDER)
                    .build(csr.getSubjectPublicKeyInfo());
            return csr.isSignatureValid(verifier);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Generate self-signed X.509 certificate
     *
     * @param keyPair Key pair for certificate
     * @param subjectDN Subject Distinguished Name
     * @param validityDays Certificate validity in days
     * @return Self-signed X509Certificate
     * @throws Exception if certificate generation fails
     */
    public X509Certificate generateSelfSignedCertificate(
            KeyPair keyPair, String subjectDN, int validityDays) throws Exception {

        long now = System.currentTimeMillis();
        Date notBefore = new Date(now);
        Date notAfter = new Date(now + (validityDays * 24L * 60 * 60 * 1000));

        X500Name issuer = new X500Name(subjectDN);
        X500Name subject = new X500Name(subjectDN);
        BigInteger serialNumber = BigInteger.valueOf(now);

        SubjectPublicKeyInfo publicKeyInfo = SubjectPublicKeyInfo.getInstance(
            keyPair.getPublic().getEncoded());

        X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(
            issuer, serialNumber, notBefore, notAfter, subject, publicKeyInfo);

        ContentSigner signer = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM)
            .setProvider(PROVIDER)
            .build(keyPair.getPrivate());

        X509CertificateHolder certHolder = certBuilder.build(signer);

        return new JcaX509CertificateConverter()
            .setProvider(PROVIDER)
            .getCertificate(certHolder);
    }

    // ========================================================================
    // KeyStore Entry Management (Core Methods)
    // ========================================================================

    /**
     * Store private key with certificate chain in KeyStore
     * This is the PRIMARY method for certificate storage in RA application
     *
     * @param keyStore Target KeyStore
     * @param alias Unique identifier for this entry
     * @param privateKey Private key to store
     * @param password Password to protect the private key
     * @param certificateChain Certificate chain (end-entity cert first)
     * @throws KeyStoreException if entry cannot be stored
     */
    public void storeKeyEntry(KeyStore keyStore, String alias,
            PrivateKey privateKey, char[] password, Certificate[] certificateChain)
            throws KeyStoreException {

        // Validate inputs
        if (alias == null || alias.trim().isEmpty()) {
            throw new IllegalArgumentException("Alias cannot be null or empty");
        }
        if (privateKey == null) {
            throw new IllegalArgumentException("Private key cannot be null");
        }
        if (certificateChain == null || certificateChain.length == 0) {
            throw new IllegalArgumentException("Certificate chain cannot be null or empty");
        }

        // Store key entry using KeyStore.setKeyEntry()
        keyStore.setKeyEntry(alias, privateKey, password, certificateChain);
    }

    /**
     * Retrieve private key from KeyStore
     *
     * @param keyStore Source KeyStore
     * @param alias Entry alias
     * @param password Entry password
     * @return Private key
     * @throws Exception if key cannot be retrieved
     */
    public PrivateKey getPrivateKey(KeyStore keyStore, String alias, char[] password)
            throws Exception {
        if (!keyStore.containsAlias(alias)) {
            throw new KeyStoreException("Alias not found: " + alias);
        }
        Key key = keyStore.getKey(alias, password);
        if (!(key instanceof PrivateKey)) {
            throw new KeyStoreException("Entry is not a private key: " + alias);
        }
        return (PrivateKey) key;
    }

    /**
     * Retrieve certificate chain from KeyStore
     *
     * @param keyStore Source KeyStore
     * @param alias Entry alias
     * @return Certificate chain
     * @throws KeyStoreException if chain cannot be retrieved
     */
    public Certificate[] getCertificateChain(KeyStore keyStore, String alias)
            throws KeyStoreException {
        if (!keyStore.containsAlias(alias)) {
            throw new KeyStoreException("Alias not found: " + alias);
        }
        Certificate[] chain = keyStore.getCertificateChain(alias);
        if (chain == null || chain.length == 0) {
            throw new KeyStoreException("No certificate chain found for alias: " + alias);
        }
        return chain;
    }

    /**
     * List all entries in KeyStore
     *
     * @param keyStore KeyStore to list
     * @return List of KeyStoreEntry objects
     * @throws KeyStoreException if entries cannot be listed
     */
    public List<KeyStoreEntry> listEntries(KeyStore keyStore) throws KeyStoreException {
        List<KeyStoreEntry> entries = new ArrayList<>();
        Enumeration<String> aliases = keyStore.aliases();

        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            KeyStoreEntry entry = new KeyStoreEntry();
            entry.setAlias(alias);
            entry.setCreationDate(keyStore.getCreationDate(alias));

            if (keyStore.isKeyEntry(alias)) {
                entry.setType(KeyStoreEntryType.PRIVATE_KEY);
                Certificate[] chain = keyStore.getCertificateChain(alias);
                if (chain != null && chain.length > 0 && chain[0] instanceof X509Certificate) {
                    X509Certificate cert = (X509Certificate) chain[0];
                    entry.setSubjectDN(cert.getSubjectDN().toString());
                    entry.setIssuerDN(cert.getIssuerDN().toString());
                    entry.setSerialNumber(cert.getSerialNumber().toString());
                    entry.setNotBefore(cert.getNotBefore());
                    entry.setNotAfter(cert.getNotAfter());
                    entry.setCertificateChainLength(chain.length);
                }
            } else if (keyStore.isCertificateEntry(alias)) {
                entry.setType(KeyStoreEntryType.CERTIFICATE);
                Certificate cert = keyStore.getCertificate(alias);
                if (cert instanceof X509Certificate) {
                    X509Certificate x509 = (X509Certificate) cert;
                    entry.setSubjectDN(x509.getSubjectDN().toString());
                    entry.setIssuerDN(x509.getIssuerDN().toString());
                }
            }

            entries.add(entry);
        }

        return entries;
    }

    /**
     * Delete entry from KeyStore
     *
     * @param keyStore KeyStore to modify
     * @param alias Entry alias to delete
     * @throws KeyStoreException if entry cannot be deleted
     */
    public void deleteEntry(KeyStore keyStore, String alias) throws KeyStoreException {
        if (!keyStore.containsAlias(alias)) {
            throw new KeyStoreException("Alias not found: " + alias);
        }
        keyStore.deleteEntry(alias);
    }

    // ========================================================================
    // Utility Methods
    // ========================================================================

    /**
     * Generate secure random password
     *
     * @param length Password length
     * @return Random password as char array
     */
    public char[] generateSecurePassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        SecureRandom random = new SecureRandom();
        char[] password = new char[length];
        for (int i = 0; i < length; i++) {
            password[i] = chars.charAt(random.nextInt(chars.length()));
        }
        return password;
    }

    /**
     * Clear password from memory
     *
     * @param password Password to clear
     */
    public void clearPassword(char[] password) {
        if (password != null) {
            Arrays.fill(password, '\0');
        }
    }

    /**
     * Check if certificate is expired
     *
     * @param certificate Certificate to check
     * @return true if expired, false otherwise
     */
    public boolean isCertificateExpired(X509Certificate certificate) {
        Date now = new Date();
        try {
            certificate.checkValidity(now);
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Get days until certificate expiration
     *
     * @param certificate Certificate to check
     * @return Days until expiration (negative if already expired)
     */
    public long getDaysUntilExpiration(X509Certificate certificate) {
        Date now = new Date();
        Date expiry = certificate.getNotAfter();
        long diffMillis = expiry.getTime() - now.getTime();
        return diffMillis / (24 * 60 * 60 * 1000);
    }

    // ========================================================================
    // Supporting Classes
    // ========================================================================

    /**
     * Represents a KeyStore entry with metadata
     */
    public static class KeyStoreEntry {
        private String alias;
        private KeyStoreEntryType type;
        private Date creationDate;
        private String subjectDN;
        private String issuerDN;
        private String serialNumber;
        private Date notBefore;
        private Date notAfter;
        private int certificateChainLength;

        // Getters and Setters
        public String getAlias() { return alias; }
        public void setAlias(String alias) { this.alias = alias; }

        public KeyStoreEntryType getType() { return type; }
        public void setType(KeyStoreEntryType type) { this.type = type; }

        public Date getCreationDate() { return creationDate; }
        public void setCreationDate(Date creationDate) { this.creationDate = creationDate; }

        public String getSubjectDN() { return subjectDN; }
        public void setSubjectDN(String subjectDN) { this.subjectDN = subjectDN; }

        public String getIssuerDN() { return issuerDN; }
        public void setIssuerDN(String issuerDN) { this.issuerDN = issuerDN; }

        public String getSerialNumber() { return serialNumber; }
        public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

        public Date getNotBefore() { return notBefore; }
        public void setNotBefore(Date notBefore) { this.notBefore = notBefore; }

        public Date getNotAfter() { return notAfter; }
        public void setNotAfter(Date notAfter) { this.notAfter = notAfter; }

        public int getCertificateChainLength() { return certificateChainLength; }
        public void setCertificateChainLength(int length) { this.certificateChainLength = length; }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("KeyStoreEntry{");
            sb.append("alias='").append(alias).append('\'');
            sb.append(", type=").append(type);
            sb.append(", subjectDN='").append(subjectDN).append('\'');
            if (notBefore != null && notAfter != null) {
                sb.append(", validity=").append(notBefore).append(" to ").append(notAfter);
            }
            sb.append('}');
            return sb.toString();
        }
    }

    /**
     * KeyStore entry types
     */
    public enum KeyStoreEntryType {
        PRIVATE_KEY,
        CERTIFICATE,
        SECRET_KEY
    }
}
