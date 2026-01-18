# KeyStore Quick Reference Guide

**Version**: 2.0
**Date**: 2026-01-16
**For**: Java 21 with Bouncy Castle 1.78

---

## 🚀 Quick Start

```java
import com.corp.ra.keystore.KeyStoreManager;
import java.security.*;
import java.security.cert.Certificate;

KeyStoreManager ksm = new KeyStoreManager();

// 1. Create KeyStore
KeyStore ks = ksm.createKeyStore(); // PKCS#12 by default

// 2. Generate key pair and certificate
KeyPair keyPair = ksm.generateKeyPair();
String subjectDN = "CN=John Doe, E=john@corp.local, O=Corp";
X509Certificate cert = ksm.generateSelfSignedCertificate(keyPair, subjectDN, 365);

// 3. Store in KeyStore
char[] password = "SecurePass123".toCharArray();
ksm.storeKeyEntry(ks, "my-cert", keyPair.getPrivate(), password,
    new Certificate[] { cert });

// 4. Save to file
ksm.saveKeyStore(ks, "certificates.p12", password);

// 5. Load and retrieve
KeyStore loadedKS = ksm.loadKeyStore("certificates.p12", password);
PrivateKey key = ksm.getPrivateKey(loadedKS, "my-cert", password);
```

---

## 📦 Supported KeyStore Types

| Type | Create | Use Case |
|------|--------|----------|
| **PKCS#12** | `ksm.createKeyStore(KeyStoreType.PKCS12)` | ✓ Default, cross-platform |
| **JKS** | `ksm.createKeyStore(KeyStoreType.JKS)` | Legacy Java apps |
| **JCEKS** | `ksm.createKeyStore(KeyStoreType.JCEKS)` | Stronger than JKS |
| **BKS** | `ksm.createKeyStore(KeyStoreType.BKS)` | Android, Bouncy Castle |
| **PKCS#11** | `ksm.loadPKCS11KeyStore("cfg", pin)` | HSM, Smart Cards |

---

## 🔑 Common Operations

### Create Different KeyStore Types

```java
KeyStoreManager ksm = new KeyStoreManager();

// PKCS#12 (recommended)
KeyStore pkcs12 = ksm.createKeyStore(KeyStoreManager.KeyStoreType.PKCS12);

// JKS (legacy)
KeyStore jks = ksm.createKeyStore(KeyStoreManager.KeyStoreType.JKS);

// PKCS#11 (HSM)
KeyStore hsm = ksm.loadPKCS11KeyStore("pkcs11.cfg", pin);
```

### Generate CSR (Certificate Signing Request)

```java
KeyPair keyPair = ksm.generateKeyPair();
String subjectDN = "CN=User, E=user@example.com, O=Corp";

PKCS10CertificationRequest csr = ksm.createCSR(keyPair, subjectDN);

// Verify CSR signature
boolean valid = ksm.verifyCSRSignature(csr);
```

### List All Certificates

```java
KeyStore ks = ksm.loadKeyStore("certs.p12", password);
List<KeyStoreManager.KeyStoreEntry> entries = ksm.listEntries(ks);

for (KeyStoreManager.KeyStoreEntry entry : entries) {
    System.out.println("Alias: " + entry.getAlias());
    System.out.println("Subject: " + entry.getSubjectDN());
    System.out.println("Expires: " + entry.getNotAfter());
}
```

### Check Certificate Expiration

```java
KeyStore ks = ksm.loadKeyStore("certs.p12", password);
Certificate[] chain = ksm.getCertificateChain(ks, "cert-alias");
X509Certificate cert = (X509Certificate) chain[0];

long daysRemaining = ksm.getDaysUntilExpiration(cert);
boolean expired = ksm.isCertificateExpired(cert);

if (daysRemaining <= 30) {
    System.out.println("⚠ Certificate expiring soon: " + daysRemaining + " days");
}
```

### Convert JKS to PKCS#12

```java
// Load JKS
KeyStore jks = ksm.loadKeyStore("old.jks", password,
    KeyStoreManager.KeyStoreType.JKS);

// Extract keys and certificates
PrivateKey key = ksm.getPrivateKey(jks, "alias", password);
Certificate[] chain = ksm.getCertificateChain(jks, "alias");

// Create PKCS#12 and store
KeyStore pkcs12 = ksm.createKeyStore(KeyStoreManager.KeyStoreType.PKCS12);
ksm.storeKeyEntry(pkcs12, "alias", key, password, chain);
ksm.saveKeyStore(pkcs12, "converted.p12", password);
```

