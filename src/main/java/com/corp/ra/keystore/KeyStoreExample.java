package com.corp.ra.keystore;

import org.bouncycastle.pkcs.PKCS10CertificationRequest;

import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * Practical examples demonstrating KeyStoreManager usage
 * for RA Web Application certificate management scenarios
 *
 * @author RA Development Team
 * @version 1.0
 * @since 2026-01-16
 */
public class KeyStoreExample {

    private final KeyStoreManager ksManager = new KeyStoreManager();

    /**
     * Example 1: End Entity Certificate Enrollment
     * Scenario: User submits CSR, RA issues certificate, stores in KeyStore
     */
    public void example1_EndEntityEnrollment() throws Exception {
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("Example 1: End Entity Certificate Enrollment");
        System.out.println("═══════════════════════════════════════════════════════\n");

        // Step 1: End entity generates key pair
        System.out.println("Step 1: Generating key pair for end entity...");
        KeyPair userKeyPair = ksManager.generateKeyPair();
        System.out.println("✓ Key pair generated\n");

        // Step 2: Create CSR
        System.out.println("Step 2: Creating PKCS#10 CSR...");
        String userDN = "CN=John Doe, E=john.doe@corp.local, OU=Engineering, O=Corp, C=US";
        PKCS10CertificationRequest csr = ksManager.createCSR(userKeyPair, userDN);

        boolean csrValid = ksManager.verifyCSRSignature(csr);
        System.out.println("✓ CSR created and signature verified: " + csrValid + "\n");

        // Step 3: RA issues certificate (simulated with self-signed)
        System.out.println("Step 3: RA issuing certificate...");
        X509Certificate certificate = ksManager.generateSelfSignedCertificate(
            userKeyPair, userDN, 365);
        System.out.println("✓ Certificate issued");
        System.out.println("  Serial: " + certificate.getSerialNumber());
        System.out.println("  Valid until: " + certificate.getNotAfter() + "\n");

        // Step 4: Create user's KeyStore
        System.out.println("Step 4: Creating user's KeyStore...");
        KeyStore userKeyStore = ksManager.createKeyStore();
        System.out.println("✓ KeyStore created\n");

        // Step 5: Store private key and certificate using setKeyEntry()
        System.out.println("Step 5: Storing certificate with private key...");
        String alias = "john-doe-email-cert";
        char[] password = "User@Password123".toCharArray();
        Certificate[] chain = new Certificate[] { certificate };

        ksManager.storeKeyEntry(userKeyStore, alias, userKeyPair.getPrivate(),
            password, chain);
        System.out.println("✓ Certificate stored with alias: " + alias + "\n");

        // Step 6: Save KeyStore to file
        System.out.println("Step 6: Saving KeyStore to file...");
        String keystorePath = "john-doe-keystore.p12";
        ksManager.saveKeyStore(userKeyStore, keystorePath, password);
        System.out.println("✓ KeyStore saved: " + keystorePath + "\n");

        // Step 7: Verify - Load and retrieve
        System.out.println("Step 7: Verifying stored data...");
        KeyStore loadedKS = ksManager.loadKeyStore(keystorePath, password);
        PrivateKey retrievedKey = ksManager.getPrivateKey(loadedKS, alias, password);
        Certificate[] retrievedChain = ksManager.getCertificateChain(loadedKS, alias);

        System.out.println("✓ Retrieved private key: " + retrievedKey.getAlgorithm());
        System.out.println("✓ Retrieved certificate chain length: " + retrievedChain.length);
        System.out.println("✓ Certificate subject: " +
            ((X509Certificate) retrievedChain[0]).getSubjectDN());

        // Clean up
        ksManager.clearPassword(password);
        System.out.println("\n✓ Example 1 completed successfully!\n");
    }

