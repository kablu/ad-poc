# Java KeyStore with Bouncy Castle - POC Documentation

**Date**: 2026-01-16
**Purpose**: Certificate and Private Key Management for RA Web Application
**Technology**: Java 21, Bouncy Castle 1.78, PKCS#12

---

## 🔑 Supported KeyStore Types

This implementation supports **5 different KeyStore types** for various use cases:

| Type | Extension | Provider | Use Case |
|------|-----------|----------|----------|
| **PKCS#12** | .p12, .pfx | BC | ✓ **Recommended** - Industry standard, cross-platform |
| **PKCS#11** | N/A | SunPKCS11 | ✓ Hardware Security Modules (HSM), Smart Cards |
| **JKS** | .jks | SUN | ⚠ Legacy Java KeyStore (older Java apps) |
| **JCEKS** | .jceks | SunJCE | ⚠ Java Cryptography Extension (stronger than JKS) |
| **BKS** | .bks | BC | ✓ Android applications, BC-specific features |

### Key Features:
- ✅ **Provider Pattern**: Uses string constants instead of direct provider instantiation
- ✅ **Type Safety**: Enum-based KeyStore type selection
- ✅ **Cross-format Conversion**: Migrate between different KeyStore types
- ✅ **HSM Support**: PKCS#11 integration for hardware tokens and smart cards

### Example Usage:
```java
KeyStoreManager ksm = new KeyStoreManager();

// Create PKCS#12 (default, recommended)
KeyStore pkcs12 = ksm.createKeyStore(KeyStoreManager.KeyStoreType.PKCS12);

// Create JKS for legacy support
KeyStore jks = ksm.createKeyStore(KeyStoreManager.KeyStoreType.JKS);

// Load PKCS#11 for HSM
KeyStore hsm = ksm.loadPKCS11KeyStore("pkcs11.cfg", pin);

// Convert JKS to PKCS#12
KeyStore oldJKS = ksm.loadKeyStore("old.jks", password, KeyStoreManager.KeyStoreType.JKS);
PrivateKey key = ksm.getPrivateKey(oldJKS, "alias", password);
Certificate[] chain = ksm.getCertificateChain(oldJKS, "alias");

KeyStore newPKCS12 = ksm.createKeyStore(KeyStoreManager.KeyStoreType.PKCS12);
ksm.storeKeyEntry(newPKCS12, "alias", key, password, chain);
ksm.saveKeyStore(newPKCS12, "converted.p12", password);
```

---

## 📁 Project Structure

```
D:\ecc-dev\jdk-21-poc\ra-web\
│
├── src/main/java/com/corp/ra/keystore/
│   ├── KeyStoreManager.java      # Main KeyStore management class
│   └── KeyStoreExample.java      # 5 practical examples
│
├── KeyStorePOC.java               # Standalone POC (no dependencies on project structure)
├── keystore-bouncycastle-guide.md # Complete guide with documentation
└── KEYSTORE-README.md             # This file
```

---

## 🚀 Quick Start

### Option 1: Run Standalone POC

```bash
# Navigate to project directory
cd D:\ecc-dev\jdk-21-poc\ra-web

# Compile (requires Bouncy Castle JARs in classpath)
javac -cp "lib/*" KeyStorePOC.java

# Run
java -cp ".:lib/*" KeyStorePOC
```

### Option 2: Run Structured Examples

```bash
# Compile
javac -cp "lib/*" src/main/java/com/corp/ra/keystore/*.java

# Run all examples
java -cp "src/main/java:lib/*" com.corp.ra.keystore.KeyStoreExample
```

---

## 📚 Documentation Files

### 1. **KeyStorePOC.java** - Standalone Demonstration
- **Purpose**: Single-file POC demonstrating all KeyStore operations
- **Features**:
  - ✅ RSA key pair generation
  - ✅ PKCS#10 CSR creation and verification
  - ✅ Self-signed certificate generation
  - ✅ **KeyStore.setKeyEntry()** demonstration
  - ✅ KeyStore save/load operations
  - ✅ Multiple entry management
  - ✅ Integrity verification

**When to use**: Quick demonstration, learning, testing

