import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;

import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Date;

/**
 * Java KeyStore POC using Bouncy Castle
 * Demonstrates:
 * 1. Key pair generation
 * 2. CSR creation and signing
 * 3. Self-signed certificate generation
 * 4. KeyStore.setKeyEntry() - Store private key with certificate chain
 * 5. KeyStore operations - Store, load, retrieve, list entries
 *
 * Use Case: Registration Authority (RA) Web Application
 * - Generate key pairs for certificate requests
 * - Create PKCS#10 CSR for submission to CA
 * - Store issued certificates with private keys
 * - Manage certificate inventory
 */
public class KeyStorePOC {

    static {
        // Register Bouncy Castle as security provider
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * Main demonstration method
     */
    public static void main(String[] args) {
        try {
            System.out.println("═══════════════════════════════════════════════════════");
            System.out.println("    Java KeyStore POC with Bouncy Castle");
            System.out.println("    For RA Web Application Certificate Management");
            System.out.println("═══════════════════════════════════════════════════════\n");

            // Step 1: Generate RSA Key Pair
            System.out.println("Step 1: Generating RSA 2048-bit Key Pair...");
            KeyPair keyPair = generateKeyPair();
            System.out.println("✓ Key pair generated successfully");
            System.out.println("  Algorithm: " + keyPair.getPrivate().getAlgorithm());
            System.out.println("  Format: " + keyPair.getPrivate().getFormat());
            System.out.println();

            // Step 2: Create PKCS#10 Certificate Signing Request (CSR)
            System.out.println("Step 2: Creating PKCS#10 CSR...");
            String subjectDN = "CN=John Doe, E=john.doe@corp.local, OU=Engineering, O=Corp, C=US";
            PKCS10CertificationRequest csr = createCSR(keyPair, subjectDN);
            System.out.println("✓ CSR created successfully");
            System.out.println("  Subject DN: " + csr.getSubject());
            System.out.println("  Signature Algorithm: " + csr.getSignatureAlgorithm().getAlgorithm());

            // Verify CSR signature
            boolean csrValid = verifyCSRSignature(csr);
            System.out.println("  CSR Signature Valid: " + (csrValid ? "✓ YES" : "✗ NO"));
            System.out.println();

            // Step 3: Generate Self-Signed Certificate (simulates CA issuing certificate)
            System.out.println("Step 3: Generating Self-Signed Certificate...");
            X509Certificate certificate = generateSelfSignedCertificate(keyPair, subjectDN);
            System.out.println("✓ Certificate generated successfully");
            System.out.println("  Serial Number: " + certificate.getSerialNumber());
            System.out.println("  Valid From: " + certificate.getNotBefore());
            System.out.println("  Valid Until: " + certificate.getNotAfter());
            System.out.println("  Issuer: " + certificate.getIssuerDN());
            System.out.println("  Subject: " + certificate.getSubjectDN());
            System.out.println();

            // Step 4: Create KeyStore and use setKeyEntry()
            System.out.println("Step 4: Creating PKCS12 KeyStore...");
            KeyStore keyStore = KeyStore.getInstance("PKCS12", "BC");
            keyStore.load(null, null); // Initialize empty keystore
            System.out.println("✓ KeyStore initialized (type: PKCS12)");
            System.out.println();

            // Step 5: Store Private Key and Certificate using setKeyEntry()
            System.out.println("Step 5: Storing Private Key and Certificate using setKeyEntry()...");
            String alias = "john-doe-cert";
            char[] password = "keystorePassword123".toCharArray();

            // Create certificate chain (in production, this would include intermediate CA certs)
            Certificate[] certificateChain = new Certificate[] { certificate };

            // CRITICAL METHOD: setKeyEntry()
            // Parameters:
            // 1. alias - unique identifier for this key entry
            // 2. privateKey - the private key to store
            // 3. password - password to protect the private key
            // 4. chain - certificate chain (end-entity cert + intermediate CAs)
            keyStore.setKeyEntry(alias, keyPair.getPrivate(), password, certificateChain);

            System.out.println("✓ Private key and certificate stored successfully");
            System.out.println("  Alias: " + alias);
            System.out.println("  Key Type: " + keyPair.getPrivate().getAlgorithm());
            System.out.println("  Certificate Chain Length: " + certificateChain.length);
            System.out.println();

            // Step 6: Save KeyStore to file
            System.out.println("Step 6: Saving KeyStore to file...");
            String keystorePath = "ra-keystore.p12";
            saveKeyStore(keyStore, keystorePath, password);
            System.out.println("✓ KeyStore saved to: " + keystorePath);
            System.out.println();

            // Step 7: Load KeyStore from file
            System.out.println("Step 7: Loading KeyStore from file...");
            KeyStore loadedKeyStore = loadKeyStore(keystorePath, password);
            System.out.println("✓ KeyStore loaded successfully");
            System.out.println("  Type: " + loadedKeyStore.getType());
            System.out.println("  Provider: " + loadedKeyStore.getProvider().getName());
            System.out.println();

            // Step 8: Retrieve and verify stored entries
            System.out.println("Step 8: Retrieving stored entries from KeyStore...");
            listKeyStoreEntries(loadedKeyStore);
            System.out.println();

            // Step 9: Retrieve Private Key and Certificate
            System.out.println("Step 9: Retrieving Private Key and Certificate...");
            Key retrievedKey = loadedKeyStore.getKey(alias, password);
            Certificate[] retrievedChain = loadedKeyStore.getCertificateChain(alias);

            System.out.println("✓ Retrieved successfully");
            System.out.println("  Private Key Algorithm: " + retrievedKey.getAlgorithm());
            System.out.println("  Private Key Format: " + retrievedKey.getFormat());
            System.out.println("  Certificate Chain Length: " + retrievedChain.length);
            System.out.println("  Certificate Subject: " +
                ((X509Certificate) retrievedChain[0]).getSubjectDN());
            System.out.println();

            // Step 10: Verify retrieved key matches original
            System.out.println("Step 10: Verifying key integrity...");
            boolean keysMatch = keyPair.getPrivate().equals(retrievedKey);
            System.out.println("✓ Private key integrity: " + (keysMatch ? "VERIFIED" : "MISMATCH"));

            boolean certsMatch = certificate.equals(retrievedChain[0]);
            System.out.println("✓ Certificate integrity: " + (certsMatch ? "VERIFIED" : "MISMATCH"));
            System.out.println();

            // Demonstration: Adding multiple entries
            System.out.println("Step 11: Adding multiple certificate entries...");
            addMultipleEntries(loadedKeyStore, password);
            saveKeyStore(loadedKeyStore, keystorePath, password);
            System.out.println();

            System.out.println("Step 12: Final KeyStore contents:");
            listKeyStoreEntries(loadedKeyStore);
            System.out.println();

            System.out.println("═══════════════════════════════════════════════════════");
            System.out.println("✓ POC Completed Successfully!");
            System.out.println("═══════════════════════════════════════════════════════");
            System.out.println("\nKey Methods Demonstrated:");
            System.out.println("  ✓ KeyStore.getInstance() - Create KeyStore");
            System.out.println("  ✓ KeyStore.load() - Initialize/Load KeyStore");
            System.out.println("  ✓ KeyStore.setKeyEntry() - Store private key + cert chain");
            System.out.println("  ✓ KeyStore.store() - Save KeyStore to file");
            System.out.println("  ✓ KeyStore.getKey() - Retrieve private key");
            System.out.println("  ✓ KeyStore.getCertificateChain() - Retrieve cert chain");
            System.out.println("  ✓ KeyStore.aliases() - List all entries");
            System.out.println("  ✓ KeyStore.isKeyEntry() - Check entry type");

        } catch (Exception e) {
            System.err.println("✗ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Generate RSA 2048-bit key pair
     */
    private static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA",
            new BouncyCastleProvider());
        keyPairGenerator.initialize(2048, new SecureRandom());
        return keyPairGenerator.generateKeyPair();
    }

    /**
     * Create PKCS#10 Certificate Signing Request (CSR)
     * This demonstrates what an end entity would submit to the RA
     */
    private static PKCS10CertificationRequest createCSR(KeyPair keyPair, String subjectDN)
            throws Exception {
        X500Name subject = new X500Name(subjectDN);

        // Create public key info from key pair
        SubjectPublicKeyInfo publicKeyInfo = SubjectPublicKeyInfo.getInstance(
            keyPair.getPublic().getEncoded());

        // Build CSR
        org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder csrBuilder =
            new org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder(subject, publicKeyInfo);

        // Sign CSR with private key (Proof of Possession)
        ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSA")
            .setProvider("BC")
            .build(keyPair.getPrivate());

        return csrBuilder.build(signer);
    }

    /**
     * Verify CSR signature (RA validation step)
     */
    private static boolean verifyCSRSignature(PKCS10CertificationRequest csr) {
        try {
            org.bouncycastle.operator.ContentVerifierProvider verifierProvider =
                new org.bouncycastle.operator.jcajce.JcaContentVerifierProviderBuilder()
                    .setProvider("BC")
                    .build(csr.getSubjectPublicKeyInfo());

            return csr.isSignatureValid(verifierProvider);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Generate self-signed certificate
     * In production, this would be done by the Certificate Authority (CA)
     */
    private static X509Certificate generateSelfSignedCertificate(KeyPair keyPair, String subjectDN)
            throws Exception {

        long now = System.currentTimeMillis();
        Date notBefore = new Date(now);
        Date notAfter = new Date(now + 365L * 24 * 60 * 60 * 1000); // 1 year validity

        X500Name issuer = new X500Name(subjectDN);
        X500Name subject = new X500Name(subjectDN);

        BigInteger serialNumber = BigInteger.valueOf(now);

        SubjectPublicKeyInfo publicKeyInfo = SubjectPublicKeyInfo.getInstance(
            keyPair.getPublic().getEncoded());

        X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(
            issuer,
            serialNumber,
            notBefore,
            notAfter,
            subject,
            publicKeyInfo
        );

        ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSA")
            .setProvider("BC")
            .build(keyPair.getPrivate());

        X509CertificateHolder certHolder = certBuilder.build(signer);

        return new JcaX509CertificateConverter()
            .setProvider("BC")
            .getCertificate(certHolder);
    }

    /**
     * Save KeyStore to file
     * IMPORTANT: In production, use strong passwords and secure storage
     */
    private static void saveKeyStore(KeyStore keyStore, String filePath, char[] password)
            throws Exception {
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            keyStore.store(fos, password);
        }
    }

    /**
     * Load KeyStore from file
     */
    private static KeyStore loadKeyStore(String filePath, char[] password) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12", "BC");
        try (FileInputStream fis = new FileInputStream(filePath)) {
            keyStore.load(fis, password);
        }
        return keyStore;
    }

    /**
     * List all entries in KeyStore
     */
    private static void listKeyStoreEntries(KeyStore keyStore) throws Exception {
        System.out.println("  KeyStore Entries:");
        System.out.println("  ─────────────────────────────────────────");

        int entryCount = 0;
        java.util.Enumeration<String> aliases = keyStore.aliases();

        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            entryCount++;

            System.out.println("  [" + entryCount + "] Alias: " + alias);

            if (keyStore.isKeyEntry(alias)) {
                System.out.println("      Type: Private Key Entry");
                Certificate[] chain = keyStore.getCertificateChain(alias);
                System.out.println("      Certificate Chain Length: " + chain.length);

                if (chain.length > 0 && chain[0] instanceof X509Certificate) {
                    X509Certificate cert = (X509Certificate) chain[0];
                    System.out.println("      Subject: " + cert.getSubjectDN());
                    System.out.println("      Issuer: " + cert.getIssuerDN());
                    System.out.println("      Serial: " + cert.getSerialNumber());
                    System.out.println("      Valid: " + cert.getNotBefore() +
                        " to " + cert.getNotAfter());
                }
            } else if (keyStore.isCertificateEntry(alias)) {
                System.out.println("      Type: Trusted Certificate Entry");
                Certificate cert = keyStore.getCertificate(alias);
                if (cert instanceof X509Certificate) {
                    X509Certificate x509 = (X509Certificate) cert;
                    System.out.println("      Subject: " + x509.getSubjectDN());
                }
            }
            System.out.println();
        }

        System.out.println("  Total Entries: " + entryCount);
    }

    /**
     * Demonstrate adding multiple entries to KeyStore
     */
    private static void addMultipleEntries(KeyStore keyStore, char[] password) throws Exception {
        // Entry 2: Alice's certificate
        KeyPair aliceKeyPair = generateKeyPair();
        String aliceSubjectDN = "CN=Alice Smith, E=alice@corp.local, OU=Engineering, O=Corp, C=US";
        X509Certificate aliceCert = generateSelfSignedCertificate(aliceKeyPair, aliceSubjectDN);

        keyStore.setKeyEntry(
            "alice-cert",
            aliceKeyPair.getPrivate(),
            password,
            new Certificate[] { aliceCert }
        );
        System.out.println("  ✓ Added: alice-cert");

        // Entry 3: Bob's certificate
        KeyPair bobKeyPair = generateKeyPair();
        String bobSubjectDN = "CN=Bob Admin, E=bob.admin@corp.local, OU=IT, O=Corp, C=US";
        X509Certificate bobCert = generateSelfSignedCertificate(bobKeyPair, bobSubjectDN);

        keyStore.setKeyEntry(
            "bob-admin-cert",
            bobKeyPair.getPrivate(),
            password,
            new Certificate[] { bobCert }
        );
        System.out.println("  ✓ Added: bob-admin-cert");

        // Entry 4: Server certificate (different key usage)
        KeyPair serverKeyPair = generateKeyPair();
        String serverSubjectDN = "CN=ra.corp.local, O=Corp, C=US";
        X509Certificate serverCert = generateSelfSignedCertificate(serverKeyPair, serverSubjectDN);

        keyStore.setKeyEntry(
            "ra-server-cert",
            serverKeyPair.getPrivate(),
            password,
            new Certificate[] { serverCert }
        );
        System.out.println("  ✓ Added: ra-server-cert");
    }
}