    /**
     * Example 2: Auto-Enrollment for Multiple Users
     * Scenario: RA automatically enrolls certificates for AD users
     */
    public void example2_AutoEnrollment() throws Exception {
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("Example 2: Auto-Enrollment for Multiple Users");
        System.out.println("═══════════════════════════════════════════════════════\n");

        // Create centralized RA KeyStore
        System.out.println("Creating centralized RA KeyStore...");
        KeyStore raKeyStore = ksManager.createKeyStore();
        char[] raPassword = "RA@MasterPassword123".toCharArray();

        // Simulate AD users
        String[] adUsers = {
            "CN=Alice Smith, E=alice@corp.local, OU=Engineering, O=Corp, C=US",
            "CN=Bob Admin, E=bob.admin@corp.local, OU=IT, O=Corp, C=US",
            "CN=Charlie Dev, E=charlie@corp.local, OU=Engineering, O=Corp, C=US"
        };

        // Auto-enroll each user
        for (int i = 0; i < adUsers.length; i++) {
            System.out.println("\nAuto-enrolling user " + (i + 1) + "...");

            // Generate key pair
            KeyPair keyPair = ksManager.generateKeyPair();

            // Generate certificate
            X509Certificate cert = ksManager.generateSelfSignedCertificate(
                keyPair, adUsers[i], 365);

            // Create unique alias
            String alias = "autoenroll-user-" + (i + 1);
            char[] entryPassword = ksManager.generateSecurePassword(16);

            // Store in centralized KeyStore
            ksManager.storeKeyEntry(raKeyStore, alias, keyPair.getPrivate(),
                entryPassword, new Certificate[] { cert });

            System.out.println("✓ Enrolled: " + alias);
            System.out.println("  Subject: " + cert.getSubjectDN());

            // In production, would send PKCS#12 to user
            ksManager.clearPassword(entryPassword);
        }

        // Save centralized KeyStore
        String raKeystorePath = "ra-autoenroll-keystore.p12";
        ksManager.saveKeyStore(raKeyStore, raKeystorePath, raPassword);
        System.out.println("\n✓ Centralized KeyStore saved: " + raKeystorePath);

        // List all entries
        System.out.println("\nListing all enrolled certificates:");
        List<KeyStoreManager.KeyStoreEntry> entries = ksManager.listEntries(raKeyStore);
        for (KeyStoreManager.KeyStoreEntry entry : entries) {
            System.out.println("  • " + entry.getAlias() + " - " + entry.getSubjectDN());
        }

        ksManager.clearPassword(raPassword);
        System.out.println("\n✓ Example 2 completed successfully!\n");
    }

    /**
     * Example 3: Certificate Expiration Monitoring
     * Scenario: Check certificates expiring within 30 days
     */
    public void example3_ExpirationMonitoring() throws Exception {
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("Example 3: Certificate Expiration Monitoring");
        System.out.println("═══════════════════════════════════════════════════════\n");

        // Create KeyStore with certificates of varying validity
        KeyStore monitorKS = ksManager.createKeyStore();
        char[] password = "Monitor@Password123".toCharArray();

        // Certificate 1: Valid for 365 days
        KeyPair kp1 = ksManager.generateKeyPair();
        X509Certificate cert1 = ksManager.generateSelfSignedCertificate(
            kp1, "CN=Long Valid Cert, O=Corp", 365);
        ksManager.storeKeyEntry(monitorKS, "cert-365-days", kp1.getPrivate(),
            password, new Certificate[] { cert1 });

        // Certificate 2: Valid for 15 days (expiring soon)
        KeyPair kp2 = ksManager.generateKeyPair();
        X509Certificate cert2 = ksManager.generateSelfSignedCertificate(
            kp2, "CN=Expiring Soon Cert, O=Corp", 15);
        ksManager.storeKeyEntry(monitorKS, "cert-15-days", kp2.getPrivate(),
            password, new Certificate[] { cert2 });

        // Certificate 3: Valid for 5 days (critical)
        KeyPair kp3 = ksManager.generateKeyPair();
        X509Certificate cert3 = ksManager.generateSelfSignedCertificate(
            kp3, "CN=Critical Cert, O=Corp", 5);
        ksManager.storeKeyEntry(monitorKS, "cert-5-days", kp3.getPrivate(),
            password, new Certificate[] { cert3 });

        // Monitor expiration
        System.out.println("Certificate Expiration Report:");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        List<KeyStoreManager.KeyStoreEntry> entries = ksManager.listEntries(monitorKS);
        for (KeyStoreManager.KeyStoreEntry entry : entries) {
            Certificate[] chain = ksManager.getCertificateChain(monitorKS, entry.getAlias());
            if (chain[0] instanceof X509Certificate) {
                X509Certificate cert = (X509Certificate) chain[0];
                long daysRemaining = ksManager.getDaysUntilExpiration(cert);
                boolean expired = ksManager.isCertificateExpired(cert);

                String status;
                if (expired) {
                    status = "✗ EXPIRED";
                } else if (daysRemaining <= 7) {
                    status = "⚠ CRITICAL (≤7 days)";
                } else if (daysRemaining <= 30) {
                    status = "⚠ WARNING (≤30 days)";
                } else {
                    status = "✓ VALID";
                }

                System.out.println("\nAlias: " + entry.getAlias());
                System.out.println("  Subject: " + cert.getSubjectDN());
                System.out.println("  Days remaining: " + daysRemaining);
                System.out.println("  Status: " + status);

                // Action recommendation
                if (!expired && daysRemaining <= 30) {
                    System.out.println("  Action: Renew certificate before " + cert.getNotAfter());
                }
            }
        }

        ksManager.clearPassword(password);
        System.out.println("\n✓ Example 3 completed successfully!\n");
    }