### 2. **KeyStoreManager.java** - Production-Ready Class
- **Location**: `src/main/java/com/corp/ra/keystore/KeyStoreManager.java`
- **Purpose**: Reusable, well-structured class for production use
- **Key Methods**:
  ```java
  // KeyStore Operations
  KeyStore createKeyStore()
  KeyStore loadKeyStore(String path, char[] password)
  void saveKeyStore(KeyStore ks, String path, char[] password)

  // Key Pair & Certificates
  KeyPair generateKeyPair()
  PKCS10CertificationRequest createCSR(KeyPair kp, String subjectDN)
  boolean verifyCSRSignature(PKCS10CertificationRequest csr)
  X509Certificate generateSelfSignedCertificate(KeyPair kp, String subjectDN, int validityDays)

  // Entry Management (Core Methods)
  void storeKeyEntry(KeyStore ks, String alias, PrivateKey key, char[] password, Certificate[] chain)
  PrivateKey getPrivateKey(KeyStore ks, String alias, char[] password)
  Certificate[] getCertificateChain(KeyStore ks, String alias)
  List<KeyStoreEntry> listEntries(KeyStore ks)
  void deleteEntry(KeyStore ks, String alias)

  // Utilities
  char[] generateSecurePassword(int length)
  void clearPassword(char[] password)
  boolean isCertificateExpired(X509Certificate cert)
  long getDaysUntilExpiration(X509Certificate cert)
  ```

**When to use**: Integration into RA application, production deployment

### 3. **KeyStoreExample.java** - 6 Practical Examples
- **Location**: `src/main/java/com/corp/ra/keystore/KeyStoreExample.java`
- **Examples**:
  1. **End Entity Certificate Enrollment** - Standard CSR workflow
  2. **Auto-Enrollment for Multiple Users** - Batch certificate generation
  3. **Certificate Expiration Monitoring** - Identify expiring certificates
  4. **Certificate Import/Export** - Migration and backup
  5. **RA Officer Digital Signature** - Document signing with certificates
  6. **Different KeyStore Types** - PKCS#12, JKS, JCEKS, BKS, PKCS#11 support

**When to use**: Learning real-world use cases, testing integration patterns