### Delete Certificate

```java
KeyStore ks = ksm.loadKeyStore("certs.p12", password);
ksm.deleteEntry(ks, "old-cert-alias");
ksm.saveKeyStore(ks, "certs.p12", password);
```

---

## 🔐 Security Best Practices

### Password Management

```java
// ✅ GOOD: Use char[] and clear after use
char[] password = getPasswordFromSecureSource();
try {
    KeyStore ks = ksm.loadKeyStore("certs.p12", password);
    // Use KeyStore...
} finally {
    ksm.clearPassword(password); // Clear from memory
}

// ❌ BAD: Never use String
String password = "myPassword"; // Stays in String pool forever
```

### Generate Secure Passwords

```java
// Generate random 32-character password
char[] securePassword = ksm.generateSecurePassword(32);

// Use for entry-specific protection
ksm.storeKeyEntry(ks, "cert1", key1, securePassword, chain1);
```

### File Permissions

```java
// KeyStoreManager automatically sets secure permissions (rw-------)
ksm.saveKeyStore(ks, "certs.p12", password);
// File created with permissions: 600 (owner read/write only)
```

---

## 🎯 Use Case Examples

### 1. End Entity Certificate Enrollment

```java
KeyStoreManager ksm = new KeyStoreManager();

// User generates key pair locally
KeyPair keyPair = ksm.generateKeyPair();

// Create CSR
String subjectDN = "CN=John Doe, E=john@corp.local, OU=IT";
PKCS10CertificationRequest csr = ksm.createCSR(keyPair, subjectDN);

// RA validates CSR
if (ksm.verifyCSRSignature(csr)) {
    // Submit to CA for signing
    X509Certificate issuedCert = caService.signCSR(csr);

    // Store certificate with private key
    KeyStore userKS = ksm.createKeyStore();
    char[] password = "UserPass123".toCharArray();
    ksm.storeKeyEntry(userKS, "my-cert", keyPair.getPrivate(),
        password, new Certificate[] { issuedCert });
    ksm.saveKeyStore(userKS, "user-cert.p12", password);
}
```

### 2. Auto-Enrollment for AD Users

```java
KeyStoreManager ksm = new KeyStoreManager();
KeyStore raKS = ksm.createKeyStore();

List<ADUser> adUsers = adService.getEligibleUsers();

for (ADUser user : adUsers) {
    // Generate key pair server-side
    KeyPair kp = ksm.generateKeyPair();

    // Create certificate from AD attributes
    String subjectDN = String.format("CN=%s, E=%s, OU=%s",
        user.getDisplayName(), user.getEmail(), user.getDepartment());
    X509Certificate cert = caService.issueCertificate(kp, subjectDN);

    // Store in RA KeyStore
    char[] entryPass = ksm.generateSecurePassword(32);
    ksm.storeKeyEntry(raKS, user.getUsername(), kp.getPrivate(),
        entryPass, new Certificate[] { cert });
}

ksm.saveKeyStore(raKS, "ra-autoenroll.p12", raPassword);
```

### 3. Certificate Expiration Monitoring

```java
KeyStoreManager ksm = new KeyStoreManager();
KeyStore ks = ksm.loadKeyStore("certificates.p12", password);

List<KeyStoreManager.KeyStoreEntry> entries = ksm.listEntries(ks);

for (KeyStoreManager.KeyStoreEntry entry : entries) {
    Certificate[] chain = ksm.getCertificateChain(ks, entry.getAlias());
    X509Certificate cert = (X509Certificate) chain[0];
    long daysRemaining = ksm.getDaysUntilExpiration(cert);

    if (daysRemaining <= 30) {
        emailService.sendRenewalNotification(
            entry.getAlias(), cert, daysRemaining);
    }
}
```

### 4. HSM Integration

```java
KeyStoreManager ksm = new KeyStoreManager();

// Load KeyStore from HSM
char[] pin = "1234".toCharArray();
KeyStore hsmKS = ksm.loadPKCS11KeyStore("pkcs11.cfg", pin);

// Sign using hardware-backed key
PrivateKey hsmKey = ksm.getPrivateKey(hsmKS, "ca-signing-key", pin);

Signature signer = Signature.getInstance("SHA256withRSA");
signer.initSign(hsmKey);
signer.update(dataToSign);
byte[] signature = signer.sign();

// Private key never leaves HSM!
```