    /**
     * Example 4: Certificate Import/Export
     * Scenario: Import existing PKCS#12 and export to new KeyStore
     */
    public void example4_ImportExport() throws Exception {
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("Example 4: Certificate Import/Export");
        System.out.println("═══════════════════════════════════════════════════════\n");

        // Create source KeyStore with certificates
        System.out.println("Creating source KeyStore...");
        KeyStore sourceKS = ksManager.createKeyStore();
        char[] sourcePassword = "Source@Password123".toCharArray();

        for (int i = 1; i <= 3; i++) {
            KeyPair kp = ksManager.generateKeyPair();
            X509Certificate cert = ksManager.generateSelfSignedCertificate(
                kp, "CN=User " + i + ", O=Corp", 365);
            ksManager.storeKeyEntry(sourceKS, "user-" + i, kp.getPrivate(),
                sourcePassword, new Certificate[] { cert });
        }

        String sourcePath = "source-keystore.p12";
        ksManager.saveKeyStore(sourceKS, sourcePath, sourcePassword);
        System.out.println("✓ Source KeyStore created with 3 certificates\n");

        // Create destination KeyStore
        System.out.println("Creating destination KeyStore...");
        KeyStore destKS = ksManager.createKeyStore();
        char[] destPassword = "Dest@Password123".toCharArray();

        // Import all entries from source to destination
        System.out.println("Importing certificates...");
        List<KeyStoreManager.KeyStoreEntry> sourceEntries = ksManager.listEntries(sourceKS);

        for (KeyStoreManager.KeyStoreEntry entry : sourceEntries) {
            if (entry.getType() == KeyStoreManager.KeyStoreEntryType.PRIVATE_KEY) {
                // Retrieve from source
                PrivateKey key = ksManager.getPrivateKey(sourceKS, entry.getAlias(),
                    sourcePassword);
                Certificate[] chain = ksManager.getCertificateChain(sourceKS,
                    entry.getAlias());

                // Store in destination with new alias
                String newAlias = "imported-" + entry.getAlias();
                ksManager.storeKeyEntry(destKS, newAlias, key, destPassword, chain);

                System.out.println("✓ Imported: " + entry.getAlias() + " → " + newAlias);
            }
        }

        // Save destination KeyStore
        String destPath = "destination-keystore.p12";
        ksManager.saveKeyStore(destKS, destPath, destPassword);
        System.out.println("\n✓ Destination KeyStore saved: " + destPath);

        // Verify import
        System.out.println("\nVerifying imported certificates:");
        List<KeyStoreManager.KeyStoreEntry> destEntries = ksManager.listEntries(destKS);
        System.out.println("  Total imported: " + destEntries.size());
        for (KeyStoreManager.KeyStoreEntry entry : destEntries) {
            System.out.println("  • " + entry.getAlias());
        }

        ksManager.clearPassword(sourcePassword);
        ksManager.clearPassword(destPassword);
        System.out.println("\n✓ Example 4 completed successfully!\n");
    }

