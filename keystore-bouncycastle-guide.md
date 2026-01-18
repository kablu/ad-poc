# Java KeyStore with Bouncy Castle - Complete Guide

**Purpose**: Certificate and private key management for RA Web Application
**Date**: 2026-01-16
**Technology Stack**: Java 21, Bouncy Castle 1.70+, PKCS#12

---

## Table of Contents
1. [Overview](#overview)
2. [KeyStore Fundamentals](#keystore-fundamentals)
3. [setKeyEntry() Method Explained](#setkeyentry-method-explained)
4. [Complete Working Example](#complete-working-example)
5. [Integration with RA Application](#integration-with-ra-application)
6. [Security Best Practices](#security-best-practices)
7. [Common Use Cases](#common-use-cases)
8. [Troubleshooting](#troubleshooting)

---

## Overview

### What is Java KeyStore?

A Java KeyStore (JKS/PKCS12) is a repository for storing:
- **Private Keys**: Used for signing and decryption
- **Public Key Certificates**: Used for encryption and signature verification
- **Trusted Certificates**: CA certificates for trust chain validation

### Why Bouncy Castle?

Bouncy Castle provides:
- **PKCS#10 CSR** creation and parsing
- **X.509 certificate** generation and manipulation
- **PKCS#12 KeyStore** support (industry standard)
- **Cryptographic operations** (signing, verification, encryption)
- **ASN.1 encoding/decoding** for certificate structures

### RA Application Context

In the Registration Authority (RA) web application, KeyStore is used for:
1. **End Entity Certificates**: Store issued certificates with private keys
2. **Server Authentication**: HTTPS/TLS certificates for the RA server
3. **Certificate Inventory**: Track and manage user certificates
4. **Auto-Enrollment**: Generate and store certificates automatically
5. **PKCS#12 Distribution**: Package certificates for end users

---

## KeyStore Fundamentals

### KeyStore Types

| Type | Extension | Description | Use Case |
|------|-----------|-------------|----------|
| **PKCS12** | .p12, .pfx | Industry standard, cross-platform | **Recommended for RA** |
| **JKS** | .jks | Java-specific, legacy | Older Java applications |
| **JCEKS** | .jceks | Java Cryptography Extension | Strong key protection |
| **BKS** | .bks | Bouncy Castle KeyStore | Android, BC-specific |

**Recommendation**: Use **PKCS12** for maximum compatibility and security.

### Entry Types

1. **Private Key Entry** (`KeyStore.setKeyEntry()`)
   - Private key + certificate chain
   - Password-protected
   - Used for signing and decryption

2. **Trusted Certificate Entry** (`KeyStore.setCertificateEntry()`)
   - Public certificate only (no private key)
   - Used for trust anchors (CA certificates)

---

## setKeyEntry() Method Explained

### Method Signature

```java
public final void setKeyEntry(
    String alias,              // Unique identifier for this entry
    Key key,                   // Private key to store
    char[] password,           // Password to protect the private key
    Certificate[] chain        // Certificate chain (end-entity + intermediates + root)
) throws KeyStoreException
```

### Parameters Explained

#### 1. `alias` - String
- **Purpose**: Unique identifier for the key entry within the KeyStore
- **Requirements**:
  - Must be unique within the KeyStore
  - Case-sensitive
  - Typically uses meaningful names (e.g., "john-doe-email-cert", "ra-server-tls")
- **Best Practice**: Use a naming convention like `{username}-{purpose}-cert`

#### 2. `key` - Private Key
- **Type**: `java.security.PrivateKey` (typically `RSAPrivateKey`, `ECPrivateKey`)
- **Purpose**: The private key that pairs with the public key in the certificate
- **Security**: Encrypted in the KeyStore using the password parameter
- **Source**: Generated via `KeyPairGenerator` or imported from PKCS#8 format

#### 3. `password` - char[]
- **Purpose**: Protects the private key within the KeyStore
- **Important**: Use `char[]` not `String` (for security - can be cleared from memory)
- **Requirement**: Can be different from KeyStore password (allows per-entry protection)
- **Best Practice**: Use strong, random passwords; clear array after use

#### 4. `chain` - Certificate[]
- **Purpose**: Certificate chain from end-entity to root CA
- **Order**: MUST be ordered: [end-entity, intermediate CA(s), root CA]
- **Minimum**: At least one certificate (the end-entity certificate)
- **Validation**: KeyStore validates that chain is properly ordered and signed

### Certificate Chain Example

```
chain[0] = End-Entity Certificate (user/server certificate)
           Subject: CN=John Doe, E=john@corp.local
           Issuer:  CN=Intermediate CA

chain[1] = Intermediate CA Certificate
           Subject: CN=Intermediate CA
           Issuer:  CN=Root CA

chain[2] = Root CA Certificate (optional - trust anchor)
           Subject: CN=Root CA
           Issuer:  CN=Root CA (self-signed)
```

### setKeyEntry() vs setCertificateEntry()

| Feature | setKeyEntry() | setCertificateEntry() |
|---------|---------------|----------------------|
| **Private Key** | ✓ YES | ✗ NO |
| **Certificate** | ✓ YES (chain) | ✓ YES (single) |
| **Password** | ✓ Required | ✗ Not used |
| **Use Case** | Certificates you own | Trusted CA certs |
| **Signing** | ✓ Can sign | ✗ Cannot sign |
| **Entry Type** | `isKeyEntry()` | `isCertificateEntry()` |

---

## Complete Working Example

### Structured Class Implementation

For production use, we've created a complete `KeyStoreManager` class with organized methods:

**Location**: `src/main/java/com/corp/ra/keystore/KeyStoreManager.java`

**Key Features**:
- ✅ Comprehensive KeyStore operations (create, load, save)
- ✅ Key pair and certificate generation
- ✅ PKCS#10 CSR creation and validation
- ✅ Private key storage using `setKeyEntry()`
- ✅ Certificate retrieval and management
- ✅ Entry listing and deletion
- ✅ Security utilities (password generation, expiration checks)

**Example Usage**: `src/main/java/com/corp/ra/keystore/KeyStoreExample.java`

### Prerequisites

#### Maven Dependencies (pom.xml)

```xml
<dependencies>
    <!-- Bouncy Castle Provider -->
    <dependency>
        <groupId>org.bouncycastle</groupId>
        <artifactId>bcprov-jdk18on</artifactId>
        <version>1.78</version>
    </dependency>

    <!-- Bouncy Castle PKIX (for certificates) -->
    <dependency>
        <groupId>org.bouncycastle</groupId>
        <artifactId>bcpkix-jdk18on</artifactId>
        <version>1.78</version>
    </dependency>
</dependencies>
```

#### Register Bouncy Castle Provider

```java
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import java.security.Security;

static {
    Security.addProvider(new BouncyCastleProvider());
}
```

### Step-by-Step Example

#### Step 1: Generate Key Pair

```java
import java.security.*;

public KeyPair generateKeyPair() throws NoSuchAlgorithmException {
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA",
        new BouncyCastleProvider());
    keyGen.initialize(2048, new SecureRandom());
    return keyGen.generateKeyPair();
}
```

#### Step 2: Create PKCS#10 CSR

```java
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;

public PKCS10CertificationRequest createCSR(KeyPair keyPair, String subjectDN)
        throws Exception {
    X500Name subject = new X500Name(subjectDN);

    SubjectPublicKeyInfo publicKeyInfo = SubjectPublicKeyInfo.getInstance(
        keyPair.getPublic().getEncoded());

    PKCS10CertificationRequestBuilder csrBuilder =
        new PKCS10CertificationRequestBuilder(subject, publicKeyInfo);

    // Sign CSR with private key (Proof of Possession)
    ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSA")
        .setProvider("BC")
        .build(keyPair.getPrivate());

    return csrBuilder.build(signer);
}
```

#### Step 3: Generate Self-Signed Certificate

```java
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import java.security.cert.X509Certificate;
import java.math.BigInteger;
import java.util.Date;

public X509Certificate generateSelfSignedCert(KeyPair keyPair, String subjectDN)
        throws Exception {

    long now = System.currentTimeMillis();
    Date notBefore = new Date(now);
    Date notAfter = new Date(now + 365L * 24 * 60 * 60 * 1000); // 1 year

    X500Name issuer = new X500Name(subjectDN);
    X500Name subject = new X500Name(subjectDN);
    BigInteger serialNumber = BigInteger.valueOf(now);

    SubjectPublicKeyInfo publicKeyInfo = SubjectPublicKeyInfo.getInstance(
        keyPair.getPublic().getEncoded());

    X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(
        issuer, serialNumber, notBefore, notAfter, subject, publicKeyInfo);

    ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSA")
        .setProvider("BC")
        .build(keyPair.getPrivate());

    X509CertificateHolder certHolder = certBuilder.build(signer);

    return new JcaX509CertificateConverter()
        .setProvider("BC")
        .getCertificate(certHolder);
}
```

#### Step 4: Create and Use KeyStore

```java
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.io.*;

public void demonstrateKeyStore() throws Exception {
    // 1. Generate key pair
    KeyPair keyPair = generateKeyPair();

    // 2. Generate certificate
    String subjectDN = "CN=John Doe, E=john@corp.local, OU=IT, O=Corp, C=US";
    X509Certificate certificate = generateSelfSignedCert(keyPair, subjectDN);

    // 3. Create PKCS12 KeyStore
    KeyStore keyStore = KeyStore.getInstance("PKCS12", "BC");
    keyStore.load(null, null); // Initialize empty keystore

    // 4. Store private key with certificate using setKeyEntry()
    String alias = "john-doe-cert";
    char[] password = "SecurePassword123!".toCharArray();
    Certificate[] chain = new Certificate[] { certificate };

    // *** CRITICAL METHOD: setKeyEntry() ***
    keyStore.setKeyEntry(alias, keyPair.getPrivate(), password, chain);

    // 5. Save KeyStore to file
    try (FileOutputStream fos = new FileOutputStream("user-keystore.p12")) {
        keyStore.store(fos, password);
    }

    System.out.println("✓ KeyStore created and saved: user-keystore.p12");
    System.out.println("✓ Private key and certificate stored under alias: " + alias);
}
```

#### Step 5: Load and Retrieve from KeyStore

```java
public void loadAndRetrieveFromKeyStore() throws Exception {
    String keystorePath = "user-keystore.p12";
    char[] password = "SecurePassword123!".toCharArray();
    String alias = "john-doe-cert";

    // 1. Load KeyStore from file
    KeyStore keyStore = KeyStore.getInstance("PKCS12", "BC");
    try (FileInputStream fis = new FileInputStream(keystorePath)) {
        keyStore.load(fis, password);
    }

    // 2. Retrieve private key
    Key privateKey = keyStore.getKey(alias, password);
    System.out.println("✓ Private Key Algorithm: " + privateKey.getAlgorithm());

    // 3. Retrieve certificate chain
    Certificate[] chain = keyStore.getCertificateChain(alias);
    System.out.println("✓ Certificate Chain Length: " + chain.length);

    // 4. Display certificate details
    if (chain[0] instanceof X509Certificate) {
        X509Certificate cert = (X509Certificate) chain[0];
        System.out.println("  Subject: " + cert.getSubjectDN());
        System.out.println("  Issuer: " + cert.getIssuerDN());
        System.out.println("  Serial: " + cert.getSerialNumber());
        System.out.println("  Valid From: " + cert.getNotBefore());
        System.out.println("  Valid Until: " + cert.getNotAfter());
    }
}
```

---

## Integration with RA Application

### Use Case 1: End Entity Certificate Enrollment

**Scenario**: End entity submits CSR, RA issues certificate, stores in KeyStore

```java
@Service
public class CertificateEnrollmentService {

    @Autowired
    private CertificateAuthorityService caService;

    /**
     * Process CSR submission and store issued certificate
     */
    public String enrollCertificate(String username, PKCS10CertificationRequest csr,
                                   PrivateKey privateKey, char[] keystorePassword)
            throws Exception {

        // 1. Validate CSR (done by RA)
        if (!validateCSR(csr)) {
            throw new InvalidCSRException("CSR validation failed");
        }

        // 2. Submit CSR to CA for signing
        X509Certificate issuedCertificate = caService.signCSR(csr);

        // 3. Retrieve CA certificate chain
        Certificate[] caChain = caService.getCertificateChain();

        // 4. Build complete certificate chain
        Certificate[] fullChain = new Certificate[caChain.length + 1];
        fullChain[0] = issuedCertificate; // End-entity cert
        System.arraycopy(caChain, 0, fullChain, 1, caChain.length); // CA chain

        // 5. Store in user's KeyStore
        String keystorePath = getUserKeystorePath(username);
        KeyStore userKeyStore = loadOrCreateKeyStore(keystorePath, keystorePassword);

        String alias = username + "-" + System.currentTimeMillis();

        // *** Store certificate with private key ***
        userKeyStore.setKeyEntry(alias, privateKey, keystorePassword, fullChain);

        // 6. Save KeyStore
        saveKeyStore(userKeyStore, keystorePath, keystorePassword);

        // 7. Audit log
        auditLog(username, "CERTIFICATE_ENROLLED", alias);

        return alias;
    }
}
```

### Use Case 2: Auto-Enrollment (Server-Side Key Generation)

**Scenario**: RA generates key pair and certificate for user automatically

```java
@Service
public class AutoEnrollmentService {

    /**
     * Auto-enroll certificate for user based on AD group membership
     */
    public CertificateInfo autoEnroll(LdapUser user, CertificateTemplate template)
            throws Exception {

        // 1. Generate key pair on server (for auto-enrollment)
        KeyPair keyPair = generateKeyPair();

        // 2. Build subject DN from AD attributes
        String subjectDN = buildSubjectDN(user, template);

        // 3. Create CSR
        PKCS10CertificationRequest csr = createCSR(keyPair, subjectDN);

        // 4. Submit to CA (auto-approved for auto-enrollment templates)
        X509Certificate certificate = caService.signCSR(csr);

        // 5. Store in centralized KeyStore
        KeyStore raKeyStore = loadRAKeyStore();
        String alias = user.getUsername() + "-autoenroll-" + template.getName();
        char[] entryPassword = generateSecurePassword();

        raKeyStore.setKeyEntry(alias, keyPair.getPrivate(), entryPassword,
            new Certificate[] { certificate });

        saveRAKeyStore(raKeyStore);

        // 6. Generate PKCS#12 for user download
        String p12Path = generateUserPKCS12(keyPair, certificate, user.getUsername());

        // 7. Notify user
        emailService.sendCertificateAvailable(user.getEmail(), p12Path);

        return new CertificateInfo(alias, certificate, p12Path);
    }
}
```

### Use Case 3: Certificate Retrieval for Signing

**Scenario**: RA officer signs certificate request approval

```java
@Service
public class CertificateSigningService {

    /**
     * Sign approval document using RA officer's certificate
     */
    public byte[] signApproval(String officerUsername, String requestId,
                              byte[] approvalDocument) throws Exception {

        // 1. Load officer's KeyStore
        String keystorePath = getOfficerKeystorePath(officerUsername);
        char[] password = getOfficerKeystorePassword(officerUsername);

        KeyStore keyStore = loadKeyStore(keystorePath, password);

        // 2. Retrieve signing certificate and private key
        String signingAlias = officerUsername + "-signing-cert";
        PrivateKey privateKey = (PrivateKey) keyStore.getKey(signingAlias, password);
        X509Certificate certificate = (X509Certificate) keyStore.getCertificate(signingAlias);

        // 3. Sign document
        Signature signature = Signature.getInstance("SHA256withRSA", "BC");
        signature.initSign(privateKey);
        signature.update(approvalDocument);
        byte[] signatureBytes = signature.sign();

        // 4. Create signed approval record
        SignedApproval approval = new SignedApproval(
            requestId,
            officerUsername,
            approvalDocument,
            signatureBytes,
            certificate
        );

        return approval.toBytes();
    }
}
```

### Use Case 4: Bulk Certificate Export

**Scenario**: Export all user certificates for backup/migration

```java
@Service
public class CertificateExportService {

    /**
     * Export all certificates from KeyStore to individual PEM files
     */
    public int exportAllCertificates(String outputDirectory) throws Exception {
        KeyStore keyStore = loadRAKeyStore();
        char[] password = getRAKeystorePassword();

        int exportCount = 0;
        Enumeration<String> aliases = keyStore.aliases();

        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();

            if (keyStore.isKeyEntry(alias)) {
                // Retrieve certificate chain
                Certificate[] chain = keyStore.getCertificateChain(alias);

                // Export end-entity certificate
                X509Certificate cert = (X509Certificate) chain[0];
                String pemPath = outputDirectory + "/" + alias + ".pem";
                exportCertificateToPEM(cert, pemPath);

                // Export private key (encrypted)
                PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, password);
                String keyPath = outputDirectory + "/" + alias + ".key";
                exportPrivateKeyToPEM(privateKey, keyPath, password);

                exportCount++;
            }
        }

        return exportCount;
    }

    private void exportCertificateToPEM(X509Certificate cert, String path)
            throws Exception {
        try (FileWriter writer = new FileWriter(path);
             org.bouncycastle.openssl.jcajce.JcaPEMWriter pemWriter =
                 new org.bouncycastle.openssl.jcajce.JcaPEMWriter(writer)) {
            pemWriter.writeObject(cert);
        }
    }
}
```

---

## Security Best Practices

### 1. Password Management

```java
// ✓ GOOD: Use char[] for passwords
char[] password = getPasswordFromSecureSource();
try {
    keyStore.load(inputStream, password);
} finally {
    // Clear password from memory
    Arrays.fill(password, '\0');
}

// ✗ BAD: Never use String for passwords
String password = "myPassword"; // Remains in String pool
```

### 2. KeyStore Protection

```java
// Strong KeyStore password
char[] keystorePassword = generateStrongPassword(32); // 32 characters

// Different password for each private key entry
char[] keyPassword1 = generateStrongPassword(32);
char[] keyPassword2 = generateStrongPassword(32);

keyStore.setKeyEntry("user1-cert", key1, keyPassword1, chain1);
keyStore.setKeyEntry("user2-cert", key2, keyPassword2, chain2);
```

### 3. File System Security

```java
import java.nio.file.*;
import java.nio.file.attribute.*;

// Set restrictive permissions on KeyStore file (POSIX)
Path keystorePath = Paths.get("ra-keystore.p12");
Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rw-------");
Files.setPosixFilePermissions(keystorePath, perms);

// Or use Java NIO for more control
FileAttribute<Set<PosixFilePermission>> attr =
    PosixFilePermissions.asFileAttribute(perms);
Files.createFile(keystorePath, attr);
```

### 4. HSM Integration (Production)

```java
// For production, use Hardware Security Module (HSM)
// Example with PKCS#11
Provider pkcs11Provider = Security.getProvider("SunPKCS11");
KeyStore hsmKeyStore = KeyStore.getInstance("PKCS11", pkcs11Provider);
hsmKeyStore.load(null, pin); // PIN protected

// Private keys never leave HSM
hsmKeyStore.setKeyEntry(alias, null, null, chain); // Key reference only
```

### 5. Audit Logging

```java
@Aspect
@Component
public class KeyStoreAuditAspect {

    @Around("execution(* KeyStore.setKeyEntry(..))")
    public Object auditSetKeyEntry(ProceedingJoinPoint joinPoint) throws Throwable {
        String alias = (String) joinPoint.getArgs()[0];
        String username = SecurityContextHolder.getContext()
            .getAuthentication().getName();

        auditLog.log(AuditEvent.builder()
            .action("KEYSTORE_SET_KEY_ENTRY")
            .username(username)
            .alias(alias)
            .timestamp(Instant.now())
            .ipAddress(getClientIP())
            .build());

        return joinPoint.proceed();
    }
}
```

---

## Common Use Cases

### 1. Import Existing PKCS#12 File

```java
public void importPKCS12(String p12FilePath, char[] p12Password,
                        KeyStore targetKeyStore, char[] targetPassword)
        throws Exception {

    // Load source PKCS#12
    KeyStore sourceKeyStore = KeyStore.getInstance("PKCS12", "BC");
    try (FileInputStream fis = new FileInputStream(p12FilePath)) {
        sourceKeyStore.load(fis, p12Password);
    }

    // Copy all entries to target KeyStore
    Enumeration<String> aliases = sourceKeyStore.aliases();
    while (aliases.hasMoreElements()) {
        String alias = aliases.nextElement();

        if (sourceKeyStore.isKeyEntry(alias)) {
            Key key = sourceKeyStore.getKey(alias, p12Password);
            Certificate[] chain = sourceKeyStore.getCertificateChain(alias);

            targetKeyStore.setKeyEntry(alias, key, targetPassword, chain);
        }
    }
}
```

### 2. Convert JKS to PKCS#12

```java
public void convertJKStoPKCS12(String jksPath, String p12Path, char[] password)
        throws Exception {

    // Load JKS
    KeyStore jksKeyStore = KeyStore.getInstance("JKS");
    try (FileInputStream fis = new FileInputStream(jksPath)) {
        jksKeyStore.load(fis, password);
    }

    // Create PKCS#12
    KeyStore p12KeyStore = KeyStore.getInstance("PKCS12", "BC");
    p12KeyStore.load(null, null);

    // Copy all entries
    Enumeration<String> aliases = jksKeyStore.aliases();
    while (aliases.hasMoreElements()) {
        String alias = aliases.nextElement();

        if (jksKeyStore.isKeyEntry(alias)) {
            Key key = jksKeyStore.getKey(alias, password);
            Certificate[] chain = jksKeyStore.getCertificateChain(alias);
            p12KeyStore.setKeyEntry(alias, key, password, chain);
        } else if (jksKeyStore.isCertificateEntry(alias)) {
            Certificate cert = jksKeyStore.getCertificate(alias);
            p12KeyStore.setCertificateEntry(alias, cert);
        }
    }

    // Save PKCS#12
    try (FileOutputStream fos = new FileOutputStream(p12Path)) {
        p12KeyStore.store(fos, password);
    }
}
```

### 3. Generate User-Specific PKCS#12

```java
public File generateUserPKCS12(String username, PrivateKey privateKey,
                              X509Certificate certificate) throws Exception {

    KeyStore userKeyStore = KeyStore.getInstance("PKCS12", "BC");
    userKeyStore.load(null, null);

    // Generate random password for PKCS#12
    char[] p12Password = generateSecurePassword(16);

    // Store certificate with private key
    userKeyStore.setKeyEntry(
        username,
        privateKey,
        p12Password,
        new Certificate[] { certificate }
    );

    // Save to temporary file
    File p12File = File.createTempFile(username + "-", ".p12");
    try (FileOutputStream fos = new FileOutputStream(p12File)) {
        userKeyStore.store(fos, p12Password);
    }

    // Send password to user via secure channel (email, SMS)
    notifyUserOfPassword(username, p12Password);

    // Clear password
    Arrays.fill(p12Password, '\0');

    return p12File;
}
```

### 4. List and Filter Certificates

```java
public List<CertificateInfo> listActiveCertificates(KeyStore keyStore, char[] password)
        throws Exception {

    List<CertificateInfo> certificates = new ArrayList<>();
    Enumeration<String> aliases = keyStore.aliases();

    while (aliases.hasMoreElements()) {
        String alias = aliases.nextElement();

        if (keyStore.isKeyEntry(alias)) {
            Certificate[] chain = keyStore.getCertificateChain(alias);
            if (chain.length > 0 && chain[0] instanceof X509Certificate) {
                X509Certificate cert = (X509Certificate) chain[0];

                // Filter: Only include valid (not expired) certificates
                Date now = new Date();
                if (cert.getNotAfter().after(now) && cert.getNotBefore().before(now)) {
                    certificates.add(new CertificateInfo(
                        alias,
                        cert.getSubjectDN().toString(),
                        cert.getSerialNumber(),
                        cert.getNotBefore(),
                        cert.getNotAfter()
                    ));
                }
            }
        }
    }

    return certificates;
}
```

---

## Troubleshooting

### Error 1: "Invalid keystore format"

**Problem**: Trying to load PKCS#12 as JKS or vice versa

```java
// ✗ WRONG
KeyStore keyStore = KeyStore.getInstance("JKS");
keyStore.load(new FileInputStream("file.p12"), password); // FAILS

// ✓ CORRECT
KeyStore keyStore = KeyStore.getInstance("PKCS12", "BC");
keyStore.load(new FileInputStream("file.p12"), password);
```

### Error 2: "Cannot find alias"

**Problem**: Alias doesn't exist or case-sensitive mismatch

```java
// Debug: List all aliases
Enumeration<String> aliases = keyStore.aliases();
System.out.println("Available aliases:");
while (aliases.hasMoreElements()) {
    System.out.println("  - " + aliases.nextElement());
}

// Check if alias exists before retrieval
String alias = "user-cert";
if (keyStore.containsAlias(alias)) {
    Key key = keyStore.getKey(alias, password);
} else {
    throw new AliasNotFoundException("Alias not found: " + alias);
}
```

### Error 3: "Incorrect password"

**Problem**: Wrong password for KeyStore or key entry

```java
// KeyStore password vs entry password are different
char[] keystorePassword = "keystorePass".toCharArray();
char[] entryPassword = "entryPass".toCharArray();

// Load KeyStore with KeyStore password
keyStore.load(inputStream, keystorePassword);

// Retrieve key with entry password (may be different)
Key key = keyStore.getKey(alias, entryPassword);
```

### Error 4: "Certificate chain not valid"

**Problem**: Certificate chain is not properly ordered

```java
// ✗ WRONG ORDER
Certificate[] chain = new Certificate[] {
    intermediateCert,  // Should be end-entity first
    endEntityCert,
    rootCert
};

// ✓ CORRECT ORDER
Certificate[] chain = new Certificate[] {
    endEntityCert,     // End-entity certificate first
    intermediateCert,  // Intermediate CA
    rootCert          // Root CA last
};

keyStore.setKeyEntry(alias, privateKey, password, chain);
```

### Error 5: "Provider not found"

**Problem**: Bouncy Castle provider not registered

```java
// Register Bouncy Castle provider
import org.bouncycastle.jce.provider.BouncyCastleProvider;

static {
    // Add BC provider if not already added
    if (Security.getProvider("BC") == null) {
        Security.addProvider(new BouncyCastleProvider());
    }
}

// Or specify provider explicitly
KeyStore keyStore = KeyStore.getInstance("PKCS12", "BC");
```

---

## Complete Maven Project Setup

### pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.corp.ra</groupId>
    <artifactId>keystore-poc</artifactId>
    <version>1.0.0</version>

    <properties>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <bouncycastle.version>1.78</bouncycastle.version>
    </properties>

    <dependencies>
        <!-- Bouncy Castle Provider -->
        <dependency>
            <groupId>org.bouncycastle</groupId>
            <artifactId>bcprov-jdk18on</artifactId>
            <version>${bouncycastle.version}</version>
        </dependency>

        <!-- Bouncy Castle PKIX (X.509 certificates) -->
        <dependency>
            <groupId>org.bouncycastle</groupId>
            <artifactId>bcpkix-jdk18on</artifactId>
            <version>${bouncycastle.version}</version>
        </dependency>

        <!-- Bouncy Castle Utilities -->
        <dependency>
            <groupId>org.bouncycastle</groupId>
            <artifactId>bcutil-jdk18on</artifactId>
            <version>${bouncycastle.version}</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
            </plugin>
        </plugins>
    </build>
</project>
```

### Run the POC

```bash
# Compile
mvn clean compile

# Run
mvn exec:java -Dexec.mainClass="KeyStorePOC"

# Or compile and run manually
javac -cp "target/classes:lib/*" KeyStorePOC.java
java -cp "target/classes:lib/*" KeyStorePOC
```

---

## Summary

### Key Takeaways

1. **KeyStore.setKeyEntry()** is the primary method for storing private keys with certificate chains
2. **PKCS#12** is the recommended KeyStore type for cross-platform compatibility
3. **Bouncy Castle** provides comprehensive cryptographic operations for PKI
4. **Certificate chain order** must be: end-entity → intermediate(s) → root
5. **Password management** is critical - use char[], never String
6. **Per-entry passwords** provide defense-in-depth for KeyStore protection

### Methods Reference

| Method | Purpose | Returns |
|--------|---------|---------|
| `KeyStore.getInstance(type, provider)` | Create KeyStore | KeyStore |
| `KeyStore.load(stream, password)` | Load/initialize KeyStore | void |
| `KeyStore.setKeyEntry(alias, key, pwd, chain)` | Store private key + certs | void |
| `KeyStore.getKey(alias, password)` | Retrieve private key | Key |
| `KeyStore.getCertificateChain(alias)` | Retrieve cert chain | Certificate[] |
| `KeyStore.aliases()` | List all aliases | Enumeration |
| `KeyStore.store(stream, password)` | Save KeyStore to file | void |

### Next Steps

1. Review the complete POC code: `KeyStorePOC.java`
2. Run the POC to see KeyStore operations in action
3. Integrate KeyStore management into RA application
4. Implement secure password management and HSM integration for production
5. Add audit logging for all KeyStore operations

---

## Practical Examples with KeyStoreManager

### Running the Examples

```bash
# Compile the classes
javac -cp "lib/*" src/main/java/com/corp/ra/keystore/*.java

# Run all examples
java -cp "src/main/java:lib/*" com.corp.ra.keystore.KeyStoreExample
```

### Example Output

```
╔═══════════════════════════════════════════════════════╗
║  KeyStore Manager - Practical Examples               ║
║  RA Web Application Certificate Management           ║
╚═══════════════════════════════════════════════════════╝

═══════════════════════════════════════════════════════
Example 1: End Entity Certificate Enrollment
═══════════════════════════════════════════════════════

Step 1: Generating key pair for end entity...
✓ Key pair generated

Step 2: Creating PKCS#10 CSR...
✓ CSR created and signature verified: true

Step 3: RA issuing certificate...
✓ Certificate issued
  Serial: 1737062400000
  Valid until: Thu Jan 15 10:00:00 UTC 2027

Step 4: Creating user's KeyStore...
✓ KeyStore created

Step 5: Storing certificate with private key...
✓ Certificate stored with alias: john-doe-email-cert

Step 6: Saving KeyStore to file...
✓ KeyStore saved: john-doe-keystore.p12

Step 7: Verifying stored data...
✓ Retrieved private key: RSA
✓ Retrieved certificate chain length: 1
✓ Certificate subject: CN=John Doe,E=john.doe@corp.local,OU=Engineering,O=Corp,C=US

✓ Example 1 completed successfully!
```

### Example 1: End Entity Certificate Enrollment

**Use Case**: Standard certificate enrollment workflow where an end entity submits a CSR and receives a certificate.

```java
KeyStoreManager ksManager = new KeyStoreManager();

// Generate key pair
KeyPair keyPair = ksManager.generateKeyPair();

// Create CSR
String subjectDN = "CN=John Doe, E=john.doe@corp.local, OU=Engineering, O=Corp, C=US";
PKCS10CertificationRequest csr = ksManager.createCSR(keyPair, subjectDN);

// Validate CSR
boolean isValid = ksManager.verifyCSRSignature(csr);

// Issue certificate (from CA)
X509Certificate certificate = ksManager.generateSelfSignedCertificate(
    keyPair, subjectDN, 365);

// Create and store in KeyStore
KeyStore keyStore = ksManager.createKeyStore();
char[] password = "SecurePassword123".toCharArray();

ksManager.storeKeyEntry(
    keyStore,
    "john-doe-email-cert",
    keyPair.getPrivate(),
    password,
    new Certificate[] { certificate }
);

// Save to file
ksManager.saveKeyStore(keyStore, "john-doe.p12", password);
```

**Output Files**:
- `john-doe-keystore.p12` - User's PKCS#12 file with private key and certificate

### Example 2: Auto-Enrollment for Multiple Users

**Use Case**: RA automatically generates and stores certificates for multiple AD users.

```java
KeyStoreManager ksManager = new KeyStoreManager();
KeyStore raKeyStore = ksManager.createKeyStore();

String[] adUsers = {
    "CN=Alice Smith, E=alice@corp.local, OU=Engineering",
    "CN=Bob Admin, E=bob.admin@corp.local, OU=IT",
    "CN=Charlie Dev, E=charlie@corp.local, OU=Engineering"
};

for (int i = 0; i < adUsers.length; i++) {
    // Generate key pair and certificate
    KeyPair kp = ksManager.generateKeyPair();
    X509Certificate cert = ksManager.generateSelfSignedCertificate(
        kp, adUsers[i], 365);

    // Store in centralized KeyStore
    String alias = "autoenroll-user-" + (i + 1);
    char[] entryPassword = ksManager.generateSecurePassword(16);

    ksManager.storeKeyEntry(raKeyStore, alias, kp.getPrivate(),
        entryPassword, new Certificate[] { cert });
}

// Save centralized KeyStore
ksManager.saveKeyStore(raKeyStore, "ra-autoenroll.p12", raPassword);
```

**Output Files**:
- `ra-autoenroll-keystore.p12` - Centralized KeyStore with all auto-enrolled certificates

### Example 3: Certificate Expiration Monitoring

**Use Case**: Monitor certificates in KeyStore and identify those expiring soon.

```java
KeyStoreManager ksManager = new KeyStoreManager();
KeyStore keyStore = ksManager.loadKeyStore("certificates.p12", password);

List<KeyStoreManager.KeyStoreEntry> entries = ksManager.listEntries(keyStore);

for (KeyStoreManager.KeyStoreEntry entry : entries) {
    Certificate[] chain = ksManager.getCertificateChain(keyStore, entry.getAlias());
    if (chain[0] instanceof X509Certificate) {
        X509Certificate cert = (X509Certificate) chain[0];
        long daysRemaining = ksManager.getDaysUntilExpiration(cert);

        if (daysRemaining <= 30) {
            System.out.println("⚠ WARNING: Certificate expiring soon!");
            System.out.println("  Alias: " + entry.getAlias());
            System.out.println("  Days remaining: " + daysRemaining);
            System.out.println("  Subject: " + cert.getSubjectDN());
        }
    }
}
```

**Sample Output**:
```
Certificate Expiration Report:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Alias: cert-365-days
  Subject: CN=Long Valid Cert,O=Corp
  Days remaining: 365
  Status: ✓ VALID

Alias: cert-15-days
  Subject: CN=Expiring Soon Cert,O=Corp
  Days remaining: 15
  Status: ⚠ WARNING (≤30 days)
  Action: Renew certificate before Thu Jan 30 10:00:00 UTC 2026

Alias: cert-5-days
  Subject: CN=Critical Cert,O=Corp
  Days remaining: 5
  Status: ⚠ CRITICAL (≤7 days)
  Action: Renew certificate before Mon Jan 20 10:00:00 UTC 2026
```

### Example 4: Certificate Import/Export

**Use Case**: Import certificates from one KeyStore to another (e.g., migration, backup).

```java
KeyStoreManager ksManager = new KeyStoreManager();

// Load source KeyStore
KeyStore sourceKS = ksManager.loadKeyStore("source.p12", sourcePassword);

// Create destination KeyStore
KeyStore destKS = ksManager.createKeyStore();

// Import all entries
List<KeyStoreManager.KeyStoreEntry> entries = ksManager.listEntries(sourceKS);
for (KeyStoreManager.KeyStoreEntry entry : entries) {
    if (entry.getType() == KeyStoreManager.KeyStoreEntryType.PRIVATE_KEY) {
        // Retrieve from source
        PrivateKey key = ksManager.getPrivateKey(sourceKS, entry.getAlias(),
            sourcePassword);
        Certificate[] chain = ksManager.getCertificateChain(sourceKS,
            entry.getAlias());

        // Store in destination
        String newAlias = "imported-" + entry.getAlias();
        ksManager.storeKeyEntry(destKS, newAlias, key, destPassword, chain);
    }
}

// Save destination
ksManager.saveKeyStore(destKS, "destination.p12", destPassword);
```

**Output Files**:
- `source-keystore.p12` - Original KeyStore
- `destination-keystore.p12` - New KeyStore with imported certificates

### Example 5: RA Officer Digital Signature

**Use Case**: RA Officer signs certificate approval documents using their certificate.

```java
KeyStoreManager ksManager = new KeyStoreManager();

// Load officer's KeyStore
KeyStore officerKS = ksManager.loadKeyStore("officer.p12", password);

// Retrieve signing certificate and private key
PrivateKey signingKey = ksManager.getPrivateKey(officerKS,
    "officer-signing-cert", password);
Certificate[] chain = ksManager.getCertificateChain(officerKS,
    "officer-signing-cert");
X509Certificate officerCert = (X509Certificate) chain[0];

// Document to sign
String approvalDocument = "CERTIFICATE REQUEST APPROVAL\n" +
                         "Request ID: REQ-2026-001\n" +
                         "Applicant: John Doe\n" +
                         "Approved by: RA Officer\n";

// Sign document
java.security.Signature signature = java.security.Signature.getInstance(
    "SHA256withRSA", "BC");
signature.initSign(signingKey);
signature.update(approvalDocument.getBytes());
byte[] signatureBytes = signature.sign();

// Verify signature
java.security.Signature verifier = java.security.Signature.getInstance(
    "SHA256withRSA", "BC");
verifier.initVerify(officerCert.getPublicKey());
verifier.update(approvalDocument.getBytes());
boolean verified = verifier.verify(signatureBytes);

System.out.println("Signature verified: " + verified); // true
```

**Sample Output**:
```
Document to sign:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
CERTIFICATE REQUEST APPROVAL
Request ID: REQ-2026-001
Applicant: John Doe
Certificate Type: Email Certificate
Approved by: RA Officer
Date: 2026-01-16
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

✓ Document signed successfully
  Signature algorithm: SHA256withRSA
  Signature length: 256 bytes
  Signature (Base64): MEUCIQDxY2k9...

✓ Signature verification: SUCCESS ✓
```

### Quick Start: Using KeyStoreManager in Your Project

#### 1. Add to your Spring Boot service:

```java
@Service
public class CertificateManagementService {

    private final KeyStoreManager ksManager = new KeyStoreManager();

    @Value("${ra.keystore.path}")
    private String keystorePath;

    @Value("${ra.keystore.password}")
    private String keystorePassword;

    public void enrollCertificate(String username, String email) throws Exception {
        // Load RA KeyStore
        KeyStore keyStore = ksManager.loadKeyStore(keystorePath,
            keystorePassword.toCharArray());

        // Generate key pair and certificate
        KeyPair keyPair = ksManager.generateKeyPair();
        String subjectDN = String.format("CN=%s, E=%s, O=Corp", username, email);
        X509Certificate cert = ksManager.generateSelfSignedCertificate(
            keyPair, subjectDN, 365);

        // Store certificate
        String alias = username + "-" + System.currentTimeMillis();
        ksManager.storeKeyEntry(keyStore, alias, keyPair.getPrivate(),
            keystorePassword.toCharArray(), new Certificate[] { cert });

        // Save KeyStore
        ksManager.saveKeyStore(keyStore, keystorePath,
            keystorePassword.toCharArray());
    }
}
```

#### 2. Add to application.yml:

```yaml
ra:
  keystore:
    path: /var/ra/keystore/ra-certificates.p12
    password: ${KEYSTORE_PASSWORD:changeme}
    type: PKCS12
```

---

**Document Status**: Complete
**Last Updated**: 2026-01-16
**Tested With**: Java 21, Bouncy Castle 1.78
**Repository**: D:\ecc-dev\jdk-21-poc\ra-web