---

## 📋 Method Reference

### KeyStore Operations
```java
KeyStore createKeyStore()
KeyStore createKeyStore(KeyStoreType type)
KeyStore loadKeyStore(String path, char[] password)
KeyStore loadKeyStore(String path, char[] password, KeyStoreType type)
KeyStore loadPKCS11KeyStore(String configPath, char[] pin)
void saveKeyStore(KeyStore ks, String path, char[] password)
```

### Key & Certificate Generation
```java
KeyPair generateKeyPair()
KeyPair generateKeyPair(int keySize)
PKCS10CertificationRequest createCSR(KeyPair kp, String subjectDN)
boolean verifyCSRSignature(PKCS10CertificationRequest csr)
X509Certificate generateSelfSignedCertificate(KeyPair kp, String subjectDN, int validityDays)
```

### Entry Management
```java
void storeKeyEntry(KeyStore ks, String alias, PrivateKey key, char[] password, Certificate[] chain)
PrivateKey getPrivateKey(KeyStore ks, String alias, char[] password)
Certificate[] getCertificateChain(KeyStore ks, String alias)
List<KeyStoreEntry> listEntries(KeyStore ks)
void deleteEntry(KeyStore ks, String alias)
```

### Utilities
```java
char[] generateSecurePassword(int length)
void clearPassword(char[] password)
boolean isCertificateExpired(X509Certificate cert)
long getDaysUntilExpiration(X509Certificate cert)
```

---

## ⚠️ Common Issues

### Issue: "Invalid keystore format"
**Cause**: Wrong KeyStore type specified
```java
// ❌ Wrong
KeyStore ks = ksm.loadKeyStore("file.jks", pass, KeyStoreType.PKCS12);

// ✅ Correct
KeyStore ks = ksm.loadKeyStore("file.jks", pass, KeyStoreType.JKS);
```

### Issue: "Cannot find alias"
**Cause**: Alias doesn't exist
```java
// ✅ Check first
if (ks.containsAlias("my-cert")) {
    PrivateKey key = ksm.getPrivateKey(ks, "my-cert", password);
}
```

### Issue: "Certificate chain not valid"
**Cause**: Chain ordering wrong
```java
// ✅ Correct order: end-entity → intermediate → root
Certificate[] chain = new Certificate[] {
    endEntityCert,    // First
    intermediateCert, // Second
    rootCert         // Last
};
```

### Issue: "sun.security.pkcs11 cannot be resolved"
**Cause**: Direct use of internal class
**Solution**: Already fixed! Uses reflection in `loadPKCS11KeyStore()`

---

## 📚 Examples

Run all examples:
```bash
javac -cp "lib/*" src/main/java/com/corp/ra/keystore/*.java
java -cp "src/main/java:lib/*" com.corp.ra.keystore.KeyStoreExample
```

Individual examples:
1. End Entity Certificate Enrollment
2. Auto-Enrollment for Multiple Users
3. Certificate Expiration Monitoring
4. Certificate Import/Export
5. RA Officer Digital Signature
6. Different KeyStore Types (PKCS#12, JKS, JCEKS, BKS, PKCS#11)

---

## 📖 Documentation

- **KEYSTORE-README.md** - Quick start guide
- **keystore-bouncycastle-guide.md** - Complete documentation
- **KEYSTORE-UPDATE-SUMMARY.md** - All changes and updates
- **PKCS11-COMPATIBILITY-FIX.md** - PKCS#11 implementation details
- **KEYSTORE-QUICK-REFERENCE.md** - This document

---

## 🔗 Related Files

| File | Purpose |
|------|---------|
| `KeyStorePOC.java` | Standalone POC |
| `KeyStoreManager.java` | Production-ready class |
| `KeyStoreExample.java` | 6 practical examples |

---

**Quick Tip**: For most use cases, stick with **PKCS#12** KeyStore type. It's the industry standard and works everywhere (Java, OpenSSL, browsers, .NET, etc.).

---

**Version**: 2.0
**Last Updated**: 2026-01-16
**Status**: ✅ Ready to Use