    /**
     * Example 5: RA Officer Digital Signature
     * Scenario: Officer signs approval using certificate from KeyStore
     */
    public void example5_DigitalSignature() throws Exception {
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("Example 5: RA Officer Digital Signature");
        System.out.println("═══════════════════════════════════════════════════════\n");

        // Setup: Create RA Officer's KeyStore with signing certificate
        System.out.println("Setting up RA Officer's KeyStore...");
        KeyStore officerKS = ksManager.createKeyStore();
        char[] password = "Officer@Password123".toCharArray();

        KeyPair officerKeyPair = ksManager.generateKeyPair();
        String officerDN = "CN=RA Officer, OU=PKI, O=Corp, C=US";
        X509Certificate officerCert = ksManager.generateSelfSignedCertificate(
            officerKeyPair, officerDN, 730); // 2 years validity

        ksManager.storeKeyEntry(officerKS, "officer-signing-cert",
            officerKeyPair.getPrivate(), password, new Certificate[] { officerCert });

        System.out.println("✓ Officer certificate stored");
        System.out.println("  Subject: " + officerCert.getSubjectDN());
        System.out.println("  Serial: " + officerCert.getSerialNumber() + "\n");

        // Simulate approval document
        String approvalDocument = "CERTIFICATE REQUEST APPROVAL\n" +
                                 "Request ID: REQ-2026-001\n" +
                                 "Applicant: John Doe\n" +
                                 "Certificate Type: Email Certificate\n" +
                                 "Approved by: RA Officer\n" +
                                 "Date: 2026-01-16\n";

        System.out.println("Document to sign:");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println(approvalDocument);
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");

        // Retrieve officer's private key for signing
        PrivateKey signingKey = ksManager.getPrivateKey(officerKS,
            "officer-signing-cert", password);

        // Sign the document
        java.security.Signature signature = java.security.Signature.getInstance(
            "SHA256withRSA", "BC");
        signature.initSign(signingKey);
        signature.update(approvalDocument.getBytes());
        byte[] signatureBytes = signature.sign();

        System.out.println("✓ Document signed successfully");
        System.out.println("  Signature algorithm: SHA256withRSA");
        System.out.println("  Signature length: " + signatureBytes.length + " bytes");
        System.out.println("  Signature (Base64): " +
            java.util.Base64.getEncoder().encodeToString(signatureBytes).substring(0, 60) + "...");

        // Verify signature
        java.security.Signature verifier = java.security.Signature.getInstance(
            "SHA256withRSA", "BC");
        verifier.initVerify(officerCert.getPublicKey());
        verifier.update(approvalDocument.getBytes());
        boolean verified = verifier.verify(signatureBytes);

        System.out.println("\n✓ Signature verification: " +
            (verified ? "SUCCESS ✓" : "FAILED ✗"));

        ksManager.clearPassword(password);
        System.out.println("\n✓ Example 5 completed successfully!\n");
    }

