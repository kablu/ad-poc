# Registration Authority (RA) Application - Functional Design Specification

## Document Information
- **Document Title**: Registration Authority (RA) Functional Design Specification
- **Document Version**: 1.0
- **Date**: 2026-01-13
- **Status**: Draft - Ready for Review
- **Classification**: Confidential
- **Target Audience**: Developers, System Architects, Security Teams, PKI Administrators
- **Purpose**: Complete functional design specification for RA application integrating with existing CA

---

## Document Revision History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-01-13 | PKI Team | Initial version - Complete FSD |

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Introduction](#2-introduction)
3. [Scope and Objectives](#3-scope-and-objectives)
4. [Applicable Standards and RFCs](#4-applicable-standards-and-rfcs)
5. [System Architecture](#5-system-architecture)
6. [Authentication and Authorization](#6-authentication-and-authorization)
7. [Certificate Request Workflow](#7-certificate-request-workflow)
8. [Certificate Issuance Process](#8-certificate-issuance-process)
9. [Certificate Download and Distribution](#9-certificate-download-and-distribution)
10. [Certificate Revocation](#10-certificate-revocation)
11. [Certificate Renewal](#11-certificate-renewal)
12. [Audit and Logging](#12-audit-and-logging)
13. [Security Architecture](#13-security-architecture)
14. [Database Design](#14-database-design)
15. [API Specifications](#15-api-specifications)
16. [Integration with CA](#16-integration-with-ca)
17. [User Interface Requirements](#17-user-interface-requirements)
18. [Non-Functional Requirements](#18-non-functional-requirements)
19. [Deployment Architecture](#19-deployment-architecture)
20. [Testing Strategy](#20-testing-strategy)
21. [Appendices](#21-appendices)

---

## 1. Executive Summary

### 1.1 Purpose
This document specifies the functional design for a Registration Authority (RA) application that acts as an intermediary between end entities and a Certificate Authority (CA). The RA will handle certificate request validation, authentication, and approval workflows while the CA performs actual certificate issuance.

### 1.2 Key Features
- **Multi-layered Authentication**: Secure end entity authentication
- **PKCS#10 CSR Processing**: Standard-compliant certificate request handling
- **Role-Based Access Control**: RA Admin, Officer, Operator, Auditor roles
- **CA Integration**: Seamless integration with existing CA infrastructure
- **Certificate Lifecycle Management**: Issuance, renewal, revocation, suspension
- **Comprehensive Audit Trail**: Full compliance and traceability
- **RESTful API**: Modern API-first architecture
- **Web Portal**: User-friendly web interface

### 1.3 Compliance
- RFC 2986 (PKCS#10 CSR)
- RFC 5280 (X.509 Certificate Profile)
- RFC 6960 (OCSP)
- RFC 5652 (CMS)
- RFC 3647 (CA/Browser Forum Guidelines)
- NIST SP 800-63 (Digital Identity Guidelines)
- Common Criteria (EAL4+)

---

## 2. Introduction

### 2.1 Background
The Registration Authority (RA) is a critical component in a Public Key Infrastructure (PKI) that performs identity verification and certificate request validation before forwarding approved requests to the Certificate Authority (CA) for issuance.

### 2.2 RA vs CA Separation
According to RFC 3647 and industry best practices:

**Registration Authority (RA):**
- Authenticates end entity identity
- Validates certificate requests
- Approves/rejects certificate requests
- Does NOT hold CA private keys
- Does NOT sign certificates
- Acts as trusted agent of CA

**Certificate Authority (CA):**
- Issues and signs certificates
- Maintains CA private keys (HSM-protected)
- Publishes certificates and CRLs
- Revokes certificates
- Maintains certificate database

### 2.3 System Context

```
┌─────────────────────────────────────────────────────────────┐
│                    PKI Ecosystem                            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────────┐         ┌──────────────┐                 │
│  │  End Entity  │────────▶│      RA      │                 │
│  │  (Requester) │         │ Application  │                 │
│  └──────────────┘         └──────┬───────┘                 │
│                                  │                          │
│                                  │ Approved                 │
│                                  │ Requests                 │
│                                  ▼                          │
│                           ┌──────────────┐                  │
│                           │      CA      │                  │
│                           │ (Existing)   │                  │
│                           └──────┬───────┘                  │
│                                  │                          │
│                                  │ Issued                   │
│                                  │ Certificates             │
│                                  ▼                          │
│                           ┌──────────────┐                  │
│                           │ Certificate  │                  │
│                           │  Repository  │                  │
│                           └──────────────┘                  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 3. Scope and Objectives

### 3.1 In Scope
- ✅ End entity authentication (multiple methods)
- ✅ PKCS#10 CSR submission and validation
- ✅ RA operator/officer approval workflow
- ✅ Integration with existing CA via API/protocol
- ✅ Certificate download and distribution
- ✅ Certificate revocation requests
- ✅ Certificate renewal workflow
- ✅ Comprehensive audit logging
- ✅ Role-based access control (RBAC)
- ✅ Web-based user interface
- ✅ RESTful API
- ✅ Email/SMS notifications

### 3.2 Out of Scope
- ❌ Certificate Authority (CA) implementation (existing CA)
- ❌ HSM integration (handled by CA)
- ❌ Certificate signing (done by CA)
- ❌ CRL/OCSP publication (CA responsibility)
- ❌ Root CA operations
- ❌ Hardware token provisioning

### 3.3 Objectives
1. **Security**: Implement multi-layered authentication and authorization
2. **Compliance**: Adhere to RFC standards and PKI best practices
3. **Scalability**: Support high-volume certificate operations
4. **Usability**: Provide intuitive interfaces for all user roles
5. **Auditability**: Maintain comprehensive, immutable audit trails
6. **Flexibility**: Support multiple certificate profiles and policies
7. **Integration**: Seamless integration with existing CA infrastructure

---

## 4. Applicable Standards and RFCs

### 4.1 Core PKI Standards

#### RFC 2986 - PKCS#10: Certification Request Syntax
**Purpose**: Defines the format for certificate requests

**Key Requirements:**
- CSR must contain subject distinguished name (DN)
- CSR must contain public key
- CSR must be signed by corresponding private key
- Signature algorithm must be specified
- Optional attributes supported (extensions, challenge password)

**Implementation:**
```java
// Parse PKCS#10 CSR
PKCS10CertificationRequest csr = new PKCS10CertificationRequest(csrBytes);

// Extract components
X500Name subject = csr.getSubject();
SubjectPublicKeyInfo publicKeyInfo = csr.getSubjectPublicKeyInfo();

// Verify signature (Proof of Possession)
ContentVerifierProvider verifier =
    new JcaContentVerifierProviderBuilder().build(publicKeyInfo);
boolean valid = csr.isSignatureValid(verifier);
```

#### RFC 5280 - X.509 Public Key Infrastructure Certificate Profile
**Purpose**: Defines X.509 certificate structure

**Key Requirements:**
- Certificate version (v3 recommended)
- Serial number (unique per CA)
- Signature algorithm
- Issuer DN
- Validity period (notBefore, notAfter)
- Subject DN
- Subject public key info
- Extensions (key usage, extended key usage, SAN, etc.)

**Mandatory Extensions for Specific Certificate Types:**

| Certificate Type | Mandatory Extensions |
|-----------------|---------------------|
| End Entity | Key Usage, Extended Key Usage, Subject Alternative Name |
| CA Certificate | Basic Constraints (CA=true), Key Usage (keyCertSign, cRLSign) |
| OCSP Responder | Extended Key Usage (OCSP Signing) |

#### RFC 6960 - Online Certificate Status Protocol (OCSP)
**Purpose**: Real-time certificate status checking

**RA Responsibility:**
- Query OCSP responder for certificate status
- Display status to end entities
- Log OCSP responses

#### RFC 3647 - Certificate Policy and Certification Practice Statement
**Purpose**: Defines operational procedures

**RA Requirements:**
- Identity verification procedures
- Authentication mechanisms
- Approval workflows
- Audit and logging requirements
- Security controls

### 4.2 Cryptographic Standards

#### RFC 3370 - Cryptographic Message Syntax (CMS) Algorithms
**Supported Algorithms:**

**Signature Algorithms:**
- RSA with SHA-256 (MANDATORY)
- RSA with SHA-384 (RECOMMENDED)
- RSA with SHA-512 (RECOMMENDED)
- ECDSA with SHA-256 (RECOMMENDED)
- ECDSA with SHA-384 (RECOMMENDED)

**Key Sizes:**
- RSA: Minimum 2048 bits (4096 recommended for high-value certificates)
- ECDSA: P-256, P-384, P-521

**Deprecated (NOT Supported):**
- MD5 (MUST NOT use)
- SHA-1 (MUST NOT use for signatures)
- RSA 1024-bit (MUST NOT use)

### 4.3 Security Standards

#### NIST SP 800-63 - Digital Identity Guidelines
**Authentication Assurance Levels (AAL):**

| AAL | Authentication Method | Use Case |
|-----|----------------------|----------|
| AAL1 | Password only | Low-risk certificates |
| AAL2 | Password + OTP/SMS | Standard certificates |
| AAL3 | Hardware token + PIN | High-value certificates |

#### NIST SP 800-57 - Key Management
**Key Lifecycle Requirements:**
- Private keys MUST be generated on secure devices
- Private keys MUST NOT be transmitted unencrypted
- Private keys MUST be stored encrypted
- Key escrow policies defined

### 4.4 Industry Standards

#### CA/Browser Forum Baseline Requirements
**Domain Validation (DV):**
- Domain control validation required
- WHOIS record validation OR
- Email to admin@domain OR
- HTTP/HTTPS challenge

**Organization Validation (OV):**
- Legal existence verification
- Physical address verification
- Telephone verification

**Extended Validation (EV):**
- Extensive legal identity verification
- Face-to-face verification option
- Operational existence verification

---

## 5. System Architecture

### 5.1 High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                        RA Application                               │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │                  Presentation Layer                        │   │
│  ├────────────────────────────────────────────────────────────┤   │
│  │  Web UI (React/Angular)  │  REST API  │  Mobile API       │   │
│  └─────────────┬────────────────────────────┬─────────────────┘   │
│                │                            │                      │
│  ┌─────────────▼────────────────────────────▼─────────────────┐   │
│  │                  Application Layer                         │   │
│  ├────────────────────────────────────────────────────────────┤   │
│  │  Authentication │ Authorization │ Request Validation       │   │
│  │  Approval       │ Notification  │ Certificate Mgmt         │   │
│  └─────────────┬────────────────────────────┬─────────────────┘   │
│                │                            │                      │
│  ┌─────────────▼────────────────────────────▼─────────────────┐   │
│  │                   Business Logic Layer                     │   │
│  ├────────────────────────────────────────────────────────────┤   │
│  │  CSR Processing │ Identity Verification │ Workflow Engine  │   │
│  │  Policy Engine  │ Audit Logger         │ Event Handler    │   │
│  └─────────────┬────────────────────────────┬─────────────────┘   │
│                │                            │                      │
│  ┌─────────────▼────────────────────────────▼─────────────────┐   │
│  │                   Data Access Layer                        │   │
│  ├────────────────────────────────────────────────────────────┤   │
│  │  RA Database  │  Cache (Redis)  │  File Storage           │   │
│  └─────────────┬────────────────────────────┬─────────────────┘   │
│                │                            │                      │
│  ┌─────────────▼────────────────────────────▼─────────────────┐   │
│  │                 Integration Layer                          │   │
│  ├────────────────────────────────────────────────────────────┤   │
│  │  CA API Client │ Email Service │ SMS Service │ LDAP/AD     │   │
│  └────────────────────────────────────────────────────────────┘   │
│                                                                     │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
                               ▼
                    ┌──────────────────────┐
                    │  Existing CA System  │
                    │  - Certificate       │
                    │    Issuance          │
                    │  - Signing           │
                    │  - CRL Publication   │
                    └──────────────────────┘
```

### 5.2 Component Description

#### 5.2.1 Presentation Layer
**Web UI:**
- End entity portal (certificate requests, downloads)
- RA operator portal (request processing)
- RA officer portal (approval/rejection)
- Admin portal (configuration, user management)

**REST API:**
- Certificate request submission
- Status queries
- Certificate download
- Revocation requests
- Administrative operations

#### 5.2.2 Application Layer
**Authentication Service:**
- Multi-factor authentication
- Session management
- Token-based authentication (JWT)
- Integration with external identity providers

**Authorization Service:**
- Role-based access control (RBAC)
- Permission evaluation
- Attribute-based access control (ABAC)

**Request Validation Service:**
- CSR parsing and validation
- Signature verification (Proof of Possession)
- Policy compliance checking
- Subject DN validation

#### 5.2.3 Business Logic Layer
**CSR Processing Engine:**
- Parse PKCS#10 CSR
- Extract subject information
- Validate public key
- Check against certificate profiles

**Identity Verification Service:**
- Multi-level identity proofing
- Document verification
- Database checks (if applicable)
- External validation services integration

**Workflow Engine:**
- State machine for request lifecycle
- Approval routing logic
- SLA tracking
- Escalation handling

**Policy Engine:**
- Certificate policy enforcement
- Profile selection
- Validity period calculation
- Extension population

#### 5.2.4 Data Access Layer
**RA Database:**
- Certificate requests
- User accounts
- Audit logs
- Configuration

**Cache Layer:**
- Session cache
- Frequently accessed data
- Rate limiting counters

#### 5.2.5 Integration Layer
**CA API Client:**
- Submit approved requests to CA
- Query certificate status
- Request revocation
- Retrieve issued certificates

### 5.3 Technology Stack Recommendations

**Backend:**
- Java 17+ with Spring Boot 3.x
- Spring Security for authentication/authorization
- Spring Data JPA for database access
- Bouncy Castle for cryptographic operations

**Frontend:**
- React 18+ or Angular 15+
- TypeScript
- Material UI or Ant Design

**Database:**
- PostgreSQL 15+ (primary)
- Redis 7+ (cache)

**Integration:**
- REST APIs (JSON over HTTPS)
- Message Queue (optional): RabbitMQ or Apache Kafka

**Security:**
- TLS 1.3 for all communications
- JWT for API authentication
- bcrypt for password hashing

---

## 6. Authentication and Authorization

### 6.1 End Entity Authentication

#### 6.1.1 Authentication Methods

**Method 1: Username/Password + Multi-Factor Authentication (MFA)**

```
┌─────────────────────────────────────────────────────────┐
│         End Entity Authentication Flow                 │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  Step 1: Primary Authentication                        │
│  ───────────────────────────────────────               │
│  User → Enters username/email + password               │
│  RA → Validates credentials against local DB or LDAP   │
│                                                         │
│  Step 2: Multi-Factor Authentication (if enabled)      │
│  ───────────────────────────────────────               │
│  Option A: SMS OTP                                     │
│    RA → Sends 6-digit OTP to registered mobile        │
│    User → Enters OTP (valid 5 minutes)                │
│                                                         │
│  Option B: Email OTP                                   │
│    RA → Sends OTP to registered email                 │
│    User → Clicks link or enters OTP                   │
│                                                         │
│  Option C: TOTP (Time-based OTP)                      │
│    User → Generates OTP via authenticator app          │
│    RA → Validates TOTP code                           │
│                                                         │
│  Option D: Hardware Token                             │
│    User → Inserts smart card or USB token             │
│    RA → Validates certificate on token                │
│                                                         │
│  Step 3: Session Creation                             │
│  ───────────────────────────────────────               │
│  RA → Issues JWT access token (1 hour validity)       │
│  RA → Issues refresh token (7 days validity)          │
│  RA → Creates session record in database              │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

**Implementation:**
```java
@Service
public class AuthenticationService {

    public AuthenticationResult authenticate(
            String username, String password, String mfaCode) {

        // Step 1: Validate credentials
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            recordFailedAttempt(username);
            throw new BadCredentialsException("Invalid credentials");
        }

        // Check account status
        if (user.isLocked() || !user.isEnabled()) {
            throw new AccountStatusException("Account is locked or disabled");
        }

        // Step 2: MFA validation (if enabled for user)
        if (user.isMfaEnabled()) {
            validateMfaCode(user, mfaCode);
        }

        // Step 3: Generate tokens
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        // Step 4: Create session
        UserSession session = sessionService.createSession(user, accessToken);

        // Step 5: Audit log
        auditLogger.log(AuditEvent.AUTHENTICATION_SUCCESS, user.getUsername());

        return AuthenticationResult.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .expiresIn(3600)
            .user(UserDTO.from(user))
            .build();
    }

    private void validateMfaCode(User user, String mfaCode) {
        switch (user.getMfaType()) {
            case SMS_OTP:
                if (!otpService.validateSmsOtp(user.getMobileNumber(), mfaCode)) {
                    throw new InvalidMfaException("Invalid OTP code");
                }
                break;
            case TOTP:
                if (!totpService.validateTotp(user.getTotpSecret(), mfaCode)) {
                    throw new InvalidMfaException("Invalid TOTP code");
                }
                break;
            case HARDWARE_TOKEN:
                if (!tokenService.validateHardwareToken(user, mfaCode)) {
                    throw new InvalidMfaException("Invalid hardware token");
                }
                break;
        }
    }
}
```

#### 6.1.2 Authentication API Endpoints

```http
POST /api/v1/auth/login
Content-Type: application/json

Request:
{
  "username": "user@company.com",
  "password": "SecureP@ssw0rd",
  "mfa_code": "123456"  // Optional, required if MFA enabled
}

Response: 200 OK
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refresh_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "Bearer",
  "expires_in": 3600,
  "user": {
    "user_id": "uuid-123",
    "username": "user@company.com",
    "full_name": "John Doe",
    "roles": ["END_ENTITY"],
    "mfa_enabled": true
  }
}
```

### 6.2 RA Staff Authentication

RA staff (Admins, Officers, Operators) require stronger authentication:

**Requirements:**
- Mandatory MFA (no option to disable)
- Hardware token recommended for Admin role
- IP address whitelist (optional)
- Session timeout: 30 minutes inactivity

### 6.3 Authorization Model

#### 6.3.1 Role Definitions

**Role: END_ENTITY**
- Submit own certificate requests
- View own certificate status
- Download own certificates
- Renew own certificates
- Request revocation of own certificates

**Role: RA_OPERATOR**
- Submit certificate requests on behalf of end entities
- View submitted requests
- Update request information (before approval)
- Upload supporting documents
- View certificate status

**Role: RA_OFFICER**
- All RA_OPERATOR permissions
- Approve certificate requests
- Reject certificate requests
- Revoke certificates
- Suspend certificates
- View all requests and certificates
- Generate reports

**Role: RA_ADMIN**
- All RA_OFFICER permissions
- Manage users (create, update, delete)
- Assign roles
- Configure certificate profiles
- Configure policies
- Access audit logs
- System configuration
- Integration settings

**Role: AUDITOR**
- Read-only access to all data
- Access audit logs
- Generate compliance reports
- Export data
- NO modification capabilities

#### 6.3.2 Permission Matrix

| Operation | END_ENTITY | RA_OPERATOR | RA_OFFICER | RA_ADMIN | AUDITOR |
|-----------|------------|-------------|------------|----------|---------|
| Submit CSR (self) | ✓ | ✓ | ✓ | ✓ | ✗ |
| Submit CSR (behalf) | ✗ | ✓ | ✓ | ✓ | ✗ |
| Approve CSR | ✗ | ✗ | ✓ | ✓ | ✗ |
| Reject CSR | ✗ | ✗ | ✓ | ✓ | ✗ |
| Revoke Certificate | ✗ (own only) | ✗ | ✓ | ✓ | ✗ |
| View All Requests | ✗ | ✗ | ✓ | ✓ | ✓ |
| View Own Requests | ✓ | ✓ | ✓ | ✓ | ✓ |
| Download Certificate | ✓ (own only) | ✓ | ✓ | ✓ | ✗ |
| Manage Users | ✗ | ✗ | ✗ | ✓ | ✗ |
| Configure Profiles | ✗ | ✗ | ✗ | ✓ | ✗ |
| View Audit Logs | ✗ | ✗ | ✓ | ✓ | ✓ |
| Export Reports | ✗ | ✗ | ✓ | ✓ | ✓ |

---

## 7. Certificate Request Workflow

### 7.1 Complete CSR Submission and Processing Flow

```
┌────────────────────────────────────────────────────────────────┐
│           Certificate Request Lifecycle                        │
├────────────────────────────────────────────────────────────────┤
│                                                                │
│  Phase 1: CSR Submission                                       │
│  ──────────────────────────────────────────                    │
│  1. End Entity authenticates to RA portal                      │
│  2. End Entity generates key pair locally (recommended)        │
│  3. End Entity creates PKCS#10 CSR                            │
│  4. End Entity uploads CSR via web form or API                │
│  5. End Entity selects certificate profile/template           │
│  6. End Entity provides justification/business reason         │
│                                                                │
│  Phase 2: Initial Validation (Automated)                       │
│  ──────────────────────────────────────────                    │
│  1. RA validates CSR format (PKCS#10 compliance)              │
│  2. RA verifies CSR signature (Proof of Possession)           │
│  3. RA extracts Subject DN and public key                     │
│  4. RA validates Subject DN against authenticated user        │
│  5. RA checks public key algorithm and size                   │
│  6. RA checks for duplicate public keys                       │
│  7. RA validates requested extensions                         │
│  8. RA checks certificate quota (if applicable)               │
│                                                                │
│  Phase 3: Identity Verification (Policy-Based)                 │
│  ──────────────────────────────────────────                    │
│  Option A: No Additional Verification (Low-Value Certs)       │
│    → Auto-approve if all automated checks pass                │
│                                                                │
│  Option B: Email Verification                                 │
│    → Send verification email to Subject email address         │
│    → User clicks link to confirm                             │
│                                                                │
│  Option C: Document Verification                              │
│    → RA Operator reviews uploaded documents                   │
│    → Operator marks identity as verified                      │
│                                                                │
│  Option D: Face-to-Face Verification (High-Value Certs)       │
│    → End Entity visits RA office in person                    │
│    → RA Officer verifies government-issued ID                 │
│    → RA Officer marks identity as verified                    │
│                                                                │
│  Phase 4: RA Officer Approval                                 │
│  ──────────────────────────────────────────                    │
│  1. RA Officer receives notification of pending request       │
│  2. RA Officer reviews:                                       │
│     - CSR details (Subject DN, key type, extensions)          │
│     - Identity verification status                            │
│     - Supporting documents                                    │
│     - Business justification                                  │
│  3. RA Officer makes decision:                                │
│     Option A: APPROVE → Proceed to Phase 5                    │
│     Option B: REJECT → Notify end entity with reason         │
│     Option C: REQUEST MORE INFO → End entity provides info    │
│                                                                │
│  Phase 5: Submission to CA                                    │
│  ──────────────────────────────────────────                    │
│  1. RA constructs certificate request for CA                  │
│  2. RA adds certificate profile information                   │
│  3. RA specifies validity period                              │
│  4. RA submits to CA via API/protocol                         │
│  5. CA validates request                                      │
│  6. CA issues and signs certificate                           │
│  7. CA returns signed certificate to RA                       │
│                                                                │
│  Phase 6: Certificate Issuance and Delivery                   │
│  ──────────────────────────────────────────                    │
│  1. RA stores certificate in database                         │
│  2. RA updates request status to ISSUED                       │
│  3. RA sends notification email to end entity                 │
│  4. End entity downloads certificate from portal              │
│  5. RA creates audit log entry                                │
│                                                                │
└────────────────────────────────────────────────────────────────┘
```

### 7.2 CSR Submission Implementation

#### 7.2.1 CSR Upload API

```http
POST /api/v1/certificates/requests
Authorization: Bearer <jwt-token>
Content-Type: application/json

Request:
{
  "csr": "-----BEGIN CERTIFICATE REQUEST-----\nMIICvDCCAaQCAQAwdzELMAkGA1UEBhMCVVMxEzARBgNVBAgMCkNhbGlmb3JuaWEx\n...\n-----END CERTIFICATE REQUEST-----",
  "certificate_profile_id": "email-certificate-v1",
  "validity_days": 365,
  "justification": "Email signing and encryption certificate for secure business communication",
  "additional_emails": [
    "john.backup@company.com"
  ],
  "delivery_method": "DOWNLOAD"  // DOWNLOAD, EMAIL, PKCS12
}

Response: 201 Created
{
  "request_id": "req-uuid-12345",
  "status": "PENDING_VALIDATION",
  "submitted_at": "2026-01-13T10:00:00Z",
  "csr_details": {
    "subject_dn": "CN=John Doe, E=john.doe@company.com, OU=Engineering, O=Company Inc, C=US",
    "public_key_algorithm": "RSA",
    "key_size": 2048,
    "signature_algorithm": "SHA256withRSA",
    "subject_dn_verified": true,
    "proof_of_possession_valid": true
  },
  "next_steps": "Your certificate request has been submitted. It will be reviewed by an RA Officer.",
  "estimated_completion": "2026-01-15T10:00:00Z"
}
```

#### 7.2.2 CSR Validation Service

```java
@Service
public class CsrValidationService {

    private static final int MIN_RSA_KEY_SIZE = 2048;
    private static final int MIN_ECDSA_KEY_SIZE = 256;
    private static final Set<String> ALLOWED_SIGNATURE_ALGORITHMS =
        Set.of("SHA256withRSA", "SHA384withRSA", "SHA512withRSA",
               "SHA256withECDSA", "SHA384withECDSA", "SHA512withECDSA");

    public CsrValidationResult validateCsr(String csrPem, User authenticatedUser)
            throws CsrValidationException {

        // 1. Parse PKCS#10 CSR
        PKCS10CertificationRequest csr = parsePKCS10(csrPem);

        // 2. Verify signature (Proof of Possession)
        if (!verifySignature(csr)) {
            throw new CsrValidationException("Invalid CSR signature. " +
                "Proof of possession failed.");
        }

        // 3. Extract components
        X500Name subjectDN = csr.getSubject();
        SubjectPublicKeyInfo publicKeyInfo = csr.getSubjectPublicKeyInfo();

        // 4. Validate public key
        validatePublicKey(publicKeyInfo);

        // 5. Validate signature algorithm
        String signatureAlgorithm = getSignatureAlgorithm(csr);
        if (!ALLOWED_SIGNATURE_ALGORITHMS.contains(signatureAlgorithm)) {
            throw new CsrValidationException("Signature algorithm not allowed: "
                + signatureAlgorithm);
        }

        // 6. Validate Subject DN
        CsrSubjectInfo subjectInfo = extractSubjectInfo(subjectDN);
        validateSubjectDnAgainstUser(subjectInfo, authenticatedUser);

        // 7. Check for duplicate public key
        String publicKeyHash = calculatePublicKeyHash(publicKeyInfo);
        if (isDuplicatePublicKey(publicKeyHash)) {
            throw new CsrValidationException("Public key already exists in system. " +
                "Possible key reuse or replay attack.");
        }

        // 8. Validate extensions (if present)
        Attribute[] attributes = csr.getAttributes();
        validateRequestedExtensions(attributes);

        return CsrValidationResult.builder()
            .valid(true)
            .subjectDn(subjectInfo)
            .publicKeyAlgorithm(getPublicKeyAlgorithm(publicKeyInfo))
            .keySize(getKeySize(publicKeyInfo))
            .signatureAlgorithm(signatureAlgorithm)
            .publicKeyHash(publicKeyHash)
            .build();
    }

    private boolean verifySignature(PKCS10CertificationRequest csr) {
        try {
            ContentVerifierProvider verifier =
                new JcaContentVerifierProviderBuilder()
                    .build(csr.getSubjectPublicKeyInfo());
            return csr.isSignatureValid(verifier);
        } catch (Exception e) {
            return false;
        }
    }

    private void validatePublicKey(SubjectPublicKeyInfo publicKeyInfo)
            throws CsrValidationException {

        String algorithm = getPublicKeyAlgorithm(publicKeyInfo);
        int keySize = getKeySize(publicKeyInfo);

        switch (algorithm) {
            case "RSA":
                if (keySize < MIN_RSA_KEY_SIZE) {
                    throw new CsrValidationException(
                        "RSA key size must be at least " + MIN_RSA_KEY_SIZE + " bits");
                }
                break;
            case "EC":
                if (keySize < MIN_ECDSA_KEY_SIZE) {
                    throw new CsrValidationException(
                        "ECDSA key size must be at least " + MIN_ECDSA_KEY_SIZE + " bits");
                }
                break;
            default:
                throw new CsrValidationException("Unsupported key algorithm: " + algorithm);
        }
    }

    private void validateSubjectDnAgainstUser(CsrSubjectInfo csrSubject, User user)
            throws CsrValidationException {

        // Common Name validation
        String csrCn = csrSubject.getCommonName();
        if (!csrCn.equals(user.getFullName())) {
            throw new CsrValidationException(
                "CSR Common Name '" + csrCn + "' does not match user profile: "
                + user.getFullName());
        }

        // Email validation
        String csrEmail = csrSubject.getEmail();
        if (csrEmail != null && !csrEmail.equals(user.getEmail())) {
            throw new CsrValidationException(
                "CSR email '" + csrEmail + "' does not match user email: "
                + user.getEmail());
        }

        // Organization validation (if policy requires)
        String csrOrg = csrSubject.getOrganization();
        if (csrOrg != null && !isAllowedOrganization(csrOrg)) {
            throw new CsrValidationException(
                "Organization '" + csrOrg + "' is not allowed");
        }
    }

    private String calculatePublicKeyHash(SubjectPublicKeyInfo publicKeyInfo) {
        try {
            byte[] encoded = publicKeyInfo.getEncoded();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(encoded);
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate public key hash", e);
        }
    }
}
```

### 7.3 Request State Machine

```
┌──────────────┐
│   DRAFT      │  ← End entity saving work in progress
└──────┬───────┘
       │ Submit
       ▼
┌──────────────────┐
│ PENDING_         │  ← Automated validation in progress
│ VALIDATION       │
└──────┬───────────┘
       │ Validation Success
       ▼
┌──────────────────┐
│ PENDING_         │  ← Waiting for identity verification
│ IDENTITY_        │
│ VERIFICATION     │
└──────┬───────────┘
       │ Identity Verified
       ▼
┌──────────────────┐
│ PENDING_         │  ← Waiting for RA Officer review
│ APPROVAL         │
└──────┬───────────┘
       │
       ├─────────────────────────────┐
       │ Approved                    │ Rejected
       ▼                             ▼
┌──────────────────┐         ┌──────────────┐
│ APPROVED         │         │  REJECTED    │  ← Final state
└──────┬───────────┘         └──────────────┘
       │ Submit to CA
       ▼
┌──────────────────┐
│ SUBMITTED_TO_CA  │  ← Sent to CA, waiting for response
└──────┬───────────┘
       │
       ├─────────────────────────────┐
       │ CA Success                  │ CA Failed
       ▼                             ▼
┌──────────────────┐         ┌──────────────┐
│ ISSUED           │         │ CA_ERROR     │  ← CA rejected
└──────────────────┘         └──────────────┘
Final state - success         Requires review
```

---

## 8. Certificate Issuance Process

### 8.1 CA Integration Architecture

```
┌─────────────────────────────────────────────────────────────┐
│              RA-to-CA Integration                           │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌────────────┐                     ┌────────────┐         │
│  │     RA     │                     │    CA      │         │
│  │            │                     │  (Existing)│         │
│  └──────┬─────┘                     └──────┬─────┘         │
│         │                                  │               │
│         │ 1. Submit Certificate Request    │               │
│         ├─────────────────────────────────▶│               │
│         │    POST /api/certificate/issue   │               │
│         │    {csr, profile, validity}      │               │
│         │                                  │               │
│         │ 2. CA Issues Certificate         │               │
│         │◀─────────────────────────────────┤               │
│         │    200 OK                        │               │
│         │    {certificate, serial}         │               │
│         │                                  │               │
│         │ 3. Query Certificate Status      │               │
│         ├─────────────────────────────────▶│               │
│         │    GET /api/certificate/status   │               │
│         │                                  │               │
│         │ 4. Download Certificate          │               │
│         │◀─────────────────────────────────┤               │
│         │    200 OK                        │               │
│         │    {certificate_pem}             │               │
│         │                                  │               │
└─────────────────────────────────────────────────────────────┘
```

### 8.2 CA Integration Options

#### Option A: REST API Integration (Recommended)
**Advantages:**
- Language-agnostic
- Easy to implement
- Supports modern architectures
- Good for cloud deployments

**Implementation:**
```java
@Service
public class CaIntegrationService {

    @Value("${ca.api.baseUrl}")
    private String caBaseUrl;

    @Value("${ca.api.username}")
    private String caUsername;

    @Value("${ca.api.password}")
    private String caPassword;

    private final RestTemplate restTemplate;

    public CertificateIssuanceResult issueCertificate(
            CertificateRequest request, CertificateProfile profile) {

        // 1. Prepare request payload
        CaIssuanceRequest caRequest = CaIssuanceRequest.builder()
            .csr(request.getCsrPem())
            .profileName(profile.getCaProfileName())
            .validityDays(profile.getValidityDays())
            .subjectDn(request.getSubjectDn())
            .subjectAltNames(request.getSubjectAltNames())
            .keyUsage(profile.getKeyUsage())
            .extendedKeyUsage(profile.getExtendedKeyUsage())
            .build();

        // 2. Create authentication
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth(caUsername, caPassword);

        HttpEntity<CaIssuanceRequest> entity =
            new HttpEntity<>(caRequest, headers);

        try {
            // 3. Call CA API
            ResponseEntity<CaIssuanceResponse> response = restTemplate.exchange(
                caBaseUrl + "/api/v1/certificates/issue",
                HttpMethod.POST,
                entity,
                CaIssuanceResponse.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                CaIssuanceResponse caResponse = response.getBody();

                return CertificateIssuanceResult.builder()
                    .success(true)
                    .certificatePem(caResponse.getCertificate())
                    .serialNumber(caResponse.getSerialNumber())
                    .notBefore(caResponse.getNotBefore())
                    .notAfter(caResponse.getNotAfter())
                    .build();
            } else {
                throw new CaIntegrationException(
                    "CA returned non-OK status: " + response.getStatusCode());
            }

        } catch (HttpClientErrorException e) {
            // CA rejected the request
            throw new CaIntegrationException(
                "CA rejected certificate issuance: " + e.getMessage(), e);
        } catch (Exception e) {
            // Network or other error
            throw new CaIntegrationException(
                "Failed to communicate with CA: " + e.getMessage(), e);
        }
    }
}
```

#### Option B: SCEP (Simple Certificate Enrollment Protocol) - RFC 8894
**Use Case:** Device/machine certificate enrollment
**Advantages:**
- Industry standard
- Widely supported
- Automatic renewal

#### Option C: EST (Enrollment over Secure Transport) - RFC 7030
**Use Case:** Modern alternative to SCEP
**Advantages:**
- Based on HTTPS/TLS
- Simpler than SCEP
- Better security

#### Option D: CMP (Certificate Management Protocol) - RFC 4210
**Use Case:** Enterprise PKI
**Advantages:**
- Comprehensive protocol
- Supports complex scenarios
- Strong security

### 8.3 Certificate Profile Configuration

```json
{
  "profile_id": "email-certificate-v1",
  "profile_name": "Standard Email Certificate",
  "ca_profile_name": "EmailCertificateProfile",
  "description": "Certificate for email signing and encryption",
  "validity_days": 365,
  "key_usage": {
    "digital_signature": true,
    "key_encipherment": true,
    "non_repudiation": false
  },
  "extended_key_usage": [
    "email_protection",
    "client_authentication"
  ],
  "subject_dn_template": "CN=${fullName}, E=${email}, OU=${department}, O=Company Inc, C=US",
  "subject_alt_name_types": [
    "email",
    "upn"
  ],
  "auto_approval_allowed": false,
  "requires_identity_verification": true,
  "verification_level": "EMAIL_VERIFICATION",
  "max_validity_days": 730,
  "allowed_roles": ["END_ENTITY", "RA_OPERATOR"]
}
```

---

## 9. Certificate Download and Distribution

### 9.1 Certificate Download Options

#### 9.1.1 Web Portal Download

```
┌─────────────────────────────────────────────────┐
│      Certificate Download Portal               │
├─────────────────────────────────────────────────┤
│                                                 │
│  Certificate: CN=John Doe                       │
│  Serial Number: 4A:3B:2C:1D:5E:6F               │
│  Status: ISSUED                                 │
│  Issued: 2026-01-13 10:00:00 UTC               │
│  Expires: 2027-01-13 10:00:00 UTC              │
│                                                 │
│  Download Options:                              │
│  ────────────────────────────────               │
│  [ Download Certificate (PEM) ]                 │
│  [ Download Certificate (DER) ]                 │
│  [ Download Certificate Chain (PEM) ]           │
│  [ Download PKCS#7 ]                           │
│  [ Download PKCS#12 (with private key) ]       │
│                                                 │
│  Installation Guides:                           │
│  ────────────────────────────────               │
│  • Windows Certificate Store                    │
│  • macOS Keychain                              │
│  • Firefox NSS Database                         │
│  • Java Keystore                               │
│  • Email Client Configuration                   │
│                                                 │
└─────────────────────────────────────────────────┘
```

#### 9.1.2 API Download

```http
GET /api/v1/certificates/{certificate_id}/download
Authorization: Bearer <jwt-token>
Accept: application/x-pem-file

Response: 200 OK
Content-Type: application/x-pem-file
Content-Disposition: attachment; filename="certificate.pem"

-----BEGIN CERTIFICATE-----
MIIDXTCCAkWgAwIBAgIJAKoSdJCyMH...
-----END CERTIFICATE-----
```

**Download Certificate Chain:**
```http
GET /api/v1/certificates/{certificate_id}/download-chain
Authorization: Bearer <jwt-token>

Response: 200 OK
Content-Type: application/x-pem-file

-----BEGIN CERTIFICATE-----
[End Entity Certificate]
-----END CERTIFICATE-----
-----BEGIN CERTIFICATE-----
[Intermediate CA Certificate]
-----END CERTIFICATE-----
-----BEGIN CERTIFICATE-----
[Root CA Certificate]
-----END CERTIFICATE-----
```

#### 9.1.3 PKCS#12 Export (with Private Key)

**Important Security Considerations:**
- Private key must be encrypted
- User must provide strong passphrase
- Warn user about key export risks
- Log all PKCS#12 exports

```http
POST /api/v1/certificates/{certificate_id}/export-pkcs12
Authorization: Bearer <jwt-token>
Content-Type: application/json

Request:
{
  "passphrase": "StrongP@ssphrase123!",
  "include_chain": true
}

Response: 200 OK
Content-Type: application/x-pkcs12
Content-Disposition: attachment; filename="certificate.p12"

[Binary PKCS#12 data]
```

**Implementation:**
```java
@Service
public class CertificateExportService {

    public byte[] exportToPkcs12(
            Certificate certificate,
            PrivateKey privateKey,
            String passphrase,
            boolean includeChain) throws CertificateException {

        try {
            // 1. Create keystore
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(null, null);

            // 2. Prepare certificate chain
            java.security.cert.Certificate[] chain;
            if (includeChain) {
                chain = buildCertificateChain(certificate);
            } else {
                chain = new java.security.cert.Certificate[] {
                    parseCertificate(certificate.getCertificatePem())
                };
            }

            // 3. Set private key entry
            keyStore.setKeyEntry(
                "certificate",
                privateKey,
                passphrase.toCharArray(),
                chain
            );

            // 4. Export to byte array
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            keyStore.store(baos, passphrase.toCharArray());

            // 5. Audit log
            auditLogger.log(
                AuditEvent.CERTIFICATE_EXPORTED_PKCS12,
                certificate.getSerialNumber(),
                "User: " + SecurityContextHolder.getContext().getAuthentication().getName()
            );

            return baos.toByteArray();

        } catch (Exception e) {
            throw new CertificateException("Failed to export to PKCS#12", e);
        }
    }
}
```

### 9.2 Email Delivery

```java
@Service
public class CertificateDeliveryService {

    @Autowired
    private EmailService emailService;

    public void sendCertificateEmail(Certificate certificate, User user) {

        EmailMessage email = EmailMessage.builder()
            .to(user.getEmail())
            .subject("Your Certificate is Ready - " + certificate.getSubjectCn())
            .body(buildEmailBody(certificate))
            .attachments(List.of(
                new EmailAttachment(
                    "certificate.pem",
                    "application/x-pem-file",
                    certificate.getCertificatePem().getBytes()
                )
            ))
            .build();

        emailService.send(email);

        auditLogger.log(
            AuditEvent.CERTIFICATE_SENT_VIA_EMAIL,
            certificate.getSerialNumber(),
            "Sent to: " + user.getEmail()
        );
    }

    private String buildEmailBody(Certificate certificate) {
        return """
            Dear %s,

            Your certificate has been issued successfully!

            Certificate Details:
            -------------------
            Subject: %s
            Serial Number: %s
            Issued: %s
            Expires: %s

            The certificate is attached to this email in PEM format.

            You can also download your certificate from the RA portal:
            %s

            Installation Instructions:
            -------------------------
            Please refer to the installation guide for your platform:
            %s

            If you have any questions, please contact support.

            Best regards,
            PKI Team
            """.formatted(
                certificate.getSubjectCn(),
                certificate.getSubjectDn(),
                certificate.getSerialNumber(),
                certificate.getNotBefore(),
                certificate.getNotAfter(),
                getDownloadUrl(certificate),
                getInstallationGuideUrl()
            );
    }
}
```

---

## 10. Certificate Revocation

### 10.1 Revocation Workflow

```
┌────────────────────────────────────────────────────────────┐
│          Certificate Revocation Process                    │
├────────────────────────────────────────────────────────────┤
│                                                            │
│  Step 1: Revocation Request                                │
│  ───────────────────────────────────                       │
│  Initiated by:                                             │
│    • Certificate owner (End Entity)                        │
│    • RA Officer                                            │
│    • RA Admin                                              │
│    • Automated system (compromised key detection)          │
│                                                            │
│  Step 2: Revocation Reason Selection                       │
│  ───────────────────────────────────                       │
│  Reasons (RFC 5280):                                       │
│    0 - unspecified                                         │
│    1 - keyCompromise                                       │
│    2 - cACompromise                                        │
│    3 - affiliationChanged                                  │
│    4 - superseded                                          │
│    5 - cessationOfOperation                                │
│    6 - certificateHold (suspension)                        │
│    8 - removeFromCRL (reactivation)                        │
│    9 - privilegeWithdrawn                                  │
│   10 - aACompromise                                        │
│                                                            │
│  Step 3: Authorization Check                               │
│  ───────────────────────────────────                       │
│  Verify:                                                   │
│    • Requester is authorized (owner or RA staff)           │
│    • Certificate is active (not already revoked/expired)   │
│    • Revocation reason is appropriate                      │
│                                                            │
│  Step 4: RA Officer Approval (if required)                 │
│  ───────────────────────────────────                       │
│  For sensitive revocations (keyCompromise), require:       │
│    • RA Officer approval                                   │
│    • Supporting evidence/documentation                     │
│    • Incident report reference                            │
│                                                            │
│  Step 5: Submit Revocation to CA                           │
│  ───────────────────────────────────                       │
│  RA → CA API:                                              │
│    • Certificate serial number                             │
│    • Revocation reason                                     │
│    • Revocation date                                       │
│    • Invalidation date (optional)                          │
│                                                            │
│  Step 6: CA Processing                                     │
│  ───────────────────────────────────                       │
│  CA:                                                       │
│    • Marks certificate as revoked in CA database           │
│    • Adds to Certificate Revocation List (CRL)             │
│    • Updates OCSP responder                                │
│    • Returns confirmation to RA                            │
│                                                            │
│  Step 7: RA Post-Revocation Actions                        │
│  ───────────────────────────────────                       │
│  RA:                                                       │
│    • Updates certificate status to REVOKED                 │
│    • Records revocation details in database                │
│    • Sends notification to certificate owner               │
│    • Creates audit log entry                               │
│    • Alerts security team (if keyCompromise)               │
│                                                            │
└────────────────────────────────────────────────────────────┘
```

### 10.2 Revocation API

```http
POST /api/v1/certificates/{certificate_id}/revoke
Authorization: Bearer <jwt-token>
Content-Type: application/json

Request:
{
  "reason": "KEY_COMPROMISE",
  "reason_code": 1,
  "comment": "Private key potentially exposed on compromised workstation",
  "incident_reference": "INC-2026-00123",
  "invalidation_date": "2026-01-13T00:00:00Z"  // Optional
}

Response: 200 OK
{
  "revocation_id": "rev-uuid-789",
  "certificate_id": "cert-uuid-456",
  "serial_number": "4A:3B:2C:1D",
  "status": "REVOKED",
  "revoked_at": "2026-01-13T12:00:00Z",
  "reason": "KEY_COMPROMISE",
  "reason_code": 1,
  "revoked_by": "ra.officer@company.com",
  "ca_confirmation": "CA-REV-12345",
  "crl_publication_expected": "2026-01-13T13:00:00Z"
}
```

### 10.3 Revocation Implementation

```java
@Service
public class CertificateRevocationService {

    @Autowired
    private CertificateRepository certificateRepository;

    @Autowired
    private CaIntegrationService caIntegrationService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AuditLogger auditLogger;

    @Transactional
    public RevocationResult revokeCertificate(
            String certificateId,
            RevocationRequest request,
            User requestedBy) throws RevocationException {

        // 1. Retrieve certificate
        Certificate certificate = certificateRepository.findById(certificateId)
            .orElseThrow(() -> new CertificateNotFoundException(certificateId));

        // 2. Validate certificate status
        if (certificate.getStatus() == CertificateStatus.REVOKED) {
            throw new RevocationException("Certificate is already revoked");
        }

        if (certificate.getStatus() == CertificateStatus.EXPIRED) {
            throw new RevocationException("Cannot revoke expired certificate");
        }

        // 3. Authorization check
        if (!canRevokeCertificate(requestedBy, certificate)) {
            throw new UnauthorizedException(
                "User not authorized to revoke this certificate");
        }

        // 4. Validate revocation reason
        RevocationReason reason = validateRevocationReason(request.getReason());

        // 5. Check if approval required
        if (requiresApproval(reason) && !isApproved(request)) {
            throw new RevocationException(
                "Revocation requires RA Officer approval for reason: " + reason);
        }

        // 6. Submit revocation to CA
        CaRevocationResponse caResponse;
        try {
            caResponse = caIntegrationService.revokeCertificate(
                certificate.getSerialNumber(),
                reason.getCode(),
                request.getInvalidationDate()
            );
        } catch (CaIntegrationException e) {
            throw new RevocationException("CA revocation failed: " + e.getMessage(), e);
        }

        // 7. Update certificate status in RA database
        certificate.setStatus(CertificateStatus.REVOKED);
        certificate.setRevokedAt(Instant.now());
        certificate.setRevocationReason(reason);
        certificate.setRevocationComment(request.getComment());
        certificate.setRevokedBy(requestedBy.getUserId());
        certificate.setCaRevocationConfirmation(caResponse.getConfirmationId());
        certificateRepository.save(certificate);

        // 8. Create revocation record
        CertificateRevocation revocation = CertificateRevocation.builder()
            .revocationId(UUID.randomUUID().toString())
            .certificateId(certificateId)
            .serialNumber(certificate.getSerialNumber())
            .revokedAt(Instant.now())
            .revocationReason(reason)
            .reasonCode(reason.getCode())
            .comment(request.getComment())
            .incidentReference(request.getIncidentReference())
            .revokedBy(requestedBy.getUserId())
            .caConfirmation(caResponse.getConfirmationId())
            .build();
        revocationRepository.save(revocation);

        // 9. Audit log
        auditLogger.log(
            AuditEvent.CERTIFICATE_REVOKED,
            certificate.getSerialNumber(),
            Map.of(
                "reason", reason.name(),
                "reason_code", reason.getCode(),
                "revoked_by", requestedBy.getUsername(),
                "incident_ref", request.getIncidentReference()
            )
        );

        // 10. Notify certificate owner
        notificationService.sendRevocationNotification(certificate, revocation);

        // 11. Alert security team if key compromise
        if (reason == RevocationReason.KEY_COMPROMISE) {
            notificationService.alertSecurityTeam(certificate, revocation);
        }

        return RevocationResult.builder()
            .revocationId(revocation.getRevocationId())
            .certificateId(certificateId)
            .serialNumber(certificate.getSerialNumber())
            .status(CertificateStatus.REVOKED)
            .revokedAt(revocation.getRevokedAt())
            .reason(reason)
            .reasonCode(reason.getCode())
            .revokedBy(requestedBy.getEmail())
            .caConfirmation(caResponse.getConfirmationId())
            .build();
    }

    private boolean canRevokeCertificate(User user, Certificate certificate) {
        // Owner can revoke own certificate
        if (certificate.getOwnerUserId().equals(user.getUserId())) {
            return true;
        }

        // RA Officer/Admin can revoke any certificate
        return user.hasAnyRole("RA_OFFICER", "RA_ADMIN");
    }

    private boolean requiresApproval(RevocationReason reason) {
        // Key compromise requires RA Officer approval
        return reason == RevocationReason.KEY_COMPROMISE ||
               reason == RevocationReason.CA_COMPROMISE;
    }
}
```

### 10.4 Certificate Suspension (Hold)

**Purpose**: Temporarily suspend certificate without permanent revocation

**Use Cases:**
- Employee on leave
- Investigation of potential compromise
- Temporary account deactivation

```http
POST /api/v1/certificates/{certificate_id}/suspend
Authorization: Bearer <jwt-token>
Content-Type: application/json

Request:
{
  "reason": "CERTIFICATE_HOLD",
  "comment": "Employee on extended leave",
  "expected_reactivation_date": "2026-03-01T00:00:00Z"
}

Response: 200 OK
{
  "certificate_id": "cert-uuid-456",
  "status": "SUSPENDED",
  "suspended_at": "2026-01-13T12:00:00Z",
  "can_reactivate": true,
  "expected_reactivation": "2026-03-01T00:00:00Z"
}
```

**Reactivation:**
```http
POST /api/v1/certificates/{certificate_id}/reactivate
Authorization: Bearer <jwt-token>

Response: 200 OK
{
  "certificate_id": "cert-uuid-456",
  "status": "ACTIVE",
  "reactivated_at": "2026-03-01T09:00:00Z",
  "reactivated_by": "ra.officer@company.com"
}
```

---

## 11. Certificate Renewal

### 11.1 Renewal Types

**Type A: Same Key Renewal (Recertification)**
- Reuse existing public key
- Issue new certificate with same key
- Faster process
- Risk: If old key compromised, new cert also compromised

**Type B: New Key Renewal (Rekeying)**
- Generate new key pair
- Submit new CSR
- More secure
- Recommended approach

### 11.2 Renewal Workflow

```
┌────────────────────────────────────────────────────────────┐
│          Certificate Renewal Process                       │
├────────────────────────────────────────────────────────────┤
│                                                            │
│  Renewal Window:                                           │
│  ──────────────                                            │
│  • Starts: 60 days before expiration                       │
│  • Grace period: 30 days after expiration                  │
│                                                            │
│  Step 1: Renewal Notification                              │
│  ───────────────────────────                               │
│  Automated emails sent at:                                 │
│    • 60 days before expiration                             │
│    • 30 days before expiration                             │
│    • 14 days before expiration                             │
│    • 7 days before expiration                              │
│    • 1 day before expiration                               │
│                                                            │
│  Step 2: End Entity Initiates Renewal                      │
│  ───────────────────────────                               │
│  Option A: Same Key Renewal                                │
│    • Click "Renew Certificate" in portal                   │
│    • Select "Reuse existing key"                           │
│    • Confirm renewal request                               │
│    • RA submits to CA with existing public key             │
│                                                            │
│  Option B: New Key Renewal                                 │
│    • Generate new key pair locally                         │
│    • Create new CSR                                        │
│    • Upload CSR via "Renew Certificate" workflow           │
│    • RA validates new CSR                                  │
│    • RA submits to CA                                      │
│                                                            │
│  Step 3: Renewal Approval                                  │
│  ───────────────────────────                               │
│  • Renewals may have simplified approval:                  │
│    - Auto-approve if within policy                         │
│    - No identity re-verification required                  │
│    - RA Officer approval for high-value certs              │
│                                                            │
│  Step 4: Certificate Issuance                              │
│  ───────────────────────────                               │
│  • CA issues new certificate                               │
│  • New serial number                                       │
│  • New validity period                                     │
│  • Same subject DN (typically)                             │
│                                                            │
│  Step 5: Old Certificate Handling                          │
│  ───────────────────────────                               │
│  Option A: Keep old cert active (overlap period)           │
│    • Both certificates valid for transition                │
│    • Auto-revoke old cert after N days                     │
│                                                            │
│  Option B: Immediate revocation                            │
│    • Revoke old certificate immediately                    │
│    • Reason: superseded                                    │
│                                                            │
└────────────────────────────────────────────────────────────┘
```

### 11.3 Renewal API

```http
POST /api/v1/certificates/{certificate_id}/renew
Authorization: Bearer <jwt-token>
Content-Type: application/json

Request (Same Key):
{
  "renewal_type": "SAME_KEY",
  "validity_days": 365,
  "revoke_old_certificate": false,
  "overlap_days": 30
}

Request (New Key):
{
  "renewal_type": "NEW_KEY",
  "csr": "-----BEGIN CERTIFICATE REQUEST-----\n...\n-----END CERTIFICATE REQUEST-----",
  "validity_days": 365,
  "revoke_old_certificate": true
}

Response: 201 Created
{
  "renewal_id": "ren-uuid-123",
  "old_certificate_id": "cert-uuid-456",
  "new_certificate_id": "cert-uuid-789",
  "renewal_type": "NEW_KEY",
  "status": "PENDING_APPROVAL",
  "submitted_at": "2026-01-13T10:00:00Z",
  "old_certificate_handling": {
    "revoke_after_issuance": true,
    "overlap_period_days": 0
  }
}
```

### 11.4 Automatic Renewal Reminders

```java
@Service
public class CertificateRenewalReminderService {

    @Autowired
    private CertificateRepository certificateRepository;

    @Autowired
    private NotificationService notificationService;

    @Scheduled(cron = "0 0 2 * * *") // Daily at 2 AM
    public void sendRenewalReminders() {

        Instant now = Instant.now();

        // Find certificates expiring in 60, 30, 14, 7, 1 days
        List<Integer> reminderDays = List.of(60, 30, 14, 7, 1);

        for (Integer days : reminderDays) {
            Instant expirationDate = now.plus(days, ChronoUnit.DAYS);

            List<Certificate> expiringCerts = certificateRepository
                .findExpiringOn(expirationDate);

            for (Certificate cert : expiringCerts) {
                sendRenewalReminder(cert, days);
            }
        }
    }

    private void sendRenewalReminder(Certificate certificate, int daysUntilExpiration) {

        EmailMessage email = EmailMessage.builder()
            .to(certificate.getOwnerEmail())
            .subject("Certificate Expiring in " + daysUntilExpiration + " Days")
            .body(buildRenewalReminderEmail(certificate, daysUntilExpiration))
            .build();

        notificationService.send(email);

        auditLogger.log(
            AuditEvent.RENEWAL_REMINDER_SENT,
            certificate.getSerialNumber(),
            "Days until expiration: " + daysUntilExpiration
        );
    }
}
```

---

## 12. Audit and Logging

### 12.1 Audit Events

**Complete list of auditable events:**

| Event | Description | Criticality |
|-------|-------------|-------------|
| `USER_LOGIN` | User authentication | Medium |
| `USER_LOGOUT` | User logout | Low |
| `USER_LOGIN_FAILED` | Failed authentication attempt | High |
| `USER_REGISTERED` | New user account created | Medium |
| `USER_MODIFIED` | User account modified | Medium |
| `USER_DELETED` | User account deleted | High |
| `USER_ROLE_ASSIGNED` | Role assigned to user | High |
| `CSR_SUBMITTED` | Certificate request submitted | Medium |
| `CSR_VALIDATED` | CSR validation completed | Low |
| `CSR_VALIDATION_FAILED` | CSR validation failed | Medium |
| `REQUEST_APPROVED` | Certificate request approved | High |
| `REQUEST_REJECTED` | Certificate request rejected | Medium |
| `CERTIFICATE_ISSUED` | Certificate issued by CA | High |
| `CERTIFICATE_DOWNLOADED` | Certificate downloaded | Low |
| `CERTIFICATE_EXPORTED_PKCS12` | Certificate exported with private key | High |
| `CERTIFICATE_REVOKED` | Certificate revoked | Critical |
| `CERTIFICATE_SUSPENDED` | Certificate suspended | High |
| `CERTIFICATE_REACTIVATED` | Certificate reactivated | High |
| `CERTIFICATE_RENEWED` | Certificate renewed | Medium |
| `PROFILE_CREATED` | Certificate profile created | High |
| `PROFILE_MODIFIED` | Certificate profile modified | High |
| `PROFILE_DELETED` | Certificate profile deleted | High |
| `CONFIG_CHANGED` | System configuration changed | Critical |
| `CA_INTEGRATION_SUCCESS` | Successful CA API call | Low |
| `CA_INTEGRATION_FAILED` | Failed CA API call | High |
| `AUDIT_LOG_EXPORTED` | Audit log exported | Medium |

### 12.2 Audit Log Structure

```java
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    private String logId;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(nullable = false, length = 100)
    @Enumerated(EnumType.STRING)
    private AuditEvent event;

    @Column(length = 50)
    private String resourceType;  // USER, CERTIFICATE, REQUEST, CONFIG

    @Column(length = 255)
    private String resourceId;

    @Column(length = 255)
    private String performedBy;  // User ID or "SYSTEM"

    @Column(length = 255)
    private String performedByUsername;

    @Column(columnDefinition = "inet")
    private InetAddress ipAddress;

    @Column(length = 1000)
    private String userAgent;

    @Column(length = 50)
    @Enumerated(EnumType.STRING)
    private AuditResult result;  // SUCCESS, FAILURE, WARNING

    @Column(length = 1000)
    private String failureReason;

    @Column(columnDefinition = "jsonb")
    private String details;  // JSON object with additional context

    @Column(length = 64)
    private String detailsHash;  // SHA-256 hash for integrity verification

    // Immutable - no setters except during creation
}
```

### 12.3 Audit Logger Implementation

```java
@Service
public class AuditLogger {

    @Autowired
    private AuditLogRepository auditLogRepository;

    public void log(AuditEvent event, String resourceId) {
        log(event, resourceId, null);
    }

    public void log(AuditEvent event, String resourceId, String message) {
        log(event, null, resourceId, message, null);
    }

    public void log(AuditEvent event, String resourceId, Map<String, Object> details) {
        log(event, null, resourceId, null, details);
    }

    public void log(
            AuditEvent event,
            String resourceType,
            String resourceId,
            String message,
            Map<String, Object> details) {

        // Get current authentication context
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String performedBy = auth != null ? auth.getName() : "SYSTEM";

        // Get HTTP request details
        HttpServletRequest request = getCurrentHttpRequest();
        InetAddress ipAddress = getClientIpAddress(request);
        String userAgent = request != null ? request.getHeader("User-Agent") : null;

        // Prepare details JSON
        Map<String, Object> detailsMap = new HashMap<>();
        if (message != null) {
            detailsMap.put("message", message);
        }
        if (details != null) {
            detailsMap.putAll(details);
        }
        String detailsJson = new ObjectMapper().writeValueAsString(detailsMap);

        // Calculate hash for integrity
        String detailsHash = calculateSHA256(detailsJson);

        // Create audit log entry
        AuditLog auditLog = AuditLog.builder()
            .logId(UUID.randomUUID().toString())
            .timestamp(Instant.now())
            .event(event)
            .resourceType(resourceType)
            .resourceId(resourceId)
            .performedBy(performedBy)
            .performedByUsername(auth != null ? ((User) auth.getPrincipal()).getUsername() : null)
            .ipAddress(ipAddress)
            .userAgent(userAgent)
            .result(AuditResult.SUCCESS)
            .details(detailsJson)
            .detailsHash(detailsHash)
            .build();

        // Save to database (asynchronous to avoid blocking main thread)
        CompletableFuture.runAsync(() -> auditLogRepository.save(auditLog));
    }

    public void logFailure(AuditEvent event, String resourceId, String failureReason) {
        // Similar to log() but with result = FAILURE
    }

    private String calculateSHA256(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
```

### 12.4 Audit Log Retention and Export

**Retention Policy:**
- Audit logs: 7 years minimum (compliance requirement)
- Immutable: Cannot be modified or deleted
- Archived: Move to cold storage after 2 years
- Encrypted: At rest and in transit

**Export API:**
```http
POST /api/v1/audit/export
Authorization: Bearer <admin-jwt-token>
Content-Type: application/json

Request:
{
  "start_date": "2026-01-01T00:00:00Z",
  "end_date": "2026-01-31T23:59:59Z",
  "event_types": ["CERTIFICATE_ISSUED", "CERTIFICATE_REVOKED"],
  "format": "CSV",  // CSV, JSON, PDF
  "include_details": true
}

Response: 200 OK
Content-Type: text/csv
Content-Disposition: attachment; filename="audit-log-2026-01.csv"

[CSV data]
```

---

## 13. Security Architecture

### 13.1 Defense in Depth

```
┌─────────────────────────────────────────────────────────────┐
│                 Security Layers                             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Layer 1: Network Security                                  │
│  ─────────────────────────────────                          │
│  • Firewall rules (only ports 443, 22 open)                 │
│  • IDS/IPS (Intrusion Detection/Prevention)                 │
│  • DDoS protection                                          │
│  • Network segmentation (DMZ, internal)                     │
│                                                             │
│  Layer 2: Transport Security                                │
│  ─────────────────────────────────                          │
│  • TLS 1.3 mandatory                                        │
│  • Strong cipher suites only                                │
│  • Certificate pinning for CA communication                 │
│  • HSTS headers                                             │
│                                                             │
│  Layer 3: Application Security                              │
│  ─────────────────────────────────                          │
│  • Input validation (all user inputs)                       │
│  • Output encoding (prevent XSS)                            │
│  • SQL injection prevention (parameterized queries)         │
│  • CSRF tokens                                              │
│  • Security headers (CSP, X-Frame-Options, etc.)            │
│                                                             │
│  Layer 4: Authentication & Authorization                    │
│  ─────────────────────────────────                          │
│  • Multi-factor authentication                              │
│  • Strong password policy                                   │
│  • JWT token security                                       │
│  • Role-based access control (RBAC)                         │
│  • Session management                                       │
│                                                             │
│  Layer 5: Data Security                                     │
│  ─────────────────────────────────                          │
│  • Encryption at rest (AES-256)                             │
│  • Database encryption (TDE)                                │
│  • Sensitive data masking                                   │
│  • Secure key management                                    │
│                                                             │
│  Layer 6: Monitoring & Auditing                             │
│  ─────────────────────────────────                          │
│  • Comprehensive audit logging                              │
│  • Real-time security monitoring                            │
│  • Anomaly detection                                        │
│  • Incident response procedures                             │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 13.2 Threat Model

**STRIDE Analysis:**

| Threat | Mitigation |
|--------|-----------|
| **Spoofing** | MFA, certificate-based authentication, JWT tokens |
| **Tampering** | TLS, input validation, audit log integrity checks |
| **Repudiation** | Comprehensive audit logs, digital signatures |
| **Information Disclosure** | Encryption (at rest and in transit), access controls |
| **Denial of Service** | Rate limiting, load balancing, DDoS protection |
| **Elevation of Privilege** | RBAC, principle of least privilege, permission checks |

### 13.3 Security Controls

#### 13.3.1 Input Validation

```java
@Component
public class SecurityValidator {

    // Prevent SQL injection
    public String validateInput(String input, InputType type) {
        switch (type) {
            case EMAIL:
                if (!input.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
                    throw new ValidationException("Invalid email format");
                }
                break;
            case USERNAME:
                if (!input.matches("^[a-z0-9._-]{5,30}$")) {
                    throw new ValidationException("Invalid username format");
                }
                break;
            case CERTIFICATE_ID:
                if (!input.matches("^[a-f0-9-]{36}$")) {
                    throw new ValidationException("Invalid certificate ID format");
                }
                break;
        }
        return input;
    }

    // Prevent XSS
    public String sanitizeHtml(String html) {
        return Jsoup.clean(html, Safelist.basic());
    }
}
```

#### 13.3.2 Rate Limiting

```java
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final LoadingCache<String, AtomicInteger> requestCounts =
        CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build(new CacheLoader<String, AtomicInteger>() {
                @Override
                public AtomicInteger load(String key) {
                    return new AtomicInteger(0);
                }
            });

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String clientId = getClientIdentifier(request);
        AtomicInteger count = requestCounts.get(clientId);

        if (count.incrementAndGet() > 100) { // 100 requests per minute
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("{\"error\":\"Rate limit exceeded\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
```

---

## 14. Database Design

### 14.1 Entity Relationship Diagram

```
┌──────────────────┐       ┌──────────────────┐
│      users       │──────<│   user_roles     │
└──────────────────┘       └──────────────────┘
         │                          │
         │                          │
         │                  ┌──────────────────┐
         │                  │      roles       │
         │                  └──────────────────┘
         │
         │ 1:N
         │
         ▼
┌──────────────────┐
│ certificate_     │
│   requests       │
└──────────────────┘
         │ 1:1
         ▼
┌──────────────────┐       ┌──────────────────┐
│  certificates    │──────<│   revocations    │
└──────────────────┘       └──────────────────┘
         │
         │ N:1
         │
         ▼
┌──────────────────┐
│ certificate_     │
│   profiles       │
└──────────────────┘

┌──────────────────┐
│   audit_logs     │  (Independent)
└──────────────────┘
```

### 14.2 Core Tables

```sql
-- Users table
CREATE TABLE users (
    user_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(255) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    full_name VARCHAR(200) NOT NULL,
    mobile_number VARCHAR(20),
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,  -- ACTIVE, DISABLED, LOCKED
    mfa_enabled BOOLEAN DEFAULT FALSE,
    mfa_type VARCHAR(20),  -- SMS_OTP, TOTP, HARDWARE_TOKEN
    totp_secret VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    last_login_at TIMESTAMP,
    failed_login_attempts INTEGER DEFAULT 0,
    locked_until TIMESTAMP,
    CONSTRAINT check_status CHECK (status IN ('ACTIVE', 'DISABLED', 'LOCKED'))
);

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);

-- User roles
CREATE TABLE user_roles (
    user_role_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    role_name VARCHAR(50) NOT NULL,
    assigned_at TIMESTAMP NOT NULL DEFAULT NOW(),
    assigned_by UUID REFERENCES users(user_id),
    CONSTRAINT unique_user_role UNIQUE (user_id, role_name),
    CONSTRAINT check_role CHECK (role_name IN ('END_ENTITY', 'RA_OPERATOR', 'RA_OFFICER', 'RA_ADMIN', 'AUDITOR'))
);

CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);

-- Certificate profiles
CREATE TABLE certificate_profiles (
    profile_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_name VARCHAR(255) UNIQUE NOT NULL,
    ca_profile_name VARCHAR(255) NOT NULL,
    description TEXT,
    validity_days INTEGER NOT NULL,
    max_validity_days INTEGER,
    key_algorithm VARCHAR(50) NOT NULL,  -- RSA, EC
    min_key_size INTEGER NOT NULL,
    signature_algorithm VARCHAR(50) NOT NULL,
    key_usage JSONB NOT NULL,  -- {digital_signature: true, key_encipherment: true, ...}
    extended_key_usage TEXT[],  -- Array of OIDs
    subject_dn_template VARCHAR(500),
    san_types TEXT[],  -- [email, dns, upn]
    auto_approval_allowed BOOLEAN DEFAULT FALSE,
    requires_identity_verification BOOLEAN DEFAULT TRUE,
    verification_level VARCHAR(50),  -- NONE, EMAIL, DOCUMENT, FACE_TO_FACE
    allowed_roles TEXT[],
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP
);

-- Certificate requests
CREATE TABLE certificate_requests (
    request_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(user_id),
    profile_id UUID NOT NULL REFERENCES certificate_profiles(profile_id),
    submitted_by_user_id UUID REFERENCES users(user_id),  -- For RA Operator submissions
    csr_pem TEXT NOT NULL,
    csr_subject_dn VARCHAR(500) NOT NULL,
    csr_public_key_hash VARCHAR(64) UNIQUE NOT NULL,  -- SHA-256 hash for duplicate detection
    public_key_algorithm VARCHAR(50) NOT NULL,
    key_size INTEGER NOT NULL,
    signature_algorithm VARCHAR(50) NOT NULL,
    subject_dn_verified BOOLEAN DEFAULT FALSE,
    proof_of_possession_verified BOOLEAN DEFAULT FALSE,
    additional_emails TEXT[],
    justification TEXT,
    status VARCHAR(50) NOT NULL,  -- DRAFT, PENDING_VALIDATION, PENDING_APPROVAL, APPROVED, REJECTED, ISSUED, CA_ERROR
    validation_errors JSONB,
    identity_verification_method VARCHAR(50),
    identity_verified_at TIMESTAMP,
    identity_verified_by UUID REFERENCES users(user_id),
    approved_by UUID REFERENCES users(user_id),
    approved_at TIMESTAMP,
    rejected_by UUID REFERENCES users(user_id),
    rejected_at TIMESTAMP,
    rejection_reason TEXT,
    ca_submission_id VARCHAR(255),  -- CA's request ID
    ca_submitted_at TIMESTAMP,
    submitted_at TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP,
    ip_address INET,
    user_agent TEXT,
    CONSTRAINT check_request_status CHECK (status IN ('DRAFT', 'PENDING_VALIDATION', 'PENDING_IDENTITY_VERIFICATION', 'PENDING_APPROVAL', 'APPROVED', 'REJECTED', 'SUBMITTED_TO_CA', 'ISSUED', 'CA_ERROR'))
);

CREATE INDEX idx_requests_user_id ON certificate_requests(user_id);
CREATE INDEX idx_requests_status ON certificate_requests(status);
CREATE INDEX idx_requests_submitted_at ON certificate_requests(submitted_at);

-- Certificates
CREATE TABLE certificates (
    certificate_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id UUID REFERENCES certificate_requests(request_id),
    user_id UUID NOT NULL REFERENCES users(user_id),
    profile_id UUID NOT NULL REFERENCES certificate_profiles(profile_id),
    certificate_pem TEXT NOT NULL,
    serial_number VARCHAR(100) UNIQUE NOT NULL,
    subject_dn VARCHAR(500) NOT NULL,
    subject_cn VARCHAR(255) NOT NULL,
    issuer_dn VARCHAR(500) NOT NULL,
    public_key_hash VARCHAR(64) NOT NULL,
    not_before TIMESTAMP NOT NULL,
    not_after TIMESTAMP NOT NULL,
    key_usage TEXT[],
    extended_key_usage TEXT[],
    subject_alt_names TEXT[],
    status VARCHAR(50) NOT NULL,  -- ACTIVE, REVOKED, EXPIRED, SUSPENDED
    issued_at TIMESTAMP NOT NULL DEFAULT NOW(),
    revoked_at TIMESTAMP,
    revocation_reason VARCHAR(50),
    revocation_reason_code INTEGER,
    revocation_comment TEXT,
    revoked_by UUID REFERENCES users(user_id),
    ca_revocation_confirmation VARCHAR(255),
    suspended_at TIMESTAMP,
    suspended_by UUID REFERENCES users(user_id),
    reactivated_at TIMESTAMP,
    reactivated_by UUID REFERENCES users(user_id),
    download_count INTEGER DEFAULT 0,
    last_downloaded_at TIMESTAMP,
    CONSTRAINT check_cert_status CHECK (status IN ('ACTIVE', 'REVOKED', 'EXPIRED', 'SUSPENDED'))
);

CREATE INDEX idx_certificates_user_id ON certificates(user_id);
CREATE INDEX idx_certificates_serial_number ON certificates(serial_number);
CREATE INDEX idx_certificates_status ON certificates(status);
CREATE INDEX idx_certificates_not_after ON certificates(not_after);

-- Certificate revocations
CREATE TABLE certificate_revocations (
    revocation_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    certificate_id UUID NOT NULL REFERENCES certificates(certificate_id),
    serial_number VARCHAR(100) NOT NULL,
    revoked_at TIMESTAMP NOT NULL DEFAULT NOW(),
    revocation_reason VARCHAR(50) NOT NULL,
    reason_code INTEGER NOT NULL,
    comment TEXT,
    incident_reference VARCHAR(255),
    invalidation_date TIMESTAMP,
    revoked_by UUID NOT NULL REFERENCES users(user_id),
    revoked_by_username VARCHAR(255),
    ca_confirmation VARCHAR(255),
    crl_publication_date TIMESTAMP
);

CREATE INDEX idx_revocations_certificate_id ON certificate_revocations(certificate_id);
CREATE INDEX idx_revocations_serial_number ON certificate_revocations(serial_number);

-- Audit logs
CREATE TABLE audit_logs (
    log_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    timestamp TIMESTAMP NOT NULL DEFAULT NOW(),
    event VARCHAR(100) NOT NULL,
    resource_type VARCHAR(50),
    resource_id VARCHAR(255),
    performed_by VARCHAR(255),
    performed_by_username VARCHAR(255),
    ip_address INET,
    user_agent TEXT,
    result VARCHAR(50) NOT NULL,  -- SUCCESS, FAILURE, WARNING
    failure_reason TEXT,
    details JSONB,
    details_hash VARCHAR(64),  -- SHA-256 for integrity verification
    CONSTRAINT check_audit_result CHECK (result IN ('SUCCESS', 'FAILURE', 'WARNING'))
);

CREATE INDEX idx_audit_timestamp ON audit_logs(timestamp);
CREATE INDEX idx_audit_event ON audit_logs(event);
CREATE INDEX idx_audit_performed_by ON audit_logs(performed_by);
CREATE INDEX idx_audit_resource_id ON audit_logs(resource_id);

-- Certificate renewals
CREATE TABLE certificate_renewals (
    renewal_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    old_certificate_id UUID NOT NULL REFERENCES certificates(certificate_id),
    new_certificate_id UUID REFERENCES certificates(certificate_id),
    renewal_request_id UUID REFERENCES certificate_requests(request_id),
    renewal_type VARCHAR(50) NOT NULL,  -- SAME_KEY, NEW_KEY
    initiated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    initiated_by UUID NOT NULL REFERENCES users(user_id),
    completed_at TIMESTAMP,
    status VARCHAR(50) NOT NULL,  -- PENDING, COMPLETED, FAILED
    revoke_old_certificate BOOLEAN DEFAULT FALSE,
    old_certificate_revoked_at TIMESTAMP,
    overlap_days INTEGER DEFAULT 0
);
```

---

## 15. API Specifications

### 15.1 RESTful API Design Principles

**Base URL:** `https://ra.company.com/api/v1`

**Authentication:** JWT Bearer token in Authorization header

**Versioning:** URL path versioning (`/api/v1`, `/api/v2`)

**Response Format:** JSON

**Error Handling:** Standard HTTP status codes + error details

### 15.2 Complete API Endpoint List

#### Authentication Endpoints

```http
POST   /api/v1/auth/login                  # User login
POST   /api/v1/auth/logout                 # User logout
POST   /api/v1/auth/refresh-token          # Refresh JWT token
POST   /api/v1/auth/mfa/setup              # Setup MFA
POST   /api/v1/auth/mfa/verify             # Verify MFA code
POST   /api/v1/auth/password/change        # Change password
POST   /api/v1/auth/password/reset         # Reset password
```

#### Certificate Request Endpoints

```http
POST   /api/v1/certificates/requests                    # Submit CSR
GET    /api/v1/certificates/requests                    # List requests
GET    /api/v1/certificates/requests/{id}               # Get request details
PUT    /api/v1/certificates/requests/{id}               # Update request (before approval)
DELETE /api/v1/certificates/requests/{id}               # Delete draft request
POST   /api/v1/certificates/requests/{id}/approve       # Approve request
POST   /api/v1/certificates/requests/{id}/reject        # Reject request
GET    /api/v1/certificates/requests/{id}/history       # Get request history
POST   /api/v1/certificates/requests/{id}/documents     # Upload supporting documents
GET    /api/v1/certificates/requests/{id}/documents     # List uploaded documents
```

#### Certificate Management Endpoints

```http
GET    /api/v1/certificates                             # List certificates
GET    /api/v1/certificates/{id}                        # Get certificate details
GET    /api/v1/certificates/{id}/download               # Download certificate (PEM)
GET    /api/v1/certificates/{id}/download-chain         # Download certificate chain
POST   /api/v1/certificates/{id}/export-pkcs12          # Export as PKCS#12
POST   /api/v1/certificates/{id}/revoke                 # Revoke certificate
POST   /api/v1/certificates/{id}/suspend                # Suspend certificate
POST   /api/v1/certificates/{id}/reactivate             # Reactivate certificate
POST   /api/v1/certificates/{id}/renew                  # Renew certificate
GET    /api/v1/certificates/{id}/revocation-status      # Check revocation status
GET    /api/v1/certificates/expiring                    # List expiring certificates
```

#### User Management Endpoints (Admin only)

```http
GET    /api/v1/users                        # List users
GET    /api/v1/users/{id}                   # Get user details
POST   /api/v1/users                        # Create user
PUT    /api/v1/users/{id}                   # Update user
DELETE /api/v1/users/{id}                   # Delete user
POST   /api/v1/users/{id}/roles             # Assign role
DELETE /api/v1/users/{id}/roles/{role}      # Remove role
POST   /api/v1/users/{id}/disable           # Disable user
POST   /api/v1/users/{id}/enable            # Enable user
POST   /api/v1/users/{id}/unlock            # Unlock user account
```

#### Certificate Profile Endpoints (Admin only)

```http
GET    /api/v1/profiles                     # List profiles
GET    /api/v1/profiles/{id}                # Get profile details
POST   /api/v1/profiles                     # Create profile
PUT    /api/v1/profiles/{id}                # Update profile
DELETE /api/v1/profiles/{id}                # Delete profile
POST   /api/v1/profiles/{id}/activate       # Activate profile
POST   /api/v1/profiles/{id}/deactivate     # Deactivate profile
```

#### Audit and Reporting Endpoints

```http
GET    /api/v1/audit/logs                   # List audit logs
GET    /api/v1/audit/logs/{id}              # Get audit log details
POST   /api/v1/audit/export                 # Export audit logs
GET    /api/v1/reports/certificates         # Certificate report
GET    /api/v1/reports/requests             # Request report
GET    /api/v1/reports/revocations          # Revocation report
GET    /api/v1/reports/users                # User report
```

#### System Endpoints

```http
GET    /api/v1/health                       # Health check
GET    /api/v1/status                       # System status
GET    /api/v1/version                      # API version info
GET    /api/v1/config                       # System configuration (admin only)
PUT    /api/v1/config                       # Update configuration (admin only)
```

### 15.3 API Error Response Format

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "CSR validation failed",
    "details": [
      {
        "field": "csr",
        "issue": "Invalid signature - proof of possession failed"
      }
    ],
    "request_id": "req-uuid-123",
    "timestamp": "2026-01-13T10:00:00Z",
    "documentation_url": "https://docs.ra.company.com/errors/VALIDATION_ERROR"
  }
}
```

**Standard Error Codes:**
- `AUTHENTICATION_REQUIRED` (401)
- `AUTHORIZATION_FAILED` (403)
- `RESOURCE_NOT_FOUND` (404)
- `VALIDATION_ERROR` (400)
- `DUPLICATE_RESOURCE` (409)
- `RATE_LIMIT_EXCEEDED` (429)
- `INTERNAL_SERVER_ERROR` (500)
- `CA_INTEGRATION_ERROR` (502)
- `SERVICE_UNAVAILABLE` (503)

---

## 16. Integration with CA

### 16.1 CA Communication Protocol

**Recommended:** REST API over HTTPS

**Alternative Options:**
- SCEP (Simple Certificate Enrollment Protocol)
- EST (Enrollment over Secure Transport)
- CMP (Certificate Management Protocol)
- Custom protocol

### 16.2 CA API Requirements

**Minimum CA API Capabilities:**

1. **Certificate Issuance**
   ```http
   POST /ca/api/certificates/issue
   Content-Type: application/json
   Authorization: Basic <ca-credentials>

   Request:
   {
     "csr": "-----BEGIN CERTIFICATE REQUEST-----\n...\n-----END CERTIFICATE REQUEST-----",
     "profile_name": "EmailCertificateProfile",
     "validity_days": 365,
     "subject_dn": "CN=John Doe, E=john@company.com, OU=Engineering, O=Company, C=US",
     "subject_alt_names": ["email:john@company.com", "email:jdoe@company.com"],
     "extensions": {
       "key_usage": ["digitalSignature", "keyEncipherment"],
       "extended_key_usage": ["emailProtection", "clientAuth"]
     }
   }

   Response: 200 OK
   {
     "certificate": "-----BEGIN CERTIFICATE-----\n...\n-----END CERTIFICATE-----",
     "serial_number": "4A3B2C1D5E6F7890",
     "not_before": "2026-01-13T10:00:00Z",
     "not_after": "2027-01-13T10:00:00Z",
     "issuer_dn": "CN=Company CA, O=Company Inc, C=US",
     "ca_request_id": "CA-REQ-12345"
   }
   ```

2. **Certificate Revocation**
   ```http
   POST /ca/api/certificates/revoke
   Content-Type: application/json
   Authorization: Basic <ca-credentials>

   Request:
   {
     "serial_number": "4A3B2C1D5E6F7890",
     "reason_code": 1,  // keyCompromise
     "revocation_date": "2026-01-13T12:00:00Z",
     "invalidation_date": "2026-01-13T00:00:00Z"  // Optional
   }

   Response: 200 OK
   {
     "status": "REVOKED",
     "revoked_at": "2026-01-13T12:00:00Z",
     "crl_update_scheduled": "2026-01-13T13:00:00Z",
     "confirmation_id": "CA-REV-67890"
   }
   ```

3. **Certificate Status Query**
   ```http
   GET /ca/api/certificates/{serial_number}/status
   Authorization: Basic <ca-credentials>

   Response: 200 OK
   {
     "serial_number": "4A3B2C1D5E6F7890",
     "status": "ACTIVE",  // ACTIVE, REVOKED, EXPIRED, SUSPENDED
     "not_before": "2026-01-13T10:00:00Z",
     "not_after": "2027-01-13T10:00:00Z",
     "revocation_info": null
   }
   ```

4. **CRL Retrieval**
   ```http
   GET /ca/api/crl/latest
   Authorization: Basic <ca-credentials>

   Response: 200 OK
   Content-Type: application/pkix-crl

   [Binary CRL data]
   ```

### 16.3 CA Integration Configuration

```yaml
# application.yml
ca:
  integration:
    type: REST_API  # REST_API, SCEP, EST, CMP
    base_url: https://ca.company.com/api/v1
    authentication:
      type: BASIC  # BASIC, CERTIFICATE, API_KEY
      username: ${CA_API_USERNAME}
      password: ${CA_API_PASSWORD}
    ssl:
      certificate_validation: true
      trusted_certificates:
        - /etc/ra/ca-trust/ca-cert.pem
      client_certificate: /etc/ra/certs/ra-client.pem
      client_key: /etc/ra/private/ra-client-key.pem
    timeout:
      connect: 5000  # milliseconds
      read: 30000
    retry:
      max_attempts: 3
      backoff_multiplier: 2
    rate_limiting:
      requests_per_minute: 100
```

---

## 17. User Interface Requirements

### 17.1 End Entity Portal

**Key Features:**
- Dashboard with certificate inventory
- Submit certificate request wizard
- Upload PKCS#10 CSR
- Track request status
- Download certificates
- Renew certificates
- Request revocation
- View notifications

**UI Mockup - Dashboard:**
```
┌────────────────────────────────────────────────────────────────┐
│ RA Portal - Dashboard              kablu@company.com  [Logout] │
├────────────────────────────────────────────────────────────────┤
│                                                                │
│  My Certificates                                    [Request]  │
│  ──────────────────────────────────────────────────────────    │
│                                                                │
│  ┌──────────────────────────────────────────────────────────┐ │
│  │ CN=Kablu Ahmed                        Status: ✓ Active   │ │
│  │ Serial: 4A:3B:2C:1D                                      │ │
│  │ Expires: 2027-01-13 (365 days)        [Download] [Renew]│ │
│  └──────────────────────────────────────────────────────────┘ │
│                                                                │
│  ┌──────────────────────────────────────────────────────────┐ │
│  │ CN=Kablu Ahmed (Code Signing)         Status: ⚠ Expiring│ │
│  │ Serial: 5B:4C:3D:2E                                      │ │
│  │ Expires: 2026-02-15 (33 days)         [Download] [Renew]│ │
│  └──────────────────────────────────────────────────────────┘ │
│                                                                │
│  Pending Requests                                              │
│  ──────────────────────────────────────────────────────────    │
│                                                                │
│  ┌──────────────────────────────────────────────────────────┐ │
│  │ Request ID: REQ-2026-001              Status: Pending    │ │
│  │ Type: Email Certificate                                  │ │
│  │ Submitted: 2026-01-13 10:00                              │ │
│  │ Next Step: RA Officer Approval        [View Details]    │ │
│  └──────────────────────────────────────────────────────────┘ │
│                                                                │
│  Recent Activity                                               │
│  ──────────────────────────────────────────────────────────    │
│  • Certificate REQ-2026-001 submitted (2 hours ago)           │
│  • Certificate 4A:3B:2C:1D downloaded (1 day ago)             │
│  • Renewal reminder sent for 5B:4C:3D:2E (3 days ago)         │
│                                                                │
└────────────────────────────────────────────────────────────────┘
```

### 17.2 RA Officer Portal

**Key Features:**
- Pending requests queue
- Request review interface
- Approve/reject with comments
- View request history
- Certificate search
- Revocation management
- Generate reports

**UI Mockup - Request Review:**
```
┌────────────────────────────────────────────────────────────────┐
│ RA Officer Portal - Request Review     officer@company.com    │
├────────────────────────────────────────────────────────────────┤
│                                                                │
│  ← Back to Queue                                              │
│                                                                │
│  Certificate Request Details                                   │
│  ══════════════════════════════════════════════════════════    │
│                                                                │
│  Request ID: REQ-2026-001                                      │
│  Status: Pending Approval                                      │
│  Submitted: 2026-01-13 10:00:00 UTC                           │
│  Submitted by: kablu@company.com (End Entity)                 │
│                                                                │
│  Certificate Information                                       │
│  ──────────────────────────────────────────────────────────    │
│  Profile: Email Certificate                                    │
│  Subject DN: CN=Kablu Ahmed, E=kablu@company.com,             │
│             OU=Engineering, O=Company Inc, C=US                │
│  Key Type: RSA 2048                                           │
│  Validity: 365 days                                           │
│                                                                │
│  Identity Verification                                         │
│  ──────────────────────────────────────────────────────────    │
│  ✓ Email verified (2026-01-13 10:05)                          │
│  ✓ Subject DN matches user profile                            │
│  ✓ Proof of possession verified                               │
│                                                                │
│  Validation Results                                            │
│  ──────────────────────────────────────────────────────────    │
│  ✓ CSR format valid (PKCS#10)                                 │
│  ✓ CSR signature valid                                        │
│  ✓ Key size meets policy (2048 >= 2048)                       │
│  ✓ No duplicate public key found                              │
│  ✓ Certificate quota not exceeded (2/10)                      │
│                                                                │
│  Justification                                                 │
│  ──────────────────────────────────────────────────────────    │
│  "Email signing and encryption certificate for secure          │
│   business communication with external partners"               │
│                                                                │
│  CSR Details                                                   │
│  ──────────────────────────────────────────────────────────    │
│  -----BEGIN CERTIFICATE REQUEST-----                           │
│  MIICvDCCAaQCAQAwdzELMAkGA1UEBhMCVVMxEzARBgNVBAgMCk...       │
│  [View Full CSR]                                               │
│                                                                │
│  Decision                                                      │
│  ──────────────────────────────────────────────────────────    │
│  Comments: [Optional - provide feedback to requester_______]   │
│                                                                │
│  [✓ Approve Request]  [✗ Reject Request]  [📄 Request More Info]│
│                                                                │
└────────────────────────────────────────────────────────────────┘
```

### 17.3 RA Admin Portal

**Key Features:**
- User management
- Role assignment
- Certificate profile configuration
- System configuration
- Audit log viewer
- Report generator
- CA integration settings

---

## 18. Non-Functional Requirements

### 18.1 Performance Requirements

| Metric | Requirement | Target |
|--------|-------------|--------|
| **API Response Time** | < 500ms (95th percentile) | < 200ms (average) |
| **Page Load Time** | < 2 seconds | < 1 second |
| **Concurrent Users** | 500 minimum | 1000+ |
| **Certificate Issuance** | < 60 seconds end-to-end | < 30 seconds |
| **CSR Processing** | < 5 seconds | < 2 seconds |
| **Database Query Time** | < 100ms (95th percentile) | < 50ms (average) |
| **Throughput** | 100 certificates/hour minimum | 500 certificates/hour |

### 18.2 Scalability Requirements

- **Horizontal Scaling:** Support load balancing across multiple RA instances
- **Database Scaling:** Support read replicas and connection pooling
- **Caching:** Redis for session and frequently accessed data
- **Stateless Design:** No server-side session state (JWT-based)
- **Asynchronous Processing:** Background jobs for email, CA communication

### 18.3 Availability Requirements

- **Uptime:** 99.9% (8.76 hours downtime/year)
- **Planned Maintenance:** Off-peak hours only
- **Recovery Time Objective (RTO):** 4 hours
- **Recovery Point Objective (RPO):** 1 hour
- **Backup Frequency:** Daily full, hourly incremental
- **Disaster Recovery:** Hot standby site

### 18.4 Security Requirements

- **Authentication:** Multi-factor for RA staff (mandatory)
- **Session Timeout:** 30 minutes inactivity
- **Password Policy:** Minimum 12 characters, complexity requirements
- **Encryption at Rest:** AES-256
- **Encryption in Transit:** TLS 1.3
- **Audit Retention:** 7 years minimum
- **Vulnerability Scanning:** Monthly
- **Penetration Testing:** Annually

### 18.5 Compliance Requirements

- **RFC Compliance:** RFC 2986, 5280, 6960, 5652, 3647
- **NIST Compliance:** SP 800-63, SP 800-57
- **CA/Browser Forum:** Baseline Requirements (if applicable)
- **GDPR:** Data protection and privacy (if applicable)
- **SOC 2:** Type II compliance
- **ISO 27001:** Information security management

---

## 19. Deployment Architecture

### 19.1 Production Deployment

```
┌────────────────────────────────────────────────────────────────┐
│                     Production Environment                     │
├────────────────────────────────────────────────────────────────┤
│                                                                │
│  ┌──────────────┐                                              │
│  │  Load        │                                              │
│  │  Balancer    │                                              │
│  │  (HA Proxy)  │                                              │
│  └──────┬───────┘                                              │
│         │                                                       │
│         ├────────────────────────────┐                         │
│         │                            │                         │
│  ┌──────▼───────┐            ┌──────▼───────┐                 │
│  │  RA App      │            │  RA App      │                 │
│  │  Instance 1  │            │  Instance 2  │                 │
│  │  (Active)    │            │  (Active)    │                 │
│  └──────┬───────┘            └──────┬───────┘                 │
│         │                            │                         │
│         └──────────┬─────────────────┘                         │
│                    │                                           │
│         ┌──────────▼──────────┐                                │
│         │   PostgreSQL        │                                │
│         │   Primary + Replica │                                │
│         └──────────┬──────────┘                                │
│                    │                                           │
│         ┌──────────▼──────────┐                                │
│         │   Redis Cluster     │                                │
│         │   (Cache + Queue)   │                                │
│         └─────────────────────┘                                │
│                                                                │
│  External Integrations:                                        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐        │
│  │      CA      │  │  Email/SMS   │  │   LDAP/AD    │        │
│  │   (Existing) │  │   Gateway    │  │  (Optional)  │        │
│  └──────────────┘  └──────────────┘  └──────────────┘        │
│                                                                │
└────────────────────────────────────────────────────────────────┘
```

### 19.2 Deployment Checklist

**Pre-Deployment:**
- [ ] Infrastructure provisioned (servers, load balancer, database)
- [ ] SSL/TLS certificates installed
- [ ] Database migrations tested
- [ ] CA integration configured and tested
- [ ] Email/SMS gateways configured
- [ ] LDAP/AD integration tested (if applicable)
- [ ] Backup and recovery procedures verified
- [ ] Monitoring and alerting configured
- [ ] Security hardening completed
- [ ] Performance testing completed
- [ ] Security audit completed

**Deployment:**
- [ ] Deploy database schema
- [ ] Deploy application code
- [ ] Configure environment variables
- [ ] Start application instances
- [ ] Verify health checks
- [ ] Run smoke tests
- [ ] Enable load balancer traffic

**Post-Deployment:**
- [ ] Create bootstrap super admin account
- [ ] Register initial RA Officers and Operators
- [ ] Configure certificate profiles
- [ ] Test end-to-end certificate issuance
- [ ] Verify audit logging
- [ ] Monitor for errors
- [ ] Conduct user training

---

## 20. Testing Strategy

### 20.1 Test Types

#### Unit Tests
- **Coverage Target:** 80%+
- **Focus:** Individual components, services, utilities
- **Framework:** JUnit 5, Mockito

#### Integration Tests
- **Focus:** Component interactions, database operations, CA integration
- **Framework:** Spring Boot Test, TestContainers

#### API Tests
- **Focus:** REST API endpoints, request/response validation
- **Framework:** REST Assured, Postman/Newman

#### Security Tests
- **Focus:** Authentication, authorization, input validation, vulnerability scanning
- **Tools:** OWASP ZAP, Burp Suite, SonarQube

#### Performance Tests
- **Focus:** Load testing, stress testing, scalability
- **Tools:** JMeter, Gatling, K6

#### End-to-End Tests
- **Focus:** Complete user workflows, UI testing
- **Framework:** Selenium, Cypress

### 20.2 Test Scenarios

**Critical Test Scenarios:**

1. **Certificate Request Submission**
   - Valid CSR submission
   - Invalid CSR format
   - CSR with incorrect signature
   - CSR with mismatched Subject DN
   - Duplicate public key
   - CSR with weak key size

2. **Approval Workflow**
   - RA Officer approval
   - RA Officer rejection
   - Unauthorized approval attempt
   - Approval with missing permissions

3. **Certificate Issuance**
   - Successful CA integration
   - CA timeout handling
   - CA error response handling
   - Certificate download

4. **Certificate Revocation**
   - Owner-initiated revocation
   - RA Officer-initiated revocation
   - Revocation of already-revoked certificate
   - Revocation with keyCompromise reason

5. **Authentication & Authorization**
   - Successful login with MFA
   - Failed login attempts
   - Account lockout
   - Role-based access control
   - JWT token expiration

6. **Security**
   - SQL injection attempts
   - XSS attempts
   - CSRF attacks
   - Session hijacking
   - Brute force attacks
   - Rate limiting

---

## 21. Appendices

### Appendix A: Glossary

| Term | Definition |
|------|------------|
| **CA** | Certificate Authority - Entity that issues digital certificates |
| **RA** | Registration Authority - Entity that validates certificate requests |
| **CSR** | Certificate Signing Request - PKCS#10 formatted request |
| **PKCS#10** | Public-Key Cryptography Standards #10 - CSR format |
| **PKI** | Public Key Infrastructure |
| **DN** | Distinguished Name - X.500 name format |
| **SAN** | Subject Alternative Name - Additional identifiers in certificate |
| **CRL** | Certificate Revocation List |
| **OCSP** | Online Certificate Status Protocol |
| **HSM** | Hardware Security Module |
| **MFA** | Multi-Factor Authentication |
| **RBAC** | Role-Based Access Control |

### Appendix B: RFC References

- **RFC 2986:** PKCS #10: Certification Request Syntax
- **RFC 5280:** X.509 Public Key Infrastructure Certificate Profile
- **RFC 6960:** Online Certificate Status Protocol (OCSP)
- **RFC 5652:** Cryptographic Message Syntax (CMS)
- **RFC 3647:** Certificate Policy and Certification Practice Statement
- **RFC 7030:** Enrollment over Secure Transport (EST)
- **RFC 8894:** Simple Certificate Enrollment Protocol (SCEP)
- **RFC 4210:** Certificate Management Protocol (CMP)

### Appendix C: Security Best Practices

1. **Never store passwords in plain text** - Use bcrypt/Argon2
2. **Always validate CSR signatures** - Verify proof of possession
3. **Implement rate limiting** - Prevent abuse
4. **Use TLS 1.3** - Enforce strong encryption
5. **Enable MFA for RA staff** - Protect privileged accounts
6. **Maintain comprehensive audit logs** - 7+ years retention
7. **Perform regular security audits** - Quarterly minimum
8. **Keep software updated** - Patch vulnerabilities promptly
9. **Implement defense in depth** - Multiple security layers
10. **Follow principle of least privilege** - Minimal permissions

### Appendix D: Sample Certificate Profiles

See Section 8.3 for detailed profile configuration examples.

### Appendix E: Deployment Environments

| Environment | Purpose | URL |
|-------------|---------|-----|
| Development | Developer testing | https://ra-dev.company.com |
| QA/Test | QA testing | https://ra-test.company.com |
| Staging | Pre-production | https://ra-staging.company.com |
| Production | Live system | https://ra.company.com |

---

## Document Approval

**Prepared By:**
- Name: PKI Development Team
- Date: 2026-01-13

**Reviewed By:**
- [ ] Security Architect
- [ ] PKI Administrator
- [ ] Development Team Lead
- [ ] QA Manager

**Approved By:**
- [ ] Chief Information Security Officer (CISO)
- [ ] IT Director
- [ ] Compliance Officer

**Approval Date:** _______________

---

**Document End**

**Document Location:** D:\ecc-dev\jdk-21-poc\ra-web\docs\RA_FUNCTIONAL_DESIGN_SPECIFICATION.md

**For Questions or Clarifications:**
Contact: PKI Development Team
Email: pki-dev@company.com