### 4. **keystore-bouncycastle-guide.md** - Complete Documentation
- **Purpose**: Comprehensive guide covering all aspects
- **Contents**:
  - KeyStore fundamentals and types (PKCS#12, JKS, JCEKS, BKS)
  - **setKeyEntry()** method detailed explanation
  - Certificate chain ordering and validation
  - Step-by-step code examples
  - RA application integration scenarios
  - Security best practices
  - Troubleshooting guide
  - Maven project setup

**When to use**: Reference documentation, troubleshooting, best practices

---

## 🎯 Key Concepts Demonstrated

### 1. KeyStore.setKeyEntry() - The Core Method

```java
keyStore.setKeyEntry(
    "john-doe-cert",           // alias - unique identifier
    keyPair.getPrivate(),      // private key to store
    "password".toCharArray(),  // password to protect the key
    new Certificate[] { cert } // certificate chain
);
```

**What it does**:
- Stores private key with associated certificate chain
- Password-protects the private key
- Enables later retrieval for signing/decryption

**Use cases in RA application**:
- Store end entity certificates with private keys
- Store RA officer signing certificates
- Store server TLS certificates
- Manage certificate inventory

### 2. Certificate Chain Structure

```
chain[0] = End-Entity Certificate (user/server cert)
chain[1] = Intermediate CA Certificate
chain[2] = Root CA Certificate (optional)
```

**Order is critical**: End-entity cert MUST be first

### 3. PKCS#10 CSR Workflow

```
1. Generate key pair (2048-bit RSA)
2. Create CSR with subject DN
3. Sign CSR with private key (Proof of Possession)
4. Verify CSR signature (RA validation)
5. Submit to CA for signing
6. Store issued certificate with private key in KeyStore
```

---

## 📋 Use Cases in RA Application

### 1. End Entity Certificate Enrollment

```java
KeyStoreManager ksm = new KeyStoreManager();

// User generates key pair
KeyPair keyPair = ksm.generateKeyPair();

// User creates CSR
PKCS10CertificationRequest csr = ksm.createCSR(keyPair,
    "CN=John Doe, E=john@corp.local");

// RA validates CSR
boolean valid = ksm.verifyCSRSignature(csr);

// CA issues certificate
X509Certificate cert = caService.signCSR(csr);

// Store in user's KeyStore
KeyStore userKS = ksm.createKeyStore();
ksm.storeKeyEntry(userKS, "john-email-cert", keyPair.getPrivate(),
    password, new Certificate[] { cert });
ksm.saveKeyStore(userKS, "john-doe.p12", password);
```

### 2. Auto-Enrollment (Server-Side Key Generation)

```java
// RA generates key pair for user
KeyPair keyPair = ksm.generateKeyPair();

// Create certificate with AD attributes
String subjectDN = buildDNFromADUser(adUser); // from AD LDAP
X509Certificate cert = caService.issueCertificate(keyPair, subjectDN);

// Store in centralized KeyStore
KeyStore raKS = ksm.loadKeyStore("ra-keystore.p12", raPassword);
ksm.storeKeyEntry(raKS, adUser.getUsername(), keyPair.getPrivate(),
    entryPassword, new Certificate[] { cert });
ksm.saveKeyStore(raKS, "ra-keystore.p12", raPassword);

// Generate PKCS#12 for user download
generateUserPKCS12(keyPair, cert, entryPassword);
```

### 3. Certificate Expiration Monitoring

```java
// Load RA KeyStore
KeyStore raKS = ksm.loadKeyStore("ra-keystore.p12", raPassword);

// Check all certificates
List<KeyStoreEntry> entries = ksm.listEntries(raKS);
for (KeyStoreEntry entry : entries) {
    Certificate[] chain = ksm.getCertificateChain(raKS, entry.getAlias());
    X509Certificate cert = (X509Certificate) chain[0];
    long daysRemaining = ksm.getDaysUntilExpiration(cert);

    if (daysRemaining <= 30) {
        // Send renewal notification
        notifyUser(entry.getAlias(), daysRemaining);
    }
}
```

---

## 🔒 Security Best Practices

### 1. Password Management

```java
// ✅ GOOD: Use char[] for passwords
char[] password = getPasswordFromSecureSource();
try {
    keyStore.load(inputStream, password);
} finally {
    ksm.clearPassword(password); // Clear from memory
}

// ❌ BAD: Never use String
String password = "myPassword"; // Remains in String pool
```

### 2. File Permissions

```java
// Set restrictive permissions on KeyStore file
Path path = Paths.get("keystore.p12");
Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rw-------");
Files.setPosixFilePermissions(path, perms);
```

### 3. Per-Entry Passwords

```java
// Different passwords for different entries
char[] raPassword = "RA@MasterPassword".toCharArray();
char[] user1Password = ksm.generateSecurePassword(32);
char[] user2Password = ksm.generateSecurePassword(32);

ksm.storeKeyEntry(raKS, "user1", key1, user1Password, chain1);
ksm.storeKeyEntry(raKS, "user2", key2, user2Password, chain2);
```

---

## 🧪 Testing

### Run All Tests

```bash
# Standalone POC
java KeyStorePOC

# Expected output:
# ✓ Key pair generated
# ✓ CSR created and verified
# ✓ Certificate generated
# ✓ KeyStore created (PKCS12)
# ✓ Private key and certificate stored
# ✓ KeyStore saved to ra-keystore.p12
# ✓ All operations verified

# Structured Examples
java com.corp.ra.keystore.KeyStoreExample

# Runs 5 examples:
# Example 1: End Entity Certificate Enrollment
# Example 2: Auto-Enrollment for Multiple Users
# Example 3: Certificate Expiration Monitoring
# Example 4: Certificate Import/Export
# Example 5: RA Officer Digital Signature
```

---

## 📦 Maven Dependencies

```xml
<dependencies>
    <!-- Bouncy Castle Provider -->
    <dependency>
        <groupId>org.bouncycastle</groupId>
        <artifactId>bcprov-jdk18on</artifactId>
        <version>1.78</version>
    </dependency>

    <!-- Bouncy Castle PKIX (X.509 certificates) -->
    <dependency>
        <groupId>org.bouncycastle</groupId>
        <artifactId>bcpkix-jdk18on</artifactId>
        <version>1.78</version>
    </dependency>

    <!-- Bouncy Castle Utilities -->
    <dependency>
        <groupId>org.bouncycastle</groupId>
        <artifactId>bcutil-jdk18on</artifactId>
        <version>1.78</version>
    </dependency>
</dependencies>
```

---

## 🔗 Integration with RA Application

### Spring Boot Service Example

```java
@Service
public class CertificateManagementService {

    private final KeyStoreManager ksManager = new KeyStoreManager();

    @Value("${ra.keystore.path}")
    private String keystorePath;

    @Value("${ra.keystore.password}")
    private String keystorePassword;

    public CertificateEnrollmentResult enrollCertificate(
            String username, PKCS10CertificationRequest csr) throws Exception {

        // Validate CSR
        if (!ksManager.verifyCSRSignature(csr)) {
            throw new InvalidCSRException("CSR signature invalid");
        }

        // Submit to CA
        X509Certificate certificate = caService.signCSR(csr);

        // Load RA KeyStore
        KeyStore raKS = ksManager.loadKeyStore(keystorePath,
            keystorePassword.toCharArray());

        // Store certificate
        String alias = username + "-" + System.currentTimeMillis();
        ksManager.storeKeyEntry(raKS, alias, null, // Private key stored by user
            keystorePassword.toCharArray(), new Certificate[] { certificate });

        // Save KeyStore
        ksManager.saveKeyStore(raKS, keystorePath,
            keystorePassword.toCharArray());

        return new CertificateEnrollmentResult(alias, certificate);
    }
}
```

### Configuration (application.yml)

```yaml
ra:
  keystore:
    path: /var/ra/keystore/ra-certificates.p12
    password: ${KEYSTORE_PASSWORD:changeme}
    type: PKCS12

  certificate:
    validity-days: 365
    renewal-threshold-days: 30
    key-size: 2048
    signature-algorithm: SHA256WithRSA
```

---

## 🐛 Troubleshooting

### Error: "Invalid keystore format"

**Problem**: Wrong KeyStore type specified

```java
// ✓ CORRECT
KeyStore keyStore = KeyStore.getInstance("PKCS12", "BC");

// ✗ WRONG
KeyStore keyStore = KeyStore.getInstance("JKS"); // For PKCS12 file
```

### Error: "Cannot find alias"

**Problem**: Alias doesn't exist or typo

```java
// Check before retrieval
if (keyStore.containsAlias("john-cert")) {
    Key key = keyStore.getKey("john-cert", password);
}
```

### Error: "Certificate chain not valid"

**Problem**: Certificate chain not properly ordered

```java
// ✓ CORRECT ORDER
Certificate[] chain = new Certificate[] {
    endEntityCert,     // First
    intermediateCert,  // Second
    rootCert          // Last
};

// ✗ WRONG ORDER
Certificate[] chain = new Certificate[] {
    rootCert,         // Should be last
    endEntityCert
};
```

---

## 📖 Related Documentation

- **Active Directory Setup**: `AD-QUICK-REFERENCE.md`
- **RA Application Requirements**: `CLAUDE.md`
- **Docker Setup**: `ad-docker-compose.yml`
- **Samba AD Common Errors**: `samba-ad-common-errors.md`

---

## ✅ What You've Learned

1. ✅ **KeyStore.setKeyEntry()** - Store private keys with certificate chains
2. ✅ **PKCS#10 CSR** - Create and validate certificate signing requests
3. ✅ **X.509 Certificates** - Generate self-signed certificates
4. ✅ **KeyStore Management** - Create, load, save, list entries
5. ✅ **Bouncy Castle Integration** - Use BC for cryptographic operations
6. ✅ **Security Best Practices** - Password management, file permissions
7. ✅ **RA Application Integration** - Real-world use cases

---

## 📞 Support

For questions or issues:
1. Review `keystore-bouncycastle-guide.md` for detailed documentation
2. Check troubleshooting section above
3. Run example code to verify setup
4. Review Bouncy Castle documentation: https://www.bouncycastle.org/

---

**Status**: ✅ Production Ready
**Last Updated**: 2026-01-16
**Tested With**: Java 21, Bouncy Castle 1.78
**Author**: RA Development Team