    /**
     * Example 6: Different KeyStore Types (PKCS#12, JKS, JCEKS, BKS)
     * Scenario: Demonstrating support for multiple KeyStore formats
     */
    public void example6_DifferentKeyStoreTypes() throws Exception {
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("Example 6: Working with Different KeyStore Types");
        System.out.println("═══════════════════════════════════════════════════════\n");

        // Generate test key pair and certificate
        System.out.println("Preparing test data...");
        KeyPair keyPair = ksManager.generateKeyPair();
        String subjectDN = "CN=Test User, E=test@corp.local, OU=IT, O=Corp, C=US";
        X509Certificate cert = ksManager.generateSelfSignedCertificate(keyPair, subjectDN, 365);
        char[] password = "Test@Password123".toCharArray();
        Certificate[] chain = new Certificate[] { cert };
        System.out.println("✓ Test certificate generated\n");

        // 1. PKCS#12 - Industry Standard (Default, Cross-platform)
        System.out.println("─────────────────────────────────────────────────────────");
        System.out.println("1. PKCS#12 KeyStore (.p12)");
        System.out.println("   Use case: General certificate storage, user keystores");
        System.out.println("   Provider: BC (Bouncy Castle)");
        System.out.println("─────────────────────────────────────────────────────────");

        KeyStore pkcs12KS = ksManager.createKeyStore(KeyStoreManager.KeyStoreType.PKCS12);
        ksManager.storeKeyEntry(pkcs12KS, "test-pkcs12", keyPair.getPrivate(), password, chain);
        ksManager.saveKeyStore(pkcs12KS, "test-keystore.p12", password);
        System.out.println("✓ Created PKCS#12 KeyStore: test-keystore.p12");
        System.out.println("  Type: " + pkcs12KS.getType());
        System.out.println("  Provider: " + pkcs12KS.getProvider().getName());
        System.out.println("  Entries: " + ksManager.listEntries(pkcs12KS).size());
        System.out.println();

        // 2. JKS - Java KeyStore (Legacy)
        System.out.println("─────────────────────────────────────────────────────────");
        System.out.println("2. JKS KeyStore (.jks)");
        System.out.println("   Use case: Legacy Java applications");
        System.out.println("   Provider: SUN");
        System.out.println("─────────────────────────────────────────────────────────");

        KeyStore jksKS = ksManager.createKeyStore(KeyStoreManager.KeyStoreType.JKS);
        ksManager.storeKeyEntry(jksKS, "test-jks", keyPair.getPrivate(), password, chain);
        ksManager.saveKeyStore(jksKS, "test-keystore.jks", password);
        System.out.println("✓ Created JKS KeyStore: test-keystore.jks");
        System.out.println("  Type: " + jksKS.getType());
        System.out.println("  Provider: " + jksKS.getProvider().getName());
        System.out.println("  Entries: " + ksManager.listEntries(jksKS).size());
        System.out.println();

        // 3. JCEKS - Java Cryptography Extension KeyStore
        System.out.println("─────────────────────────────────────────────────────────");
        System.out.println("3. JCEKS KeyStore (.jceks)");
        System.out.println("   Use case: Stronger key protection than JKS");
        System.out.println("   Provider: SunJCE");
        System.out.println("─────────────────────────────────────────────────────────");

        KeyStore jceksKS = ksManager.createKeyStore(KeyStoreManager.KeyStoreType.JCEKS);
        ksManager.storeKeyEntry(jceksKS, "test-jceks", keyPair.getPrivate(), password, chain);
        ksManager.saveKeyStore(jceksKS, "test-keystore.jceks", password);
        System.out.println("✓ Created JCEKS KeyStore: test-keystore.jceks");
        System.out.println("  Type: " + jceksKS.getType());
        System.out.println("  Provider: " + jceksKS.getProvider().getName());
        System.out.println("  Entries: " + ksManager.listEntries(jceksKS).size());
        System.out.println();

        // 4. BKS - Bouncy Castle KeyStore
        System.out.println("─────────────────────────────────────────────────────────");
        System.out.println("4. BKS KeyStore (.bks)");
        System.out.println("   Use case: Android applications, BC-specific features");
        System.out.println("   Provider: BC");
        System.out.println("─────────────────────────────────────────────────────────");

        KeyStore bksKS = ksManager.createKeyStore(KeyStoreManager.KeyStoreType.BKS);
        ksManager.storeKeyEntry(bksKS, "test-bks", keyPair.getPrivate(), password, chain);
        ksManager.saveKeyStore(bksKS, "test-keystore.bks", password);
        System.out.println("✓ Created BKS KeyStore: test-keystore.bks");
        System.out.println("  Type: " + bksKS.getType());
        System.out.println("  Provider: " + bksKS.getProvider().getName());
        System.out.println("  Entries: " + ksManager.listEntries(bksKS).size());
        System.out.println();

        // 5. Cross-format compatibility - Load and convert
        System.out.println("─────────────────────────────────────────────────────────");
        System.out.println("5. Cross-Format Conversion (JKS → PKCS#12)");
        System.out.println("   Use case: Migrating from legacy JKS to modern PKCS#12");
        System.out.println("─────────────────────────────────────────────────────────");

        // Load JKS
        KeyStore loadedJKS = ksManager.loadKeyStore("test-keystore.jks", password,
            KeyStoreManager.KeyStoreType.JKS);
        System.out.println("✓ Loaded JKS KeyStore");

        // Extract key and certificate
        PrivateKey jksKey = ksManager.getPrivateKey(loadedJKS, "test-jks", password);
        Certificate[] jksChain = ksManager.getCertificateChain(loadedJKS, "test-jks");
        System.out.println("✓ Extracted key and certificate from JKS");

        // Store in PKCS#12
        KeyStore convertedPKCS12 = ksManager.createKeyStore(KeyStoreManager.KeyStoreType.PKCS12);
        ksManager.storeKeyEntry(convertedPKCS12, "converted-from-jks", jksKey, password, jksChain);
        ksManager.saveKeyStore(convertedPKCS12, "converted-jks-to-pkcs12.p12", password);
        System.out.println("✓ Converted to PKCS#12: converted-jks-to-pkcs12.p12");
        System.out.println();

        // 6. PKCS#11 Information (HSM/Smart Card)
        System.out.println("─────────────────────────────────────────────────────────");
        System.out.println("6. PKCS#11 KeyStore (HSM/Smart Card)");
        System.out.println("   Use case: Hardware token, smart card, HSM integration");
        System.out.println("   Provider: SunPKCS11");
        System.out.println("─────────────────────────────────────────────────────────");
        System.out.println("ℹ PKCS#11 requires hardware device and configuration file.");
        System.out.println("  Example configuration (pkcs11.cfg):");
        System.out.println("    name = SmartCard");
        System.out.println("    library = /path/to/pkcs11library.so");
        System.out.println("    slot = 0");
        System.out.println();
        System.out.println("  Example usage:");
        System.out.println("    KeyStore ks = ksManager.loadPKCS11KeyStore(");
        System.out.println("        \"pkcs11.cfg\", \"PIN\".toCharArray());");
        System.out.println();

        // Summary
        System.out.println("─────────────────────────────────────────────────────────");
        System.out.println("Summary: KeyStore Type Comparison");
        System.out.println("─────────────────────────────────────────────────────────");
        System.out.println("PKCS#12 (.p12)  - ✓ Recommended (cross-platform, industry standard)");
        System.out.println("JKS (.jks)      - ⚠ Legacy (Java-specific, older format)");
        System.out.println("JCEKS (.jceks)  - ⚠ Legacy (stronger than JKS but Java-specific)");
        System.out.println("BKS (.bks)      - ✓ Android (Bouncy Castle specific)");
        System.out.println("PKCS#11         - ✓ HSM/Hardware (highest security, requires device)");
        System.out.println();

        // Clean up
        ksManager.clearPassword(password);
        System.out.println("✓ Example 6 completed successfully!\n");
    }

    /**
     * Main method to run all examples
     */
    public static void main(String[] args) {
        KeyStoreExample examples = new KeyStoreExample();

        try {
            System.out.println("\n");
            System.out.println("╔═══════════════════════════════════════════════════════╗");
            System.out.println("║  KeyStore Manager - Practical Examples               ║");
            System.out.println("║  RA Web Application Certificate Management           ║");
            System.out.println("╚═══════════════════════════════════════════════════════╝");
            System.out.println("\n");

            // Run all examples
            examples.example1_EndEntityEnrollment();
            examples.example2_AutoEnrollment();
            examples.example3_ExpirationMonitoring();
            examples.example4_ImportExport();
            examples.example5_DigitalSignature();
            examples.example6_DifferentKeyStoreTypes();

            System.out.println("╔═══════════════════════════════════════════════════════╗");
            System.out.println("║  ✓ All Examples Completed Successfully!              ║");
            System.out.println("╚═══════════════════════════════════════════════════════╝");

        } catch (Exception e) {
            System.err.println("✗ Error running examples: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
