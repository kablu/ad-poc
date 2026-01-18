# Siemens Registration Authority (RA) - Functional Specification Document
## Based on CMP RA Component and LightweightCmpRa

**Document Version**: 1.0
**Date**: 2026-01-15
**Based On**: Siemens Open Source RA Products
**Source Repositories**:
- [siemens/cmp-ra-component](https://github.com/siemens/cmp-ra-component)
- [siemens/LightweightCmpRa](https://github.com/siemens/LightweightCmpRa)

**License**: Apache License 2.0
**Status**: Reference Implementation Analysis

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Product Overview](#2-product-overview)
3. [Functional Architecture](#3-functional-architecture)
4. [Core Functional Capabilities](#4-core-functional-capabilities)
5. [Protocol Support and Standards](#5-protocol-support-and-standards)
6. [Message Flows and Use Cases](#6-message-flows-and-use-cases)
7. [Configuration and Management](#7-configuration-and-management)
8. [Integration Interfaces](#8-integration-interfaces)
9. [Security Features](#9-security-features)
10. [Transport Mechanisms](#10-transport-mechanisms)
11. [Operational Characteristics](#11-operational-characteristics)
12. [Deployment Models](#12-deployment-models)
13. [Comparison with Product Requirements](#13-comparison-with-product-requirements)

---

## 1. Executive Summary

### 1.1 Product Identity
Siemens has developed two complementary open-source products for Certificate Management Protocol (CMP) based Registration Authority operations:

1. **CMP RA Component**: A generic, production-grade Java library providing core CMP RA and client functionality
2. **LightweightCmpRa**: A CLI-based proof-of-concept application demonstrating practical RA implementation

### 1.2 Key Distinguishing Features
- **Protocol Focus**: Implements Certificate Management Protocol (CMP) per RFC 9810, RFC 9483
- **Standards Compliance**: Reference implementation used during IETF CMP standardization
- **Modular Architecture**: Separation between core CMP logic and application-specific implementations
- **Transport Agnostic**: Supports HTTP(S), CoAP, file-based, and custom transport mechanisms
- **Production Ready**: Used in Siemens SICAM GridPass enterprise PKI solution
- **Open Source**: Apache License 2.0, community-driven development

### 1.3 Primary Use Cases
- Certificate enrollment (initial, renewal, key update)
- Certificate revocation
- CA certificate distribution and updates
- Certificate request template distribution
- CRL retrieval
- Offline/online hybrid certificate operations
- Enterprise PKI integration

### 1.4 Funding and Development
Partially funded by German Federal Ministry of Education and Research (BMBF) under Quoryptan project (grant 16KIS2033), demonstrating government trust in enterprise-grade PKI solutions.

---

## 2. Product Overview

### 2.1 CMP RA Component (Core Library)

**Purpose**: Provide generic, reusable Java library for implementing CMP-based Registration Authority and client applications.

**Key Characteristics**:
- **Type**: Java library (JAR artifact)
- **Distribution**: Maven Central Repository (`com.siemens.pki:CmpRaComponent`)
- **Language**: Pure Java (JDK 11+)
- **Architecture**: Generic component with interface-based customization
- **Quality**: Production-grade, battle-tested in enterprise environments
- **Maintainability**: 289+ commits, active development, comprehensive documentation

**Primary Functions**:
1. **Message Construction**: Build CMP messages for all use cases (ir, cr, p10cr, kur, rr)
2. **Message Parsing**: Parse and validate incoming CMP messages
3. **Message Protection**: Sign/verify messages using certificates or MAC (shared secrets)
4. **Certificate Operations**: Enrollment, renewal, revocation, CA certificate retrieval
5. **Error Handling**: CMP-compliant error responses and Java exception handling
6. **State Management**: Transaction persistence for long-running operations
7. **Routing**: Profile-based message routing to multiple CAs

### 2.2 LightweightCmpRa (Reference Application)

**Purpose**: Demonstrate CMP RA implementation using the core component, provide CLI-based RA for testing and PoC deployments.

**Key Characteristics**:
- **Type**: Standalone Java application (executable JAR)
- **Distribution**: GitHub source repository
- **Language**: Java (98.5%), Python (1.5% - testing scripts)
- **Architecture**: CLI-driven with YAML configuration
- **Quality**: Proof-of-concept, suitable for demonstration and testing
- **Use Case**: Development, testing, lightweight production environments

**Primary Functions**:
1. **RA Server**: Accept certificate requests via HTTP(S), CoAP, or file-based transport
2. **CMP Client**: Submit enrollment/revocation requests to upstream CAs
3. **Configuration Management**: YAML-based multi-instance RA configuration
4. **Protocol Testing**: Validate RFC 9483 compliance
5. **Integration Testing**: Test CMP client/server interoperability

---

## 3. Functional Architecture

### 3.1 High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    END ENTITIES / CLIENTS                        │
│  (Applications, IoT devices, Users requesting certificates)     │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         │ CMP Messages (HTTP/HTTPS/CoAP/File)
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                DOWNSTREAM TRANSPORT ADAPTERS                     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │ HTTP Handler │  │ CoAP Handler │  │ File Handler │          │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘          │
└─────────┼──────────────────┼──────────────────┼─────────────────┘
          │                  │                  │
          └──────────────────┼──────────────────┘
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│              CMP RA COMPONENT (Core Library)                     │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Message Processing Engine                               │  │
│  │  - Parse CMP messages (ASN.1 DER decoding)              │  │
│  │  - Validate message structure and signatures            │  │
│  │  - Extract certificate requests                         │  │
│  │  - Build CMP responses                                  │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Cryptographic Operations                                │  │
│  │  - Message signing (PKI or MAC-based)                   │  │
│  │  - Signature verification                                │  │
│  │  - Key generation (optional)                            │  │
│  │  - Certificate validation                               │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Business Logic Interfaces                               │  │
│  │  - InventoryInterface (authorization, request mod.)     │  │
│  │  - PersistencyInterface (state storage)                │  │
│  │  - ConfigurationInterface (dynamic settings)            │  │
│  └──────────────────────────────────────────────────────────┘  │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         │ Forward to CA (CMP/PKCS#10/X.509)
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                UPSTREAM TRANSPORT ADAPTERS                       │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │ HTTP Handler │  │ HTTPS Handler│  │ File Handler │          │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘          │
└─────────┼──────────────────┼──────────────────┼─────────────────┘
          │                  │                  │
          └──────────────────┼──────────────────┘
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│              CERTIFICATE AUTHORITY (CA)                          │
│  (EJBCA, Microsoft CA, Siemens SICAM CA, etc.)                  │
└─────────────────────────────────────────────────────────────────┘
```

### 3.2 Component Layering

#### Layer 1: Transport Layer (Application-Specific)
**Responsibility**: Message routing and protocol handling

**Components**:
- HTTP/HTTPS servlet adapters (e.g., Apache Tomcat integration)
- CoAP protocol handlers
- File-based message drop/pickup
- Custom transport implementations

**Functions**:
- Receive byte arrays from clients
- Route byte arrays to CMP RA Component
- Return response byte arrays to clients
- Handle transport-level errors and timeouts

#### Layer 2: CMP Processing Layer (CMP RA Component)
**Responsibility**: CMP protocol implementation

**Components**:
- Message parser (ASN.1 DER decoder)
- Message constructor (ASN.1 DER encoder)
- Signature/MAC verification engine
- Certificate request processor
- Error message generator

**Functions**:
- Decode CMP messages from byte arrays
- Validate message structure per RFC 9810
- Verify message protection (signature or MAC)
- Extract certificate requests (CRMF or PKCS#10)
- Construct CMP responses
- Encode responses to byte arrays

#### Layer 3: Cryptographic Layer (Bouncy Castle + JCE)
**Responsibility**: Cryptographic operations

**Components**:
- Bouncy Castle library (low-level crypto)
- Java Cryptography Extension (JCE) providers
- Key generation utilities
- Certificate validation engine

**Functions**:
- Sign CMP messages with RA credentials
- Verify client/server signatures
- Generate key pairs (optional)
- Validate certificate chains
- Check certificate revocation status (CRL/OCSP)

#### Layer 4: Business Logic Layer (Interface Implementations)
**Responsibility**: Application-specific authorization and persistence

**Components**:
- `InventoryInterface` implementation
- `PersistencyInterface` implementation
- Custom configuration providers

**Functions**:
- Authorize certificate requests
- Modify requests based on policy
- Store transaction state
- Retrieve configuration dynamically
- Report enrollment/revocation status to external systems

---

## 4. Core Functional Capabilities

### 4.1 Certificate Enrollment

#### FR-ENROLL-1: Initial Request (ir)
**Description**: End entity requests certificate for the first time without existing certificate

**Message Flow**:
1. Client generates key pair locally
2. Client creates CRMF (Certificate Request Message Format) request
3. Client protects message with shared secret (MAC) or pre-configured certificate
4. RA validates message protection
5. RA invokes `InventoryInterface.checkAndModifyCertRequest()` for authorization
6. RA forwards request to CA (CMP or PKCS#10 format)
7. CA signs certificate
8. RA returns certificate in CMP response
9. RA invokes `InventoryInterface.certConsumed()` to report successful issuance

**Authorization Points**:
- Message protection verification (MAC or signature)
- Inventory interface authorization check
- Optional: Request modification by inventory (change subject DN, extensions)

**Supported Features**:
- Local key generation (end entity keeps private key)
- Central key generation (RA generates key pair, returns with certificate)
- Subject DN validation and modification
- Extension validation and modification
- Implicit confirmation (certificate returned in response)
- Explicit confirmation (separate confirmation message)

#### FR-ENROLL-2: Certificate Request (cr)
**Description**: End entity with existing certificate requests new certificate

**Message Flow**:
1. Client signs request with existing certificate
2. RA verifies client signature using trusted root certificates
3. RA authorizes request via inventory interface
4. RA forwards to CA
5. RA returns new certificate

**Key Differences from IR**:
- Message protection uses PKI signature, not shared secret
- Client identity already proven by existing certificate
- Typically used for requesting additional certificate types

#### FR-ENROLL-3: PKCS#10 Certificate Request (p10cr)
**Description**: Accept legacy PKCS#10 CSR wrapped in CMP message

**Message Flow**:
1. Client submits PKCS#10 CSR in CMP wrapper
2. RA extracts PKCS#10 CSR
3. RA validates CSR signature
4. RA authorizes via inventory interface
5. RA may forward PKCS#10 directly to CA (if CA supports) or convert to CMP
6. RA returns certificate

**Use Case**: Integration with legacy applications that only support PKCS#10

#### FR-ENROLL-4: Key Update Request (kur)
**Description**: End entity renews certificate before expiration (same key or new key)

**Message Flow**:
1. Client signs request with current certificate
2. RA validates certificate is still valid
3. RA checks if renewal is permitted (timing policy)
4. RA authorizes via inventory interface
5. RA forwards to CA for renewal
6. RA returns renewed certificate

**Renewal Policies**:
- Same key pair reuse (kur with same public key)
- New key generation (kur with new public key)
- Renewal window (e.g., only within 30 days of expiration)

### 4.2 Certificate Revocation

#### FR-REVOKE-1: Revocation Request (rr)
**Description**: Revoke certificate before expiration

**Message Flow**:
1. Client submits revocation request with:
   - Certificate serial number and issuer DN, OR
   - Complete certificate to revoke
2. Client signs request with authorized certificate
3. RA validates client is authorized to revoke (owner or administrator)
4. RA authorizes via inventory interface
5. RA forwards revocation to CA
6. CA updates CRL
7. RA returns revocation confirmation

**Revocation Reasons** (RFC 5280):
- unspecified
- keyCompromise
- cACompromise
- affiliationChanged
- superseded
- cessationOfOperation
- certificateHold
- removeFromCRL (unsuspend)
- privilegeWithdrawn
- aACompromise

**Authorization**:
- Certificate owner can revoke own certificates
- RA administrator can revoke any certificate
- Inventory interface can enforce additional policy

### 4.3 CA Certificate Distribution

#### FR-CACERT-1: Get CA Certificates (genm/genp)
**Description**: Retrieve CA certificate chain

**Message Flow**:
1. Client sends general message (genm) requesting CA certificates
2. RA returns general response (genp) with CA certificate chain
3. Chain includes: end-entity CA cert, intermediate CA certs, root CA cert

**Use Cases**:
- Initial trust anchor establishment
- Certificate validation (build chain to trusted root)
- Certificate import operations

#### FR-CACERT-2: Get Root CA Certificate Update (genm/genp)
**Description**: Update root CA certificate when root is renewed

**Message Flow**:
1. Client sends request with old root CA certificate
2. RA returns new root CA certificate
3. Client validates new root is signed by old root (or vice versa for rollover)

**Use Case**: Root CA key rollover without manual trust anchor updates

### 4.4 Support Messages

#### FR-SUPPORT-1: Get Certificate Request Template (genm/genp)
**Description**: Retrieve certificate request template for specific certificate profile

**Message Flow**:
1. Client sends request specifying certificate profile (optional)
2. RA returns template with:
   - Supported subject DN attributes
   - Supported extensions
   - Key algorithm and size requirements
   - Validity period
   - Any pre-populated values

**Use Case**: Guide client in constructing valid certificate requests

#### FR-SUPPORT-2: Get Certificate Revocation Lists (genm/genp)
**Description**: Retrieve CRLs from CA

**Message Flow**:
1. Client requests CRL for specific CA
2. RA retrieves CRL from upstream CA
3. RA returns CRL in response

**Use Case**: Offline certificate validation

### 4.5 Nested Messages

#### FR-NESTED-1: Nested Message Support
**Description**: RA forwards client messages to CA without unwrapping, returns CA response

**Message Flow**:
1. Client creates CMP message intended for CA
2. Client wraps message in outer CMP message to RA
3. RA validates outer message protection
4. RA authorizes forwarding via inventory interface
5. RA sends inner message to CA (unchanged)
6. RA wraps CA response and returns to client

**Use Case**: RA acts as authorization gateway without inspecting CA-bound messages

---

## 5. Protocol Support and Standards

### 5.1 CMP Standards Compliance

#### RFC 9810: Certificate Management Protocol (CMP)
**Status**: Primary specification, full implementation

**Supported Features**:
- All message types: ir, cr, p10cr, kur, rr, genm, genp, error
- Message protection: PKI signature, shared secret MAC
- Message confirmation: implicit, explicit
- Key generation: local, central
- Certificate formats: X.509v3
- Request formats: CRMF (RFC 4211), PKCS#10 (RFC 2986)

#### RFC 9483: Lightweight CMP Profile
**Status**: Reference implementation, full compliance

**Key Profile Requirements**:
- Mandatory HTTP transport (RFC 9811)
- Simplified protection mechanisms
- Reduced message complexity
- Mandatory implicit confirmation
- Support for ir, cr, kur, rr, genm/genp

**Siemens Role**: CMP RA Component served as reference implementation during RFC 9483 standardization at IETF.

#### RFC 9480: CMP Updates
**Status**: Implemented

**Updates**:
- Enhanced error messages
- Additional general message types
- Improved certificate request handling

#### RFC 9481: CMP Algorithms
**Status**: Implemented

**Algorithm Support**:
- RSA: 2048, 3072, 4096 bit keys
- ECDSA: P-256, P-384, P-521 curves
- Hash algorithms: SHA-256, SHA-384, SHA-512
- MAC algorithms: HMAC-SHA256, HMAC-SHA384, HMAC-SHA512

#### RFC 4210: CMPv2
**Status**: Implemented (superseded by RFC 9810)

**Legacy Support**: Ensures backward compatibility with older CMP implementations.

#### RFC 4211: Certificate Request Message Format (CRMF)
**Status**: Full implementation

**Features**:
- Proof of possession (signature, key encipherment, key agreement)
- Subject DN and SAN encoding
- Extension requests
- Public key encoding

### 5.2 Transport Protocol Support

#### RFC 9811: HTTP Transfer for CMP
**Status**: Primary transport, full implementation

**Features**:
- HTTP POST to /.well-known/cmp/[operation]
- Content-Type: application/pkixcmp
- Synchronous and asynchronous modes
- HTTP status code mapping

#### CoAP (Constrained Application Protocol)
**Status**: Implemented in LightweightCmpRa

**Features**:
- UDP-based transport for IoT devices
- Lightweight alternative to HTTP
- Suitable for resource-constrained devices

#### File-Based Transport
**Status**: Implemented in LightweightCmpRa

**Features**:
- Offline certificate operations
- Message drop/pickup via file system
- Suitable for air-gapped environments

#### Custom Transport
**Status**: Supported via byte-array interface

**Features**:
- Application provides transport implementation
- RA component operates on opaque byte arrays
- Supports any protocol (MQTT, WebSocket, proprietary)

### 5.3 Legacy Protocol Support

#### PKCS#10 / X.509
**Status**: Supported for upstream CA communication

**Use Case**: RA accepts CMP from clients, converts to PKCS#10 for legacy CA

**Conversion**:
- Extract public key and subject DN from CRMF
- Create PKCS#10 CSR
- Forward to CA
- Convert X.509 certificate response to CMP

---

## 6. Message Flows and Use Cases

### 6.1 Enrollment Flow (Initial Request with Shared Secret)

```
┌──────────┐                    ┌──────────┐                    ┌──────────┐
│  Client  │                    │    RA    │                    │    CA    │
│ (Device) │                    │Component │                    │ (EJBCA) │
└────┬─────┘                    └────┬─────┘                    └────┬─────┘
     │                               │                               │
     │ 1. Generate Key Pair          │                               │
     ├───────────────────────────────┤                               │
     │                               │                               │
     │ 2. Create CRMF cert request   │                               │
     │    (Subject DN, extensions)   │                               │
     ├───────────────────────────────┤                               │
     │                               │                               │
     │ 3. Build CMP IR message       │                               │
     │    Protected by MAC (shared   │                               │
     │    secret pre-configured)     │                               │
     ├───────────────────────────────┤                               │
     │                               │                               │
     │ 4. HTTP POST IR message       │                               │
     ├──────────────────────────────►│                               │
     │                               │                               │
     │                               │ 5. Validate MAC protection   │
     │                               ├──────────────────────────────┤
     │                               │                               │
     │                               │ 6. Parse CRMF request        │
     │                               ├──────────────────────────────┤
     │                               │                               │
     │                               │ 7. Inventory.checkAndModify  │
     │                               │    CertRequest()             │
     │                               ├──────────────────────────────┤
     │                               │    (authorize, modify DN,    │
     │                               │     extensions)              │
     │                               ◄──────────────────────────────┤
     │                               │    [approved/rejected]       │
     │                               │                               │
     │                               │ 8. Forward request to CA     │
     │                               ├──────────────────────────────►│
     │                               │    (CMP or PKCS#10)          │
     │                               │                               │
     │                               │                               │ 9. Sign cert
     │                               │                               ├──────────┤
     │                               │                               │          │
     │                               │ 10. Return signed cert       ◄──────────┤
     │                               ◄───────────────────────────────┤
     │                               │                               │
     │                               │ 11. Build CMP IP response    │
     │                               │     (include certificate)    │
     │                               ├──────────────────────────────┤
     │                               │                               │
     │                               │ 12. Inventory.certConsumed() │
     │                               ├──────────────────────────────┤
     │                               │    (report issuance)         │
     │                               ◄──────────────────────────────┤
     │                               │                               │
     │ 13. HTTP response with cert   │                               │
     ◄───────────────────────────────┤                               │
     │                               │                               │
     │ 14. Validate cert signature   │                               │
     ├───────────────────────────────┤                               │
     │                               │                               │
     │ 15. Import cert to keystore   │                               │
     ├───────────────────────────────┤                               │
     │                               │                               │
```

**Timeline**: Synchronous (immediate response) - typically < 5 seconds

### 6.2 Renewal Flow (Key Update Request)

```
┌──────────┐                    ┌──────────┐                    ┌──────────┐
│  Client  │                    │    RA    │                    │    CA    │
│ (Device) │                    │Component │                    │ (EJBCA) │
└────┬─────┘                    └────┬─────┘                    └────┬─────┘
     │                               │                               │
     │ 1. Certificate expiring soon  │                               │
     │    (30 days remaining)        │                               │
     ├───────────────────────────────┤                               │
     │                               │                               │
     │ 2. Optionally generate new    │                               │
     │    key pair (or reuse old)    │                               │
     ├───────────────────────────────┤                               │
     │                               │                               │
     │ 3. Create CRMF request        │                               │
     │    (same subject DN)          │                               │
     ├───────────────────────────────┤                               │
     │                               │                               │
     │ 4. Build CMP KUR message      │                               │
     │    Signed with current cert   │                               │
     ├───────────────────────────────┤                               │
     │                               │                               │
     │ 5. HTTP POST KUR message      │                               │
     ├──────────────────────────────►│                               │
     │                               │                               │
     │                               │ 6. Verify signature with     │
     │                               │    client's current cert     │
     │                               ├──────────────────────────────┤
     │                               │                               │
     │                               │ 7. Validate cert not expired │
     │                               │    and not revoked           │
     │                               ├──────────────────────────────┤
     │                               │                               │
     │                               │ 8. Check renewal timing      │
     │                               │    policy (not too early)    │
     │                               ├──────────────────────────────┤
     │                               │                               │
     │                               │ 9. Inventory authorization   │
     │                               ├──────────────────────────────┤
     │                               │                               │
     │                               │ 10. Forward to CA            │
     │                               ├──────────────────────────────►│
     │                               │                               │
     │                               │                               │ 11. Issue
     │                               │                               │     renewed
     │                               │                               │     cert
     │                               │                               ├──────────┤
     │                               │                               │          │
     │                               │ 12. Return renewed cert      ◄──────────┤
     │                               ◄───────────────────────────────┤
     │                               │                               │
     │                               │ 13. Inventory.certConsumed() │
     │                               ├──────────────────────────────┤
     │                               │                               │
     │ 14. HTTP response with cert   │                               │
     ◄───────────────────────────────┤                               │
     │                               │                               │
     │ 15. Import renewed cert       │                               │
     ├───────────────────────────────┤                               │
     │                               │                               │
     │ 16. Optional: revoke old cert │                               │
     ├──────────────────────────────►│                               │
     │    (after confirming new cert │                               │
     │     works)                    │                               │
     │                               │                               │
```

### 6.3 Revocation Flow

```
┌──────────┐                    ┌──────────┐                    ┌──────────┐
│  Client  │                    │    RA    │                    │    CA    │
│ (Admin)  │                    │Component │                    │ (EJBCA) │
└────┬─────┘                    └────┬─────┘                    └────┬─────┘
     │                               │                               │
     │ 1. User reports key           │                               │
     │    compromise                 │                               │
     ├───────────────────────────────┤                               │
     │                               │                               │
     │ 2. Build CMP RR message       │                               │
     │    - Cert serial + issuer DN  │                               │
     │    - Reason: keyCompromise    │                               │
     │    Signed with admin cert     │                               │
     ├───────────────────────────────┤                               │
     │                               │                               │
     │ 3. HTTP POST RR message       │                               │
     ├──────────────────────────────►│                               │
     │                               │                               │
     │                               │ 4. Verify admin signature    │
     │                               ├──────────────────────────────┤
     │                               │                               │
     │                               │ 5. Inventory.checkAndModify  │
     │                               │    RevocationRequest()       │
     │                               ├──────────────────────────────┤
     │                               │    (authorize revocation)    │
     │                               ◄──────────────────────────────┤
     │                               │                               │
     │                               │ 6. Forward to CA             │
     │                               ├──────────────────────────────►│
     │                               │                               │
     │                               │                               │ 7. Add to
     │                               │                               │    CRL
     │                               │                               ├──────────┤
     │                               │                               │          │
     │                               │ 8. Revocation confirmation   ◄──────────┤
     │                               ◄───────────────────────────────┤
     │                               │                               │
     │                               │ 9. Inventory.certConsumed()  │
     │                               │    (status = revoked)        │
     │                               ├──────────────────────────────┤
     │                               │                               │
     │ 10. HTTP response confirming  │                               │
     ◄───────────────────────────────┤                               │
     │                               │                               │
     │ 11. Notify certificate owner  │                               │
     │     of revocation             │                               │
     ├───────────────────────────────┤                               │
     │                               │                               │
```

### 6.4 Asynchronous Enrollment Flow

**Use Case**: CA requires manual approval, cannot respond immediately

```
┌──────────┐              ┌──────────┐              ┌──────────┐
│  Client  │              │    RA    │              │    CA    │
└────┬─────┘              └────┬─────┘              └────┬─────┘
     │                         │                         │
     │ 1. POST IR message      │                         │
     ├────────────────────────►│                         │
     │                         │ 2. Authorize            │
     │                         ├────────────────────────┤
     │                         │                         │
     │                         │ 3. Forward to CA        │
     │                         ├────────────────────────►│
     │                         │                         │
     │                         │ 4. CA: Manual approval  │
     │                         │    required, return     │
     │                         │    "WAITING" status     │
     │                         ◄─────────────────────────┤
     │                         │                         │
     │                         │ 5. Persistence.store    │
     │                         │    TransactionState()   │
     │                         ├────────────────────────┤
     │                         │                         │
     │ 6. HTTP response:       │                         │
     │    PKIStatus = WAITING  │                         │
     │    Transaction ID       │                         │
     ◄─────────────────────────┤                         │
     │                         │                         │
     │                         │                         │
     │ [Time passes - minutes to hours]                  │
     │                         │                         │
     │                         │ 7. CA approves, signs   │
     │                         │    cert, pushes to RA   │
     │                         ◄─────────────────────────┤
     │                         │                         │
     │                         │ 8. Persistence.update   │
     │                         │    TransactionState()   │
     │                         ├────────────────────────┤
     │                         │                         │
     │ 9. Client polls with    │                         │
     │    Transaction ID       │                         │
     ├────────────────────────►│                         │
     │                         │                         │
     │                         │ 10. Retrieve state      │
     │                         ├────────────────────────┤
     │                         │                         │
     │ 11. HTTP response:      │                         │
     │     Certificate ready   │                         │
     ◄─────────────────────────┤                         │
     │                         │                         │
```

---

## 7. Configuration and Management

### 7.1 CMP RA Component Configuration

#### Configuration Interface Structure

The component uses **interface-based configuration** allowing dynamic, profile-dependent settings.

```java
public interface Configuration {
    // Downstream: Verify clients
    VerificationContext getDownstreamConfiguration(String certProfile);

    // Downstream: Credentials to sign responses to clients
    CredentialContext getDownstreamCredentials(String certProfile);

    // Upstream: Verify CA responses
    VerificationContext getUpstreamConfiguration(String certProfile);

    // Upstream: Credentials to sign requests to CA
    CredentialContext getUpstreamCredentials(String certProfile);

    // Inventory integration
    InventoryInterface getInventory(String certProfile);

    // State persistence
    PersistencyInterface getPersistence();

    // Optional: Upstream CA endpoint (for HTTP client)
    String getUpstreamEndpoint(String certProfile);
}
```

#### VerificationContext

Defines how to verify message signatures:

```java
public interface VerificationContext {
    // Trusted root certificates
    List<X509Certificate> getTrustAnchors();

    // Intermediate certificates for chain building
    List<X509Certificate> getIntermediateCertificates();

    // Certificate validation options
    boolean checkCertificateRevocation();

    // CRL Distribution Points
    List<String> getCrlUrls();

    // OCSP responder URLs
    List<String> getOcspUrls();

    // Optional: Shared secret for MAC-based protection
    byte[] getSharedSecret(String recipientDN);
}
```

#### CredentialContext

Defines RA credentials for signing:

```java
public interface CredentialContext {
    // RA private key
    PrivateKey getPrivateKey();

    // RA certificate
    X509Certificate getCertificate();

    // RA certificate chain
    List<X509Certificate> getCertificateChain();

    // Optional: Shared secret for MAC-based protection
    byte[] getSharedSecret(String recipientDN);
}
```

### 7.2 LightweightCmpRa YAML Configuration

#### Configuration File Structure

```yaml
---
# RA Instance Configuration
ra:
  # Downstream (client-facing) configuration
  downstream:
    - interface: "http"
      listenAddress: "0.0.0.0"
      listenPort: 8080
      httpPath: "/cmp"

      # Verification (how to verify client messages)
      verification:
        trustAnchors:
          - file: "/path/to/root-ca.pem"
        intermediates:
          - file: "/path/to/intermediate-ca.pem"
        sharedSecrets:
          - recipient: "CN=Client1,O=Company"
            secret: "base64-encoded-secret"

        # Certificate validation options
        checkRevocation: true
        crlUrls:
          - "http://ca.example.com/crl"
        ocspUrls:
          - "http://ocsp.example.com"

      # Credentials (how to sign responses to clients)
      credentials:
        privateKey:
          file: "/path/to/ra-private.key"
          password: "${RA_KEY_PASSWORD}"
        certificate:
          file: "/path/to/ra-cert.pem"
        certificateChain:
          - file: "/path/to/intermediate.pem"
          - file: "/path/to/root.pem"

  # Upstream (CA-facing) configuration
  upstream:
    - interface: "https"
      url: "https://ca.example.com/.well-known/cmp/p/enrollment"

      # Verification (how to verify CA responses)
      verification:
        trustAnchors:
          - file: "/path/to/ca-root.pem"

      # Credentials (how to sign requests to CA)
      credentials:
        privateKey:
          file: "/path/to/ra-private.key"
        certificate:
          file: "/path/to/ra-cert.pem"

      # Routing rules
      routing:
        - certProfile: "email-cert"
          upstreamEndpoint: "https://ca.example.com/.well-known/cmp/p/email"
        - certProfile: "vpn-cert"
          upstreamEndpoint: "https://ca.example.com/.well-known/cmp/p/vpn"

  # Inventory interface (authorization)
  inventory:
    type: "external-api"
    url: "http://inventory.example.com/api"
    credentials:
      username: "ra-service"
      password: "${INVENTORY_PASSWORD}"

  # Persistence (transaction state storage)
  persistence:
    type: "database"
    jdbcUrl: "jdbc:postgresql://localhost:5432/radb"
    username: "rauser"
    password: "${DB_PASSWORD}"
```

#### Certificate Profile-Based Configuration

Multiple certificate profiles can have different settings:

```yaml
ra:
  downstream:
    - interface: "http"
      listenPort: 8080

      # Different verification per profile
      profiles:
        - name: "email-cert"
          verification:
            sharedSecret: "email-secret"
          credentials:
            certificate: "/path/to/email-ra-cert.pem"

        - name: "vpn-cert"
          verification:
            trustAnchors:
              - file: "/path/to/vpn-root.pem"
          credentials:
            certificate: "/path/to/vpn-ra-cert.pem"
```

### 7.3 Dynamic Configuration

The configuration interface allows **runtime decision-making**:

**Example**: Select upstream CA based on certificate profile

```java
@Override
public String getUpstreamEndpoint(String certProfile) {
    switch (certProfile) {
        case "email-cert":
            return "https://ca1.example.com/cmp/email";
        case "vpn-cert":
            return "https://ca2.example.com/cmp/vpn";
        case "code-signing":
            return "https://ca-secure.example.com/cmp/codesign";
        default:
            return "https://ca.example.com/cmp/default";
    }
}
```

**Example**: Use different shared secrets per client

```java
@Override
public byte[] getSharedSecret(String recipientDN) {
    // Query database or external service
    return secretStore.getSecretForClient(recipientDN);
}
```

---

## 8. Integration Interfaces

### 8.1 Inventory Interface

**Purpose**: Authorize certificate requests, modify requests based on policy, report enrollment/revocation status to external systems

#### Interface Definition

```java
public interface InventoryInterface {
    /**
     * Authorize and optionally modify certificate request
     *
     * @param certTemplate Certificate request from client
     * @param certProfile Certificate profile (from CMP header)
     * @param requesterDN Distinguished name of requester
     * @return Modified certificate template, or null to reject
     * @throws Exception if authorization fails
     */
    CertTemplate checkAndModifyCertRequest(
        CertTemplate certTemplate,
        String certProfile,
        String requesterDN
    ) throws Exception;

    /**
     * Authorize revocation request
     *
     * @param certToRevoke Certificate to revoke (serial + issuer)
     * @param reason Revocation reason code
     * @param requesterDN Distinguished name of requester
     * @return true if authorized, false to reject
     */
    boolean checkRevocationRequest(
        CertId certToRevoke,
        int reason,
        String requesterDN
    ) throws Exception;

    /**
     * Report successful certificate issuance
     *
     * @param certTemplate Issued certificate template
     * @param certProfile Certificate profile
     * @param requesterDN Distinguished name of requester
     * @param certificate Actual issued certificate
     */
    void certConsumed(
        CertTemplate certTemplate,
        String certProfile,
        String requesterDN,
        X509Certificate certificate
    );

    /**
     * Report certificate revocation
     *
     * @param certToRevoke Revoked certificate
     * @param reason Revocation reason
     * @param requesterDN Who requested revocation
     */
    void certRevoked(
        CertId certToRevoke,
        int reason,
        String requesterDN
    );
}
```

#### Use Cases

**Use Case 1: Authorization Based on AD Group Membership**

```java
@Override
public CertTemplate checkAndModifyCertRequest(
    CertTemplate certTemplate,
    String certProfile,
    String requesterDN
) throws Exception {

    // Query Active Directory
    User user = adService.getUserByDN(requesterDN);

    // Check authorization
    if (certProfile.equals("vpn-cert")) {
        if (!user.isMemberOf("VPN-Users")) {
            throw new Exception("User not authorized for VPN certificates");
        }
    }

    if (certProfile.equals("code-signing")) {
        if (!user.isMemberOf("Developers")) {
            throw new Exception("User not authorized for code signing");
        }
    }

    // Modify subject DN to match AD
    String expectedCN = user.getDisplayName();
    String requestedCN = certTemplate.getSubject().getRDNs(BCStyle.CN)[0]
        .getFirst().getValue().toString();

    if (!expectedCN.equals(requestedCN)) {
        // Modify certificate request to use correct CN from AD
        return modifySubjectDN(certTemplate, expectedCN);
    }

    return certTemplate; // Approved as-is
}
```

**Use Case 2: Certificate Quota Enforcement**

```java
@Override
public CertTemplate checkAndModifyCertRequest(
    CertTemplate certTemplate,
    String certProfile,
    String requesterDN
) throws Exception {

    // Check quota
    int activeCerts = database.countActiveCertificates(requesterDN, certProfile);
    int quota = getQuota(requesterDN, certProfile);

    if (activeCerts >= quota) {
        throw new Exception("Certificate quota exceeded: " + activeCerts + "/" + quota);
    }

    return certTemplate;
}
```

**Use Case 3: Audit Logging to External System**

```java
@Override
public void certConsumed(
    CertTemplate certTemplate,
    String certProfile,
    String requesterDN,
    X509Certificate certificate
) {
    // Log to audit system
    auditLog.recordEvent(
        "CERTIFICATE_ISSUED",
        "serial=" + certificate.getSerialNumber() +
        ",subject=" + certificate.getSubjectDN() +
        ",requester=" + requesterDN +
        ",profile=" + certProfile
    );

    // Update inventory database
    database.insertCertificate(
        certificate.getSerialNumber().toString(),
        requesterDN,
        certProfile,
        certificate.getNotBefore(),
        certificate.getNotAfter(),
        "ACTIVE"
    );

    // Send notification
    emailService.sendCertificateReadyEmail(requesterDN, certificate);
}
```

### 8.2 Persistence Interface

**Purpose**: Store transaction state for asynchronous operations, enable application recovery after restart

#### Interface Definition

```java
public interface PersistencyInterface {
    /**
     * Store transaction state
     *
     * @param transactionId Unique transaction identifier
     * @param state Serialized state (opaque byte array)
     */
    void storeTransactionState(String transactionId, byte[] state);

    /**
     * Retrieve transaction state
     *
     * @param transactionId Transaction identifier
     * @return Serialized state, or null if not found
     */
    byte[] getTransactionState(String transactionId);

    /**
     * Delete transaction state
     *
     * @param transactionId Transaction identifier
     */
    void deleteTransactionState(String transactionId);

    /**
     * List all pending transactions
     *
     * @return List of transaction IDs
     */
    List<String> listPendingTransactions();
}
```

#### Implementation Example: Database Persistence

```java
public class DatabasePersistence implements PersistencyInterface {

    private DataSource dataSource;

    @Override
    public void storeTransactionState(String transactionId, byte[] state) {
        try (Connection conn = dataSource.getConnection()) {
            String sql = "INSERT INTO cmp_transactions (transaction_id, state, created_at) " +
                        "VALUES (?, ?, NOW()) " +
                        "ON CONFLICT (transaction_id) DO UPDATE SET state = ?, updated_at = NOW()";

            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, transactionId);
            stmt.setBytes(2, state);
            stmt.setBytes(3, state);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to store transaction state", e);
        }
    }

    @Override
    public byte[] getTransactionState(String transactionId) {
        try (Connection conn = dataSource.getConnection()) {
            String sql = "SELECT state FROM cmp_transactions WHERE transaction_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, transactionId);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getBytes("state");
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to retrieve transaction state", e);
        }
    }

    @Override
    public void deleteTransactionState(String transactionId) {
        try (Connection conn = dataSource.getConnection()) {
            String sql = "DELETE FROM cmp_transactions WHERE transaction_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, transactionId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete transaction state", e);
        }
    }
}
```

#### Use Case: Asynchronous Enrollment Recovery

**Scenario**: RA application crashes during pending enrollment

```
1. Client submits enrollment request
2. RA stores transaction state in database
3. RA forwards request to CA
4. RA application crashes before receiving CA response
5. Application restarts
6. Application queries PersistencyInterface.listPendingTransactions()
7. For each pending transaction:
   a. Retrieve state
   b. Poll CA for response
   c. If ready, send response to client (if client re-polls)
   d. Delete transaction
```

---

## 9. Security Features

### 9.1 Message Protection

#### Signature-Based Protection (PKI)

**Purpose**: Client signs CMP message with certificate to prove identity

**Process**:
1. Client creates CMP message
2. Client signs message with private key
3. Client includes signing certificate in message
4. RA receives message
5. RA builds certificate chain to trusted root
6. RA validates certificate (not expired, not revoked, valid signature)
7. RA verifies message signature using client's public key

**Advantages**:
- Strong authentication (cryptographic proof of identity)
- Non-repudiation (client cannot deny sending message)
- No pre-shared secrets required

**Challenges**:
- Client must already have certificate (not suitable for initial enrollment)
- Certificate chain validation requires connectivity (CRL/OCSP)

#### MAC-Based Protection (Shared Secret)

**Purpose**: Client proves knowledge of pre-shared secret without transmitting it

**Process**:
1. Client and RA pre-configure shared secret (out-of-band)
2. Client creates CMP message
3. Client computes HMAC over message using shared secret
4. Client includes HMAC in message (no secret transmitted)
5. RA receives message
6. RA retrieves shared secret for this client (from configuration or database)
7. RA computes HMAC over message using shared secret
8. RA compares computed HMAC with received HMAC
9. If match, client is authenticated

**Advantages**:
- Suitable for initial enrollment (no certificate required)
- Simpler than PKI (no certificate chain validation)
- Lightweight (no asymmetric crypto)

**Challenges**:
- Shared secret must be securely distributed out-of-band
- No non-repudiation (both parties know secret)
- Secret rotation requires coordination

#### Hybrid Protection

**Recommendation**: Use MAC for initial enrollment, PKI for subsequent operations

```
First Enrollment:
  Client → RA: ir message (MAC-protected with shared secret)
  RA → Client: ip message (signed with RA certificate)

Renewal:
  Client → RA: kur message (signed with current certificate)
  RA → Client: kup message (signed with RA certificate)
```

### 9.2 Certificate Validation

#### Certificate Chain Building

**Process**:
1. Start with client certificate (from CMP message)
2. Look for issuer certificate in message or local cache
3. Repeat until reaching trusted root
4. Validate each certificate in chain:
   - Signature is valid
   - Not expired (current date within validity period)
   - Not revoked (check CRL or OCSP)
   - Critical extensions understood
   - Key usage permits operation (e.g., digitalSignature)

#### Revocation Checking

**CRL (Certificate Revocation List)**:
- Download CRL from CRL Distribution Point (CDP) in certificate
- Parse CRL and search for certificate serial number
- If found, certificate is revoked
- Cache CRL until nextUpdate time

**OCSP (Online Certificate Status Protocol)**:
- Send OCSP request to OCSP responder (URL from certificate AIA extension)
- Receive real-time status: good, revoked, unknown
- More efficient than CRL for individual certificate checks

**Configuration**:
```java
@Override
public VerificationContext getDownstreamConfiguration(String certProfile) {
    return new VerificationContext() {
        @Override
        public boolean checkCertificateRevocation() {
            return true; // Enable revocation checking
        }

        @Override
        public List<String> getCrlUrls() {
            return Arrays.asList("http://ca.example.com/crl");
        }

        @Override
        public List<String> getOcspUrls() {
            return Arrays.asList("http://ocsp.example.com");
        }
    };
}
```

### 9.3 Cryptographic Algorithms

#### Supported Key Algorithms

**RSA**:
- Key sizes: 2048, 3072, 4096 bits
- Recommended: 2048 bits minimum, 4096 for high-security

**ECDSA (Elliptic Curve)**:
- Curves: P-256 (secp256r1), P-384 (secp384r1), P-521 (secp521r1)
- Recommended: P-256 for most use cases, P-384 for high-security

**Algorithm Selection**: Configured in CertTemplate during enrollment

#### Hash Algorithms

- SHA-256 (recommended for most use cases)
- SHA-384 (for high-security)
- SHA-512 (for very high-security)

**Note**: SHA-1 and MD5 are NOT supported (deprecated due to collision attacks)

#### Signature Algorithms

- RSA with SHA-256: `sha256WithRSAEncryption`
- ECDSA with SHA-256: `ecdsa-with-SHA256`
- ECDSA with SHA-384: `ecdsa-with-SHA384`

#### MAC Algorithms (Shared Secret Protection)

- HMAC-SHA256 (recommended)
- HMAC-SHA384
- HMAC-SHA512

### 9.4 Secure Key Generation

#### Client-Side Key Generation (Recommended)

**Advantages**:
- Private key never transmitted
- Client retains control
- Maximum security

**Process**:
1. Client generates key pair locally (OpenSSL, Java KeyPairGenerator, hardware token)
2. Client creates CRMF with public key
3. Client signs CRMF with private key (Proof of Possession)
4. RA validates signature (proves client has private key)
5. RA forwards public key to CA
6. CA signs certificate with client's public key
7. RA returns certificate (public key + CA signature)
8. Client pairs certificate with existing private key

#### Server-Side Key Generation (Central Key Generation)

**Use Case**: Client cannot generate keys (constrained IoT device, legacy system)

**Process**:
1. Client requests certificate without providing public key
2. RA generates key pair
3. RA encrypts private key with client's public encryption key (from pre-configured cert)
4. RA creates CRMF with generated public key
5. RA forwards to CA
6. CA signs certificate
7. RA returns certificate + encrypted private key
8. Client decrypts private key

**Security Considerations**:
- RA temporarily has access to private key (risk)
- Encrypted transmission required
- Not suitable for non-repudiation use cases

### 9.5 Transport Layer Security

#### HTTPS (TLS)

**Requirements**:
- TLS 1.2 minimum (TLS 1.3 recommended)
- Strong cipher suites (no export ciphers, no RC4, no 3DES)
- Server certificate validation (client verifies RA server)
- Optional client certificate authentication (mutual TLS)

**LightweightCmpRa Configuration**:
```yaml
downstream:
  - interface: "https"
    listenPort: 8443
    tlsConfig:
      serverKeyStore:
        file: "/path/to/server.p12"
        password: "${TLS_KEYSTORE_PASSWORD}"
      trustStore:
        file: "/path/to/truststore.p12"
      requireClientAuth: false  # Set true for mTLS
```

#### Defense Against Network Attacks

**Man-in-the-Middle (MITM) Protection**:
- TLS encrypts message (prevents eavesdropping)
- CMP message signature (independent of transport)
- Client validates RA response signature

**Replay Attack Protection**:
- CMP nonce in messages
- Transaction IDs
- Timestamp validation

**Denial of Service (DoS) Protection**:
- Transport-level rate limiting
- Message size limits
- Transaction timeout

---

## 10. Transport Mechanisms

### 10.1 HTTP Transport (RFC 9811)

#### Specification Compliance

**Endpoint Structure**:
```
https://ra.example.com/.well-known/cmp/<operation>
```

**Operations**:
- `/ir` - Initial Request
- `/cr` - Certificate Request
- `/kur` - Key Update Request
- `/rr` - Revocation Request
- `/genm` - General Message
- `/p` - General Purpose (handles all operations)

#### Request Format

```
POST /.well-known/cmp/p/enrollment HTTP/1.1
Host: ra.example.com
Content-Type: application/pkixcmp
Content-Length: 1234

<binary CMP message in ASN.1 DER encoding>
```

#### Response Format

```
HTTP/1.1 200 OK
Content-Type: application/pkixcmp
Content-Length: 5678

<binary CMP response in ASN.1 DER encoding>
```

#### Error Responses

**CMP-Level Errors**: Return CMP error message in response body
```
HTTP/1.1 200 OK
Content-Type: application/pkixcmp

<CMP error message>
```

**HTTP-Level Errors**:
- `400 Bad Request` - Malformed message
- `401 Unauthorized` - Authentication required
- `404 Not Found` - Invalid endpoint
- `500 Internal Server Error` - Server failure
- `503 Service Unavailable` - Server overloaded

### 10.2 CoAP Transport

**Protocol**: Constrained Application Protocol (RFC 7252)

**Use Case**: IoT devices with limited resources (low bandwidth, low power)

**Advantages**:
- UDP-based (lower overhead than TCP/HTTP)
- Small message size
- Suitable for battery-powered devices

**Endpoint**:
```
coap://ra.example.com:5683/cmp
```

**Message Exchange**:
```
Client → RA: CON (confirmable) message with CMP request
RA → Client: ACK with CMP response
```

**LightweightCmpRa Configuration**:
```yaml
downstream:
  - interface: "coap"
    listenPort: 5683
    listenAddress: "0.0.0.0"
```

### 10.3 File-Based Transport

**Use Case**: Offline certificate operations, air-gapped environments

**Process**:
1. Client writes CMP request to file (e.g., `request-123.der`)
2. Client places file in RA input directory
3. RA monitors directory (polling or file system watcher)
4. RA processes request
5. RA writes response to file (e.g., `response-123.der`)
6. Client retrieves response file

**LightweightCmpRa Configuration**:
```yaml
downstream:
  - interface: "file"
    inputDirectory: "/var/ra/requests"
    outputDirectory: "/var/ra/responses"
    pollInterval: 10  # seconds
```

**Advantages**:
- No network connectivity required
- Suitable for high-security environments
- Simple integration with scripts

**Disadvantages**:
- Asynchronous only (no real-time response)
- File system security critical
- Manual file management

### 10.4 Custom Transport

**Flexibility**: CMP RA Component is transport-agnostic

**Integration Pattern**:
```java
// Application receives message via custom transport (MQTT, WebSocket, etc.)
byte[] cmpRequestBytes = receiveFromCustomTransport();

// Pass to RA Component
CmpRaComponent raComponent = new CmpRaComponent(configuration);
byte[] cmpResponseBytes = raComponent.processRequest(cmpRequestBytes);

// Send response via custom transport
sendViaCustomTransport(cmpResponseBytes);
```

**Example**: MQTT-based CMP

```java
public class MqttCmpBridge {

    private MqttClient mqttClient;
    private CmpRaComponent raComponent;

    public void onMqttMessage(String topic, byte[] payload) {
        if (topic.equals("cmp/requests")) {
            // Process CMP request
            byte[] response = raComponent.processRequest(payload);

            // Publish response
            mqttClient.publish("cmp/responses", response);
        }
    }
}
```

---

## 11. Operational Characteristics

### 11.1 Scalability

#### Stateless Design

**Advantage**: Horizontal scaling without session affinity

**Details**:
- Each CMP request is self-contained
- No server-side session state (except asynchronous transactions)
- Load balancer can route requests to any RA instance

**Scaling Pattern**:
```
                    ┌─────────────┐
                    │Load Balancer│
                    └──────┬──────┘
                           │
         ┌─────────────────┼─────────────────┐
         │                 │                 │
         ▼                 ▼                 ▼
    ┌────────┐        ┌────────┐        ┌────────┐
    │ RA     │        │ RA     │        │ RA     │
    │Instance│        │Instance│        │Instance│
    │   1    │        │   2    │        │   3    │
    └────┬───┘        └────┬───┘        └────┬───┘
         │                 │                 │
         └─────────────────┼─────────────────┘
                           │
                           ▼
                    ┌─────────────┐
                    │  Shared DB  │
                    │(Persistence)│
                    └─────────────┘
```

#### Asynchronous Operation Support

**Stateful Transactions**: Stored in shared database via PersistencyInterface

**Recovery**: Any RA instance can complete transaction started by another instance

### 11.2 Performance

#### Message Processing

**Throughput**: Dependent on:
- Cryptographic operations (signing, verification)
- Certificate chain validation
- Network latency to CA
- Database query performance (inventory, persistence)

**Typical Performance** (based on Siemens production deployments):
- Simple enrollment (cached validation): 50-100 requests/second per instance
- Complex enrollment (CRL download): 10-20 requests/second
- Revocation: 100+ requests/second

**Optimization Strategies**:
- Certificate chain caching
- CRL caching (honor nextUpdate time)
- OCSP response caching
- Connection pooling (database, HTTP to CA)

#### Memory Usage

**Baseline**: ~100 MB per RA instance

**Per-Request**: ~1-5 MB (depending on certificate chain size)

**Recommendation**: 512 MB heap for production (handles 50+ concurrent requests)

### 11.3 Reliability

#### Error Handling

**CMP-Level Errors** (returned as CMP error messages):
- `badMessageCheck` - Message signature invalid
- `badRequest` - Malformed request
- `badCertId` - Invalid certificate identifier (revocation)
- `badDataFormat` - Invalid ASN.1 encoding
- `wrongAuthority` - Request sent to wrong RA/CA
- `notAuthorized` - Authorization failed
- `systemUnavail` - Upstream CA unreachable

**Application-Level Errors** (Java exceptions):
- Configuration errors (invalid settings)
- Cryptographic errors (key/certificate issues)
- Database errors (persistence failures)

#### Transaction Recovery

**Scenario**: RA crashes during pending transaction

**Recovery Process**:
1. Application restarts
2. Query `PersistencyInterface.listPendingTransactions()`
3. For each transaction:
   - Attempt to complete (poll CA for result)
   - If CA responded, send to client (when client re-polls)
   - If CA timed out, mark as failed
   - Clean up completed transactions

#### Monitoring

**Health Checks**:
- Upstream CA connectivity
- Database connectivity
- Certificate/key validity
- Disk space (for logging)

**Metrics** (via SLF4J logging):
- Requests processed (total, per profile)
- Request processing time (average, p95, p99)
- Error rate (by error type)
- Cache hit rate (certificate chains, CRLs)

**Logging Levels**:
- `ERROR`: Critical failures requiring immediate attention
- `WARN`: Recoverable errors, authorization failures
- `INFO`: Request completion, configuration changes
- `DEBUG`: Detailed message processing, configuration lookups
- `TRACE`: Raw message dumps (ASN.1 DER hex)

---

## 12. Deployment Models

### 12.1 Standalone RA (LightweightCmpRa)

**Use Case**: Small to medium deployments, testing, PoC

**Architecture**:
```
┌──────────────────────────────────────┐
│     LightweightCmpRa Application     │
│  ┌────────────────────────────────┐  │
│  │  HTTP Servlet (Tomcat Embedded)│  │
│  └────────────���───────────────────┘  │
│               │                       │
│  ┌────────────▼───────────────────┐  │
│  │     CMP RA Component (Library) │  │
│  └────────────┬───────────────────┘  │
│               │                       │
│  ┌────────────▼───────────────────┐  │
│  │  YAML Configuration + Inventory│  │
│  └────────────────────────────────┘  │
└──────────────────────────────────────┘
```

**Deployment**:
```bash
# Run as standalone JAR
java -jar LightweightCmpRa-1.0.0.jar config.yaml

# Or as systemd service
[Unit]
Description=Lightweight CMP RA
After=network.target

[Service]
Type=simple
User=ra
ExecStart=/usr/bin/java -jar /opt/ra/LightweightCmpRa.jar /etc/ra/config.yaml
Restart=always

[Install]
WantedBy=multi-user.target
```

### 12.2 Enterprise RA (Custom Application with CMP RA Component)

**Use Case**: Large deployments, custom business logic, enterprise integration

**Architecture**:
```
┌─────────────────────────────────────────────────────────┐
│             Custom Enterprise RA Application             │
│                                                          │
│  ┌────────────────────────────────────────────────────┐ │
│  │  Web Layer (Spring Boot REST Controllers)         │ │
│  └────────────┬───────────────────────────────────────┘ │
│               │                                          │
│  ┌────────────▼───────────────────────────────────────┐ │
│  │  Business Logic (Authorization, Workflow)         │ │
│  │  - Active Directory Integration                   │ │
│  │  - Approval Workflow Engine                       │ │
│  │  - Certificate Inventory Management               │ │
│  └────────────┬───────────────────────────────────────┘ │
│               │                                          │
│  ┌────────────▼───────────────────────────────────────┐ │
│  │  CMP RA Component (via InventoryInterface)        │ │
│  └────────────┬───────────────────────────────────────┘ │
│               │                                          │
│  ┌────────────▼───────────────────────────────────────┐ │
│  │  Data Layer (PostgreSQL, LDAP, EJBCA API)        │ │
│  └────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
```

**Integration Points**:
- **InventoryInterface**: Custom implementation with AD, database, approval engine
- **PersistencyInterface**: Database-backed transaction storage
- **Configuration**: Dynamic configuration from database or config service

### 12.3 Siemens SICAM GridPass (Production Example)

**Product**: Siemens SICAM GridPass (Industrial PKI solution for power grid)

**Architecture**:
```
┌──────────────────────────────────────────────────────────┐
│            SICAM GridPass Management System               │
│  ┌────────────────────────────────────────────────────┐  │
│  │  Web UI (User Management, Certificate Lifecycle)  │  │
│  └────────────┬───────────────────────────────────────┘  │
│               │                                           │
│  ┌────────────▼───────────────────────────────────────┐  │
│  │  RA Backend (Siemens CMP RA Component)            │  │
│  │  - IEC 62351 compliance                           │  │
│  │  - Device certificate management                  │  │
│  │  - Automated renewal                              │  │
│  └────────────┬───────────────────────────────────────┘  │
│               │                                           │
│  ┌────────────▼───────────────────────────────────────┐  │
│  │  Certificate Authority (Siemens SICAM CA)         │  │
│  └────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────┘
```

**Scale**: Manages thousands of device certificates for critical infrastructure

**Deployment**: On-premises, air-gapped environments

---

## 13. Comparison with Product Requirements

### 13.1 Similarities

#### Functional Alignment

| **Requirement** | **Siemens Implementation** | **Status** |
|----------------|----------------------------|-----------|
| Certificate enrollment | CMP ir, cr, p10cr, kur | ✅ Implemented |
| Certificate revocation | CMP rr message | ✅ Implemented |
| CA certificate distribution | CMP genm/genp | ✅ Implemented |
| Authorization interface | InventoryInterface | ✅ Implemented |
| Audit logging | SLF4J framework | ✅ Implemented |
| Multiple CAs support | Profile-based routing | ✅ Implemented |
| Asynchronous operations | Transaction persistence | ✅ Implemented |
| Transport flexibility | HTTP, HTTPS, CoAP, File | ✅ Implemented |

### 13.2 Key Differences

| **Aspect** | **Our Requirements** | **Siemens Implementation** |
|-----------|---------------------|---------------------------|
| **Protocol** | Generic (supports PKCS#10, REST API) | CMP-focused (RFC 9483, RFC 9810) |
| **Authentication** | Active Directory LDAP | Configurable (MAC, PKI, can integrate AD via InventoryInterface) |
| **User Interface** | Web portal for end users | CLI-based (LightweightCmpRa), API-only (CMP RA Component) |
| **Auto-Enrollment** | Policy-based automated issuance | Not built-in (can implement via InventoryInterface) |
| **Role-Based Access** | 5 roles (Admin, Officer, Operator, Auditor, End Entity) | Authorization via InventoryInterface (customizable) |
| **Approval Workflow** | Multi-level approval in UI | Authorization in InventoryInterface (custom implementation) |
| **Identity Verification** | Email, OTP, Face-to-face, Smart card | CMP message protection (MAC or PKI signature) |
| **Reporting** | Built-in dashboards and reports | Logging via SLF4J (reporting via external tools) |
| **REST API** | JWT-based REST API | CMP protocol (binary ASN.1 over HTTP) |

### 13.3 Gaps and Enhancements Needed

#### Gap 1: Web User Interface
**Requirement**: Self-service web portal for end users

**Siemens**: CLI-based RA (LightweightCmpRa), no web UI

**Enhancement**: Develop web UI on top of CMP RA Component:
```
┌─────────────────────────────────────┐
│     React/Angular Web Frontend     │
│  (Certificate request forms,        │
│   status tracking, downloads)       │
└──────────────┬──────────────────────┘
               │ REST API
               ▼
┌─────────────────────────────────────┐
│   Spring Boot Application Layer     │
│  - User authentication (AD)         │
│  - REST endpoints                   │
│  - Business logic                   │
└──────────────┬──────────────────────┘
               │ CMP Client API
               ▼
┌─────────────────────────────────────┐
│    CMP RA Component (Backend)       │
│  - CMP message processing           │
│  - CA communication                 │
└─────────────────────────────────────┘
```

#### Gap 2: Active Directory Integration
**Requirement**: Automatic user synchronization, group-based authorization

**Siemens**: Not built-in (InventoryInterface is generic)

**Enhancement**: Implement ADInventoryAdapter:
```java
public class ADInventoryAdapter implements InventoryInterface {

    private LdapTemplate ldapTemplate;

    @Override
    public CertTemplate checkAndModifyCertRequest(
        CertTemplate certTemplate,
        String certProfile,
        String requesterDN
    ) throws Exception {

        // Query AD for user
        LdapUser user = ldapTemplate.search(
            query().where("distinguishedName").is(requesterDN),
            new LdapUserMapper()
        ).stream().findFirst().orElse(null);

        if (user == null) {
            throw new Exception("User not found in Active Directory");
        }

        // Check group membership
        if (!user.getMemberOf().contains("PKI-Users")) {
            throw new Exception("User not authorized for certificates");
        }

        // Validate subject DN matches AD
        String adCN = user.getDisplayName();
        String requestedCN = extractCN(certTemplate);
        if (!adCN.equals(requestedCN)) {
            throw new Exception("Subject DN does not match AD profile");
        }

        return certTemplate;
    }
}
```

#### Gap 3: Auto-Enrollment
**Requirement**: Automatic certificate issuance based on AD group membership

**Siemens**: Not built-in

**Enhancement**: Implement scheduled job:
```java
@Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
public void performAutoEnrollment() {

    // Query AD for users in auto-enrollment groups
    List<LdapUser> eligibleUsers = adService.getUsersByGroup("VPN-Users");

    for (LdapUser user : eligibleUsers) {
        // Check if user already has certificate
        if (certificateRepository.hasActiveCertificate(user.getDN(), "vpn-cert")) {
            continue;
        }

        // Generate key pair for user
        KeyPair keyPair = keyGenerator.generateKeyPair();

        // Create CMP enrollment request
        byte[] cmpRequest = cmpClientComponent.buildEnrollmentRequest(
            user.getDN(),
            keyPair.getPublic(),
            "vpn-cert"
        );

        // Submit to RA
        byte[] cmpResponse = cmpRaComponent.processRequest(cmpRequest);

        // Parse response and store certificate
        X509Certificate cert = cmpClientComponent.parseCertificate(cmpResponse);
        certificateRepository.save(cert, user.getDN());

        // Notify user
        emailService.sendCertificateIssuedEmail(user.getEmail(), cert);
    }
}
```

#### Gap 4: Approval Workflow UI
**Requirement**: RA Officers review and approve requests via web interface

**Siemens**: Authorization in InventoryInterface (programmatic)

**Enhancement**: Implement approval queue:
```java
@Override
public CertTemplate checkAndModifyCertRequest(
    CertTemplate certTemplate,
    String certProfile,
    String requesterDN
) throws Exception {

    // For high-value certificates, require manual approval
    if (certProfile.equals("code-signing") || certProfile.equals("admin")) {

        // Create approval request in database
        ApprovalRequest approvalRequest = new ApprovalRequest();
        approvalRequest.setRequesterDN(requesterDN);
        approvalRequest.setCertProfile(certProfile);
        approvalRequest.setCertTemplate(certTemplate);
        approvalRequest.setStatus("PENDING");
        approvalRequestRepository.save(approvalRequest);

        // Throw exception to defer processing
        throw new ApprovalRequiredException("Manual approval required");
    }

    // Auto-approve low-value certificates
    return certTemplate;
}
```

Web UI displays pending approvals, officer clicks approve/reject, system re-processes request.

#### Gap 5: RESTful API
**Requirement**: JWT-based REST API for programmatic access

**Siemens**: CMP protocol (binary, ASN.1)

**Enhancement**: Add REST API layer:
```java
@RestController
@RequestMapping("/api/v1/certificates")
public class CertificateController {

    @Autowired
    private CmpClientComponent cmpClient;

    @PostMapping("/requests")
    public ResponseEntity<CertificateRequestResponse> submitCSR(
        @RequestHeader("Authorization") String bearerToken,
        @RequestBody CSRSubmissionRequest request
    ) {
        // Validate JWT token
        User user = jwtService.validateToken(bearerToken);

        // Convert REST API CSR to CMP message
        byte[] cmpRequest = cmpClient.buildEnrollmentRequest(
            user.getDN(),
            parsePKCS10(request.getCsr()),
            request.getTemplateId()
        );

        // Submit to RA
        byte[] cmpResponse = raComponent.processRequest(cmpRequest);

        // Parse CMP response to REST response
        CertificateRequestResponse response = parseCmpResponse(cmpResponse);
        return ResponseEntity.ok(response);
    }
}
```

### 13.4 Reusability Assessment

**CMP RA Component Library**: ✅ **Highly Reusable**
- Use as foundation for custom RA application
- Handles all CMP protocol complexity
- Production-grade, battle-tested
- Extensible via interfaces

**LightweightCmpRa Application**: ⚠️ **Partially Reusable**
- Good for testing and PoC
- CLI-based (not suitable for end-user portal)
- YAML configuration (can inspire our config structure)
- Transport adapters reusable

**Recommended Approach**:
1. Use **CMP RA Component** as core library (Maven dependency)
2. Build custom web application around it (Spring Boot)
3. Implement **InventoryInterface** with AD integration
4. Implement **PersistencyInterface** with PostgreSQL
5. Add web UI (React/Angular) for user-facing operations
6. Add REST API layer for programmatic access
7. Keep CMP protocol support for IoT/industrial devices

---

## Appendices

### Appendix A: References

**Standards**:
- RFC 9810: Certificate Management Protocol (CMP)
- RFC 9483: Lightweight CMP Profile
- RFC 9480: CMP Updates
- RFC 9481: CMP Algorithms
- RFC 9811: HTTP Transfer for CMP
- RFC 4210: CMPv2
- RFC 4211: CRMF (Certificate Request Message Format)
- RFC 2986: PKCS#10 Certificate Request Syntax
- RFC 5280: X.509 Public Key Infrastructure

**Siemens Resources**:
- GitHub: https://github.com/siemens/cmp-ra-component
- GitHub: https://github.com/siemens/LightweightCmpRa
- Maven Central: com.siemens.pki:CmpRaComponent
- SICAM GridPass: https://new.siemens.com/global/en/products/energy/grid-automation/sicam-gridpass.html

**IETF Participation**:
- CMP RA Component served as reference implementation for RFC 9483 standardization

### Appendix B: Glossary

| Term | Definition |
|------|-----------|
| **CMP** | Certificate Management Protocol, protocol for automated certificate lifecycle management |
| **CRMF** | Certificate Request Message Format, format for certificate requests in CMP |
| **ASN.1** | Abstract Syntax Notation One, standard for encoding data structures |
| **DER** | Distinguished Encoding Rules, binary encoding for ASN.1 |
| **MAC** | Message Authentication Code, cryptographic checksum using shared secret |
| **HMAC** | Hash-based Message Authentication Code |
| **PKI** | Public Key Infrastructure |
| **PoP** | Proof of Possession, proof that requester has private key |
| **PKCS#10** | Public Key Cryptography Standard #10, legacy CSR format |
| **ir** | Initial Request, CMP message type for first enrollment |
| **cr** | Certificate Request, CMP message type for additional certificates |
| **kur** | Key Update Request, CMP message type for renewal |
| **rr** | Revocation Request, CMP message type for revocation |
| **genm/genp** | General Message/General Response, CMP message types for CA cert distribution, CRL retrieval |

### Appendix C: Code Examples

**Complete Enrollment Example**:

See GitHub repository examples:
- `siemens/LightweightCmpRa/src/test/java/.../test/examples/`

**Maven Dependency**:
```xml
<dependency>
    <groupId>com.siemens.pki</groupId>
    <artifactId>CmpRaComponent</artifactId>
    <version>3.2.0</version>
</dependency>
```

---

**Document End**

**Sources**:
- [siemens/cmp-ra-component](https://github.com/siemens/cmp-ra-component)
- [siemens/LightweightCmpRa](https://github.com/siemens/LightweightCmpRa)
- RFC 9483: Lightweight CMP Profile
- RFC 9810: Certificate Management Protocol

**Prepared By**: Product Analysis Team
**Review Status**: Draft for Internal Review
**Next Steps**: Gap analysis, integration planning, proof-of-concept implementation
