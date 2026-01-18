# Registration Authority (RA) Web Application - MVP Requirements

## Project Overview
A web-based Registration Authority system for managing digital certificate lifecycle operations with Active Directory integration for end entity authentication and role-based access control for local RA operations.

---

## 1. End Entity Authentication Mechanism using Active Directory

### 1.1 Authentication Requirements
- **LDAP/AD Integration**: Connect to organizational Active Directory for user authentication
- **Single Sign-On (SSO)**: Support Windows Authentication/Kerberos for seamless login
- **Credential Validation**: Authenticate users against AD credentials (username/password)
- **Session Management**: Maintain secure user sessions with appropriate timeouts
- **Multi-Factor Authentication (MFA)**: Optional support for additional authentication factors

### 1.2 User Attributes from AD
Retrieve and map the following attributes from Active Directory:
- `sAMAccountName` - Username
- `distinguishedName` - Full DN path
- `mail` - Email address
- `displayName` - Full name
- `department` - Department/Organizational unit
- `memberOf` - Group memberships for role mapping
- `employeeID` - Employee identifier
- `telephoneNumber` - Contact information

### 1.3 Technical Implementation
```
Authentication Flow:
1. User submits credentials via login form
2. Application connects to AD server via LDAP/LDAPS
3. Bind operation validates credentials
4. Retrieve user attributes and group memberships
5. Map AD groups to application roles
6. Create authenticated session
7. Redirect to appropriate dashboard based on role
```

### 1.4 Configuration Parameters
- AD Server URL/Hostname
- LDAP Port (389/636 for LDAPS)
- Base DN for user searches
- Service account credentials for AD queries
- SSL/TLS certificate validation
- Connection timeout and retry settings

---

## 2. Local RA Operation User Roles

### 2.1 Role Hierarchy

#### **2.1.1 RA Administrator**
**Permissions:**
- Full system configuration access
- User role management and assignment
- Audit log review and export
- System health monitoring
- Policy configuration and enforcement
- Certificate template management
- Integration settings (CA, AD, etc.)

**Typical Users:** IT Security Managers, PKI Administrators

#### **2.1.2 RA Officer**
**Permissions:**
- Approve/reject certificate requests
- Revoke certificates
- View certificate details and history
- Search and filter requests
- Generate reports
- Verify end entity identity documents
- Issue certificates after approval

**Typical Users:** Security Officers, Certificate Managers

#### **2.1.3 RA Operator**
**Permissions:**
- Submit certificate requests on behalf of end entities
- View own submitted requests
- Update request information (pre-approval)
- Download issued certificates
- View request status
- Upload supporting documents

**Typical Users:** Help Desk Staff, Departmental Coordinators

#### **2.1.4 Auditor (Read-Only)**
**Permissions:**
- View all certificate operations
- Access audit logs
- Generate compliance reports
- Export data for analysis
- NO modification capabilities

**Typical Users:** Compliance Officers, Internal Auditors

#### **2.1.5 End Entity (Self-Service)**
**Permissions:**
- Submit own certificate requests
- View own certificate status
- Download own certificates
- Renew own certificates
- View certificate expiration notices

**Typical Users:** All authenticated employees/users

### 2.2 Role-to-AD Group Mapping
```
AD Group                          → Application Role
---------------------------------------------------
"PKI-RA-Admins"                  → RA Administrator
"PKI-RA-Officers"                → RA Officer
"PKI-RA-Operators"               → RA Operator
"PKI-Auditors"                   → Auditor
"Domain Users" (default)         → End Entity
```

### 2.3 Access Control Matrix

| Function                          | Admin | Officer | Operator | Auditor | End Entity |
|-----------------------------------|-------|---------|----------|---------|------------|
| Submit Certificate Request        | ✓     | ✓       | ✓        | ✗       | ✓ (self)   |
| Upload PKCS#10 CSR               | ✓     | ✓       | ✓        | ✗       | ✓ (self)   |
| Approve Certificate Request       | ✓     | ✓       | ✗        | ✗       | ✗          |
| Reject Certificate Request        | ✓     | ✓       | ✗        | ✗       | ✗          |
| Revoke Certificate               | ✓     | ✓       | ✗        | ✗       | ✗          |
| View All Requests                | ✓     | ✓       | ✗        | ✓       | ✗          |
| View Own Requests                | ✓     | ✓       | ✓        | ✓       | ✓          |
| Manage Users/Roles               | ✓     | ✗       | ✗        | ✗       | ✗          |
| Configure System                 | ✓     | ✗       | ✗        | ✗       | ✗          |
| Configure Certificate Templates  | ✓     | ✗       | ✗        | ✗       | ✗          |
| Enable/Disable Auto-Enrollment   | ✓     | ✗       | ✗        | ✗       | ✗          |
| Trigger Bulk Auto-Enrollment     | ✓     | ✓       | ✗        | ✗       | ✗          |
| View Audit Logs                  | ✓     | ✓       | ✗        | ✓       | ✗          |
| Export Reports                   | ✓     | ✓       | ✗        | ✓       | ✗          |
| Download Certificates            | ✓     | ✓       | ✓        | ✗       | ✓ (self)   |

---

## 3. MVP Core Features

### 3.1 Auto-Enrollment Mechanism (Similar to Microsoft Auto-Enrollment)

#### 3.1.1 Overview
Automatic certificate enrollment allows end entities to receive certificates without manual intervention, similar to Microsoft's Group Policy-based auto-enrollment. The RA system automatically detects eligible users, generates certificate requests, and issues certificates based on predefined policies.

#### 3.1.2 Auto-Enrollment Features

**Policy-Based Auto-Enrollment:**
- Define certificate templates with auto-enrollment policies
- Map AD groups to certificate templates
- Automatic eligibility detection based on user attributes
- Scheduled enrollment checks (login-triggered or time-based)

**Enrollment Triggers:**
1. **User Login**: Certificate check on AD authentication
2. **Scheduled Job**: Background service checks for missing/expiring certificates
3. **Manual Trigger**: Administrator-initiated bulk enrollment
4. **Group Membership Change**: Auto-enroll when user joins specific AD group

#### 3.1.3 Auto-Enrollment Workflow

```
Auto-Enrollment Process:
1. User authenticates via Active Directory
2. RA checks user's AD group memberships
3. Match groups against certificate templates with auto-enroll enabled
4. Check if user already has valid certificate for template
5. If missing or expiring within threshold:
   a. Generate key pair (client-side or server-side based on template)
   b. Create CSR with user's AD attributes
   c. Auto-approve if template allows (or queue for RA Officer approval)
   d. Submit to CA for issuance
   e. Import certificate to user's certificate store
6. Notify user of new certificate availability
7. Log all actions in audit trail
```

#### 3.1.4 Certificate Templates for Auto-Enrollment

**Template Configuration:**
```
Template Properties:
- Template Name: "Employee Email Certificate"
- Auto-Enrollment Enabled: Yes
- Auto-Approval: Yes/No (if No, requires RA Officer approval)
- Eligible AD Groups: ["All Employees", "Email Users"]
- Key Size: 2048/4096
- Validity Period: 1 year
- Renewal Threshold: 30 days before expiry
- Key Usage: Digital Signature, Key Encipherment
- Extended Key Usage: Email Protection, Client Authentication
- Subject DN Template: CN=${displayName}, E=${mail}, OU=${department}
- SAN Template: email:${mail}, UPN:${userPrincipalName}
```

**Template Types:**
1. **User Certificates**: Email, VPN, Smart Card logon
2. **Computer Certificates**: Device authentication, IPsec
3. **Service Account Certificates**: Service authentication

#### 3.1.5 Certificate Distribution Methods

**Automatic Distribution:**
- **Web Download Portal**: User logs in and downloads certificate/private key
- **Browser Certificate Store**: Direct import to browser (for web-based enrollment)
- **Windows Certificate Store**: Integration via CAPI/CNG (for Windows clients)
- **Email Delivery**: Encrypted PKCS#12 file sent to user's email
- **Network Share**: Deploy to shared location with user permissions

**Client Agent (Optional):**
- Lightweight desktop agent monitors certificate status
- Automatic key generation on client machine
- Direct integration with OS certificate store
- Background renewal without user interaction

#### 3.1.6 Auto-Renewal Process

```
Auto-Renewal Flow:
1. Background job checks certificates expiring within renewal threshold
2. For auto-renewal enabled templates:
   a. Verify user still meets eligibility criteria
   b. Generate new key pair
   c. Create renewal request with same subject
   d. Auto-approve and submit to CA
   e. Issue new certificate
   f. Notify user of renewal
3. Grace period: Old certificate remains valid during transition
4. Optional: Auto-revoke old certificate after successful renewal
```

#### 3.1.7 Configuration Settings

**Administrator Configuration Panel:**
```
Auto-Enrollment Settings:
- Enable/Disable Auto-Enrollment globally
- Enrollment trigger frequency (hourly, daily, on-login)
- Renewal threshold (days before expiry)
- Auto-approval rules per template
- Maximum certificates per user
- Key generation location (client/server)
- Certificate delivery methods
- Notification preferences
```

#### 3.1.8 End Entity Experience

**Seamless Enrollment:**
1. User logs into RA portal using AD credentials
2. Dashboard shows: "Certificate enrollment in progress..."
3. Background process completes enrollment
4. Dashboard updates: "New certificate available for download"
5. One-click download or automatic browser import
6. User receives email confirmation with certificate details

**No Action Required Scenario:**
- User logs in, certificate automatically checked
- If missing, auto-enrollment initiates in background
- Certificate silently installed (if client agent present)
- User notified via email: "Certificate installed successfully"

### 3.2 Certificate Request Management (Manual Enrollment)

#### 3.2.1 End Entity Authentication for CSR Submission

**Critical Security Requirement**: Before RA accepts any CSR, the end entity MUST be authenticated to prevent unauthorized certificate requests.

**Multi-Layer Authentication Approach:**

**Layer 1: Active Directory Authentication (Mandatory)**
```
CSR Submission Flow with Authentication:
1. End entity MUST login to RA portal using AD credentials
2. AD validates username/password via LDAP/LDAPS
3. RA retrieves user attributes from AD (DN, email, department, groups)
4. Session established with authenticated user identity
5. Only after successful AD authentication, CSR upload is permitted
```

**Layer 2: CSR Subject DN Validation (Mandatory)**
```
After CSR upload, RA validates:
1. Extract Subject DN from uploaded CSR
2. Compare CSR Subject DN with authenticated AD user attributes:
   - CN (Common Name) must match AD displayName or username
   - Email must match AD mail attribute
   - OU must match AD department/organizational unit
   - Organization must match company policy
3. If mismatch detected → REJECT with error message
4. Prevents user from requesting certificate for someone else
```

**Layer 3: Proof of Possession (Automatic)**
```
PKCS#10 CSR contains digital signature:
1. CSR is signed with private key corresponding to public key in CSR
2. RA verifies signature on CSR
3. Signature verification proves:
   - Requester possesses private key
   - CSR has not been tampered with
   - Public key in CSR is authentic
4. Invalid signature → REJECT CSR
```

**Layer 4: Additional Identity Verification (Policy-Based)**

**Option A: Email Verification (Low-Security Certificates)**
```
1. After CSR upload, RA sends verification email to AD registered email
2. Email contains unique token/link valid for 24 hours
3. User must click link to confirm CSR submission
4. Prevents unauthorized requests even if account compromised
```

**Option B: One-Time Password (OTP) (Medium-Security Certificates)**
```
1. User uploads CSR
2. RA sends OTP to user's registered mobile number (from AD)
3. User enters OTP on portal within 5 minutes
4. OTP verified → CSR accepted for RA Officer review
```

**Option C: Face-to-Face Verification (High-Security Certificates)**
```
For high-value certificates (Code Signing, Admin, Root Access):
1. User uploads CSR online
2. CSR marked as "Pending Identity Verification"
3. User must visit RA Officer in person with:
   - Government-issued photo ID
   - Employee ID card
   - CSR reference number
4. RA Officer verifies identity and photo
5. RA Officer approves identity verification in system
6. CSR moves to approval workflow
```

**Option D: Smart Card/Hardware Token (Highest Security)**
```
1. User already has issued employee smart card with certificate
2. Login to RA portal requires smart card authentication
3. RA validates existing certificate on smart card
4. User authenticated via PKI (existing certificate)
5. CSR upload permitted only after smart card auth
6. Binds new certificate request to existing trusted identity
```

**Layer 5: Authorization Checks (Policy Enforcement)**
```
After authentication, RA checks authorization:
1. Is user authorized to request certificates?
   - Check AD group memberships
   - Verify user account is active (not disabled/locked)
   - Check if user has exceeded certificate quota
2. Is user authorized for requested certificate template?
   - Email certificates: All employees
   - VPN certificates: Remote workers group
   - Code Signing: Developers group only
   - Admin certificates: IT Admins group only
3. Unauthorized → REJECT with "Insufficient privileges" error
```

**Complete Authentication Flow for CSR Submission:**

```
┌─────────────────────────────────────────────────────────┐
│ Step 1: AD Authentication (Mandatory)                   │
├─────────────────────────────────────────────────────────┤
│ User → Login with AD credentials                        │
│ RA → Validate via LDAP                                  │
│ RA → Retrieve user attributes (CN, Email, OU, Groups)   │
│ RA → Create authenticated session                       │
└──────────────────┬──────────────────────────────────────┘
                   │ ✓ Authenticated
                   ▼
┌─────────────────────────────────────────────────────────┐
│ Step 2: CSR Upload                                      │
├─────────────────────────────────────────────────────────┤
│ User → Upload PKCS#10 CSR file                         │
│ RA → Parse CSR and extract Subject DN                  │
│ RA → Verify CSR signature (Proof of Possession)        │
└──────────────────┬──────────────────────────────────────┘
                   │ ✓ Valid CSR
                   ▼
┌─────────────────────────────────────────────────────────┐
│ Step 3: Subject DN Validation (Identity Match)         │
├─────────────────────────────────────────────────────────┤
│ Compare:                                                │
│   CSR CN = "John Doe"  vs  AD displayName = "John Doe" │
│   CSR Email = "john@company.com"  vs  AD mail          │
│   CSR OU = "Engineering"  vs  AD department            │
│                                                         │
│ If MATCH → Proceed                                     │
│ If MISMATCH → REJECT: "CSR subject does not match     │
│                        your AD profile"                │
└──────────────────┬──────────────────────────────────────┘
                   │ ✓ Identity Verified
                   ▼
┌─────────────────────────────────────────────────────────┐
│ Step 4: Additional Verification (Policy-Based)         │
├─────────────────────────────────────────────────────────┤
│ For Low-Security Certs: Email verification (optional)   │
│ For Medium-Security: OTP verification                   │
│ For High-Security: Face-to-face verification           │
│ For Critical Certs: Smart card authentication          │
└──────────────────┬──────────────────────────────────────┘
                   │ ✓ Additional Verification Passed
                   ▼
┌─────────────────────────────────────────────────────────┐
│ Step 5: Authorization Check                            │
├─────────────────────────────────────────────────────────┤
│ Check:                                                  │
│ - User account active?                                  │
│ - User in authorized AD group for template?            │
│ - Certificate quota not exceeded?                      │
│ - Requested key usage allowed for user?                │
│                                                         │
│ If AUTHORIZED → Accept CSR                             │
│ If UNAUTHORIZED → REJECT                               │
└──────────────────┬──────────────────────────────────────┘
                   │ ✓ Authorized
                   ▼
┌─────────────────────────────────────────────────────────┐
│ Step 6: CSR Accepted → Submit for RA Officer Approval  │
├─────────────────────────────────────────────────────────┤
│ - CSR stored in database with user identity            │
│ - Request ID generated                                  │
│ - RA Officer notified for review                       │
│ - Audit log entry created                              │
└─────────────────────────────────────────────────────────┘
```

**Security Policies by Certificate Type:**

| Certificate Type | AD Auth | DN Match | Email Verify | OTP | Face-to-Face | Smart Card |
|-----------------|---------|----------|--------------|-----|--------------|------------|
| Email/S/MIME    | ✓       | ✓        | Optional     | ✗   | ✗            | ✗          |
| VPN/Network     | ✓       | ✓        | ✓            | ✓   | ✗            | ✗          |
| Code Signing    | ✓       | ✓        | ✓            | ✓   | ✓            | ✗          |
| Admin/Root      | ✓       | ✓        | ✓            | ✓   | ✓            | ✓          |
| Server SSL/TLS  | ✓       | ✓        | ✓            | ✓   | Optional     | ✗          |

**Preventing Common Attacks:**

**Attack 1: Impersonation (User A requests cert for User B)**
```
Prevention:
- Mandatory AD authentication
- CSR Subject DN must match authenticated user's AD attributes
- Cannot submit CSR with someone else's name/email
```

**Attack 2: Stolen Credentials**
```
Prevention:
- Multi-factor authentication (OTP, Smart Card)
- Email verification to registered email
- IP address logging and geo-location checks
- Unusual activity alerts to RA Officers
```

**Attack 3: Insider Threat (RA Operator abuse)**
```
Prevention:
- Operators can submit on behalf but with justification
- All submissions logged with operator identity
- RA Officer must approve (separation of duties)
- Audit logs are immutable and monitored
```

**Attack 4: Replay Attack (Re-submit old CSR)**
```
Prevention:
- CSR public key hash checked against previously issued certs
- Duplicate public keys rejected
- Each CSR submission gets unique request ID with timestamp
- CSRs expire after 30 days if not processed
```

#### 3.2.2 Request Submission Methods

**Method 1: PKCS#10 CSR Upload (Recommended for Security)**
End entities can generate their own key pair locally and submit a signed Certificate Signing Request (CSR) in PKCS#10 format.

**Advantages:**
- Private key never leaves end entity's machine (highest security)
- User has full control over key generation
- Supports hardware token/smart card generated keys
- Industry standard format (RFC 2986)
- Digital signature proves possession of private key

**CSR Submission Process:**
```
1. End entity generates key pair locally using:
   - OpenSSL: openssl req -new -newkey rsa:2048 -nodes -keyout private.key -out request.csr
   - Java keytool: keytool -certreq -alias mykey -file request.csr
   - Web Crypto API (browser-based)
   - Hardware token (smart card, USB token)

2. End entity logs into RA portal
3. Navigate to "Submit Certificate Request" → "Upload CSR"
4. Upload PKCS#10 CSR file (.csr, .pem format)
5. RA validates CSR:
   - Verify signature on CSR
   - Extract subject DN and public key
   - Validate key size and algorithm
   - Check subject DN matches AD user attributes
6. Display extracted CSR information for user confirmation
7. Submit for RA Officer approval
8. Upon approval, CA signs and issues certificate
9. End entity downloads signed certificate only (private key already with them)
```

**PKCS#10 CSR Validation:**
- **Signature Verification**: Verify CSR is signed with corresponding private key
- **Format Check**: Validate PKCS#10 ASN.1 structure
- **Key Algorithm**: RSA 2048/4096, ECDSA P-256/P-384
- **Subject DN Validation**: Match against AD attributes (CN, E, OU, O, C)
- **Subject Alternative Names (SAN)**: Validate email, UPN, DNS names
- **Key Usage**: Verify requested extensions are allowed per policy
- **Blacklist Check**: Ensure public key not previously revoked

**Supported CSR Formats:**
- PEM encoded (Base64 with BEGIN/END CERTIFICATE REQUEST headers)
- DER binary format
- PKCS#10 standard (RFC 2986)

**Method 2: Web Form with Server-Side Key Generation**
For users who prefer convenience over maximum security:
```
1. Fill web form with certificate details
2. RA generates key pair on server
3. Create CSR internally
4. Submit for approval
5. After issuance, download PKCS#12 (.p12/.pfx) with certificate + private key
6. Import into certificate store with password protection
```

**Method 3: Web Form with Client-Side Key Generation (Browser-Based)**
Modern browsers support Web Crypto API for client-side key generation:
```
1. Fill web form
2. JavaScript generates key pair in browser using Web Crypto API
3. Create CSR client-side
4. Private key stored in browser's IndexedDB (encrypted)
5. Submit CSR to RA
6. Download certificate after approval
7. Browser automatically pairs with stored private key
```

#### 3.2.2 CSR Upload Interface Requirements

**Upload Form Fields:**
- **CSR File Upload**: Drag-and-drop or file browser (.csr, .pem, .der)
- **Certificate Template**: Select from available templates (Email, VPN, Code Signing, etc.)
- **Justification**: Business reason for certificate request
- **Validity Period**: Requested duration (subject to policy limits)
- **Additional SANs**: Optional additional email addresses or DNS names

**CSR Information Display (After Upload):**
```
Extracted CSR Details:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Subject DN:
  CN: John Doe
  E: john.doe@company.com
  OU: Engineering
  O: Company Name
  C: US

Public Key:
  Algorithm: RSA
  Key Size: 2048 bits
  Public Key Hash: SHA256:a1b2c3d4...

Requested Extensions:
  Key Usage: Digital Signature, Key Encipherment
  Extended Key Usage: Email Protection, Client Authentication

Subject Alternative Names:
  - email:john.doe@company.com
  - email:jdoe@company.com

Signature:
  Algorithm: SHA256withRSA
  Signature Valid: ✓ Verified

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[Confirm & Submit] [Cancel]
```

#### 3.2.3 PKCS#10 CSR Enrollment Workflow

```
End Entity CSR Submission Flow:
┌──────────────────┐
│ End Entity       │
│ (Local Machine)  │
└────────┬─────────┘
         │ 1. Generate Key Pair
         │    (OpenSSL/Keytool/HSM)
         ▼
┌──────────────────┐
│ Private Key      │ ← Stays on user's machine (never transmitted)
│ (Secure Storage) │
└──────────────────┘
         │
         │ 2. Create & Sign CSR
         ▼
┌──────────────────┐
│ PKCS#10 CSR      │
│ (Public Key +    │
│  Signature)      │
└────────┬─────────┘
         │ 3. Upload via HTTPS
         ▼
┌──────────────────┐
│ RA Web Portal    │
│ - Validate CSR   │
│ - Extract Info   │
│ - Create Request │
└────────┬─────────┘
         │ 4. Submit for Approval
         ▼
┌──────────────────┐
│ RA Officer       │
│ Review & Approve │
└────────┬─────────┘
         │ 5. Approved
         ▼
┌──────────────────┐
│ Certificate      │
│ Authority (CA)   │
│ Signs CSR        │
└────────┬─────────┘
         │ 6. Issued Certificate
         ▼
┌──────────────────┐
│ RA Web Portal    │
│ Certificate      │
│ Available        │
└────────┬─────────┘
         │ 7. Download Certificate
         ▼
┌──────────────────┐
│ End Entity       │
│ Import Cert      │
│ (Pairs with      │
│  Private Key)    │
└──────────────────┘
```

#### 3.2.4 CSR Generation Examples

**OpenSSL Command Line:**
```bash
# Generate private key and CSR in one command
openssl req -new -newkey rsa:2048 -nodes \
  -keyout private.key \
  -out request.csr \
  -subj "/C=US/O=Company/OU=IT/CN=John Doe/emailAddress=john@company.com"

# Generate CSR from existing private key
openssl req -new -key existing_private.key -out request.csr

# View CSR contents
openssl req -text -noout -in request.csr

# Verify CSR signature
openssl req -verify -in request.csr
```

**Java Keytool:**
```bash
# Generate key pair in keystore
keytool -genkeypair -alias mycert -keyalg RSA -keysize 2048 \
  -dname "CN=John Doe, OU=IT, O=Company, C=US" \
  -keystore mykeystore.jks

# Generate CSR from keystore
keytool -certreq -alias mycert -file request.csr -keystore mykeystore.jks

# Import signed certificate
keytool -importcert -alias mycert -file signed_cert.cer -keystore mykeystore.jks
```

**Browser-Based (JavaScript/Web Crypto API):**
```javascript
// Generate key pair
const keyPair = await window.crypto.subtle.generateKey(
  {
    name: "RSASSA-PKCS1-v1_5",
    modulusLength: 2048,
    publicExponent: new Uint8Array([1, 0, 1]),
    hash: "SHA-256"
  },
  true,
  ["sign", "verify"]
);

// Create CSR (using third-party library like PKI.js)
const pkcs10 = new org.pkijs.simpl.PKCS10({
  subject: {
    typesAndValues: [
      { type: "2.5.4.3", value: "John Doe" },
      { type: "1.2.840.113549.1.9.1", value: "john@company.com" }
    ]
  },
  attributes: [],
  subjectPublicKeyInfo: publicKeyInfo
});

// Sign CSR
await pkcs10.sign(keyPair.privateKey, "SHA-256");

// Export to PEM
const csrPEM = pkcs10.toSchema().toBER();
```

#### 3.2.5 Request Tracking & Status
- **Request Submission**: Web form with required fields (subject DN, key usage, validity period)
- **Key Generation Options**:
  - Upload PKCS#10 CSR (most secure)
  - Server-side key generation
  - Client-side browser key generation
- **Request Tracking**: Unique request ID with status tracking
- **Workflow States**: Draft → Submitted → Under Review → Approved/Rejected → Issued/Failed

### 3.3 Certificate Lifecycle Operations
- **Issuance**: Integration with CA to issue approved certificates
- **Renewal**: Automated renewal workflow before expiration
- **Revocation**: Support for revocation with reason codes (key compromise, cessation of operation, etc.)
- **Suspension**: Temporary certificate suspension capability
- **Reactivation**: Reactivate suspended certificates

### 3.4 User Interface Requirements
- **Dashboard**: Role-specific landing page with key metrics and pending actions
- **Request List**: Filterable, sortable table of certificate requests
- **Request Details**: Comprehensive view of request information and history
- **Approval Workflow**: Interface for officers to review and approve/reject requests
- **Certificate Search**: Search by serial number, subject, status, etc.
- **Profile Management**: User profile with certificate inventory

### 3.5 Security Requirements
- **Transport Security**: All communications over HTTPS/TLS
- **Authentication**: AD-based authentication with session management
- **Authorization**: Role-based access control on all operations
- **Audit Logging**: Comprehensive logging of all certificate operations
- **Input Validation**: Sanitize and validate all user inputs
- **CSRF Protection**: Anti-CSRF tokens on all state-changing operations
- **Session Security**: Secure session cookies with appropriate flags (Secure, HttpOnly, SameSite)

### 3.6 Integration Requirements
- **Certificate Authority**: EJBCA, Microsoft CA, or other RFC-compliant CA
- **Active Directory**: LDAP/LDAPS integration for authentication and user attributes
- **Database**: Persistent storage for requests, certificates metadata, and audit logs
- **Email Service**: Notifications for request status changes and certificate expiration

### 3.7 Audit and Compliance
- **Audit Trail**: Immutable log of all operations with timestamp, user, action, and result
- **Compliance Reports**: Pre-built reports for common compliance frameworks
- **Data Retention**: Configurable retention policy for audit logs and certificate records
- **Export Capability**: Export audit logs and reports in standard formats (CSV, PDF)

---

## 4. RESTful API Implementation

### 4.1 REST API Authentication & CSR Submission

#### 4.1.1 Overview
The RA system exposes RESTful endpoints for authentication and certificate operations. End entities can programmatically interact with the RA through secure API calls, enabling automation and integration with enterprise systems.

**Key Features:**
- **Secure Authentication**: Challenge-response mechanism with cryptographic verification
- **Stateless API**: JWT-based authentication for scalability
- **PKCS#10 Support**: Accept industry-standard CSR format
- **AD Integration**: Validate credentials against Active Directory
- **Rate Limiting**: Protection against abuse and DoS attacks
- **Comprehensive Audit**: All API operations logged for compliance

#### 4.1.2 API Authentication Flow

**Approach: Challenge-Response Authentication with Cryptographic Verification**

This approach ensures that passwords are NEVER transmitted over the network, even over HTTPS. Instead, cryptographic proof of password knowledge is provided.

```
Detailed Authentication Mechanism:

┌─────────────────────────────────────────────────────────────┐
│ Step 1: Request Challenge (No Authentication Required)      │
├─────────────────────────────────────────────────────────────┤
│ Client → POST /api/v1/auth/challenge                        │
│          Body: {"username": "kablu@company.com"}            │
│                                                             │
│ Server → Generates:                                         │
│          - Random 32-byte nonce (challenge)                 │
│          - Random 16-byte salt for PBKDF2                   │
│          - Unique challenge_id (UUID)                       │
│          - Expiration time (5 minutes)                      │
│                                                             │
│ Server → Stores challenge temporarily in memory/Redis       │
│                                                             │
│ Server → Returns:                                           │
│          {                                                  │
│            "challenge_id": "uuid-123",                      │
│            "challenge": "base64(nonce)",                    │
│            "salt": "base64(salt)",                          │
│            "algorithm": "AES-256-GCM",                      │
│            "expires_at": "ISO8601 timestamp"                │
│          }                                                  │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│ Step 2: Client-Side Processing (Happens on Client)         │
├─────────────────────────────────────────────────────────────┤
│ Client receives challenge                                   │
│                                                             │
│ Client derives encryption key from password:                │
│   key = PBKDF2(                                            │
│           password,                                         │
│           salt,                                             │
│           iterations=10000,                                 │
│           keyLength=256 bits                                │
│         )                                                   │
│                                                             │
│ Client creates response payload:                           │
│   payload = challenge + ":" + username + ":" + timestamp    │
│                                                             │
│ Client encrypts payload:                                    │
│   - Generate random 12-byte IV (for AES-GCM)               │
│   - encrypted = AES-256-GCM.encrypt(payload, key, IV)      │
│   - result = IV || encrypted (concatenated)                │
│   - response = base64(result)                              │
│                                                             │
│ Private key never leaves client, password never sent!       │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│ Step 3: Submit Encrypted Response                          │
├─────────────────────────────────────────────────────────────┤
│ Client → POST /api/v1/auth/login                           │
│          Body: {                                            │
│            "challenge_id": "uuid-123",                      │
│            "username": "kablu@company.com",                 │
│            "response": "base64(IV||encrypted)",             │
│            "client_info": {                                 │
│              "ip_address": "192.168.1.100",                 │
│              "user_agent": "RA-Client/1.0"                  │
│            }                                                │
│          }                                                  │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│ Step 4: Server-Side Verification (AD Authentication)       │
├─────────────────────────────────────────────────────────────┤
│ Server retrieves stored challenge using challenge_id        │
│                                                             │
│ Server checks:                                              │
│   - Challenge exists and not expired? ✓                    │
│   - Username matches challenge? ✓                          │
│                                                             │
│ Server authenticates against Active Directory:             │
│   1. LDAP bind to AD with service account                  │
│   2. Search for user: userPrincipalName = username         │
│   3. Verify user exists and is active                      │
│   4. Attempt LDAP bind with user's credentials             │
│      (This validates password against AD)                   │
│   5. If bind successful → user authenticated               │
│                                                             │
│ Server derives same key from password:                     │
│   key = PBKDF2(password_from_AD, salt, 10000, 256)        │
│                                                             │
│ Server decrypts response:                                  │
│   - Extract IV (first 12 bytes)                            │
│   - Extract encrypted data (remaining bytes)                │
│   - decrypted = AES-256-GCM.decrypt(encrypted, key, IV)    │
│   - Parse: received_challenge:username:timestamp           │
│                                                             │
│ Server validates:                                           │
│   ✓ Decryption successful (proves password knowledge)      │
│   ✓ Received challenge matches stored challenge            │
│   ✓ Received username matches request username             │
│   ✓ Timestamp within acceptable window (5 minutes)         │
│                                                             │
│ If all checks pass → Authentication successful!            │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│ Step 5: Issue JWT Token                                    │
├─────────────────────────────────────────────────────────────┤
│ Server retrieves user's AD groups and maps to roles:       │
│   - "PKI-RA-Admins" → RA_ADMIN                            │
│   - "PKI-RA-Officers" → RA_OFFICER                        │
│   - "Domain Users" → END_ENTITY                           │
│                                                             │
│ Server generates JWT access token:                         │
│   {                                                         │
│     "sub": "kablu@company.com",                            │
│     "userId": "12345",                                      │
│     "roles": ["END_ENTITY"],                               │
│     "iat": 1673518800,                                      │
│     "exp": 1673522400 (1 hour)                             │
│   }                                                         │
│   Signed with RS256 (private key)                          │
│                                                             │
│ Server generates refresh token (opaque, 7 days)            │
│                                                             │
│ Server creates user session record in database             │
│                                                             │
│ Server deletes used challenge (prevent reuse)              │
│                                                             │
│ Server logs authentication event to audit trail            │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│ Step 6: Return Token to Client                             │
├─────────────────────────────────────────────────────────────┤
│ Server → 200 OK                                             │
│          Body: {                                            │
│            "access_token": "eyJhbGc...",                    │
│            "token_type": "Bearer",                          │
│            "expires_in": 3600,                              │
│            "refresh_token": "opaque-token-...",             │
│            "user": {                                        │
│              "user_id": "12345",                            │
│              "username": "kablu@company.com",               │
│              "display_name": "Kablu",                       │
│              "email": "kablu@company.com",                  │
│              "department": "Engineering",                   │
│              "roles": ["END_ENTITY"]                        │
│            }                                                │
│          }                                                  │
│                                                             │
│ Client stores access_token for subsequent API calls        │
│ Client can now make authenticated requests!                │
└─────────────────────────────────────────────────────────────┘
```

**Security Benefits of This Approach:**

1. **Password Never Transmitted**: Even over HTTPS, password never travels over network
2. **Replay Attack Prevention**:
   - Challenge used only once
   - Timestamp validation (5-minute window)
   - Challenge expires after 5 minutes
3. **Man-in-the-Middle Protection**:
   - Encrypted response can't be decrypted without password
   - HTTPS provides additional transport security
4. **Brute Force Protection**:
   - Failed attempts logged and rate-limited
   - Account lockout after N failed attempts
5. **Forward Secrecy**: Each authentication session uses unique challenge
6. **Cryptographic Proof**: Client proves password knowledge without revealing it

#### 4.1.3 API Endpoints Specification

**Endpoint 1: Request Authentication Challenge**

```http
POST /api/v1/auth/challenge
Content-Type: application/json

Request Body:
{
  "username": "kablu@company.com"
}

Response (200 OK):
{
  "challenge_id": "uuid-12345",
  "challenge": "base64-encoded-random-nonce",
  "algorithm": "AES-256-GCM",
  "expires_at": "2026-01-12T10:15:30Z",
  "salt": "base64-encoded-salt"
}
```

**Endpoint 2: Submit Authentication Response**

```http
POST /api/v1/auth/login
Content-Type: application/json

Request Body:
{
  "challenge_id": "uuid-12345",
  "username": "kablu@company.com",
  "response": "base64-encoded-encrypted-response",
  "client_info": {
    "ip_address": "192.168.1.100",
    "user_agent": "RA-Client/1.0"
  }
}

Response (200 OK):
{
  "access_token": "jwt-token-here",
  "token_type": "Bearer",
  "expires_in": 3600,
  "refresh_token": "refresh-token-here",
  "user": {
    "user_id": "12345",
    "username": "kablu@company.com",
    "display_name": "Kablu",
    "email": "kablu@company.com",
    "department": "Engineering",
    "roles": ["END_ENTITY"]
  }
}

Response (401 Unauthorized):
{
  "error": "authentication_failed",
  "error_description": "Invalid credentials or challenge expired"
}
```

**Endpoint 3: Submit PKCS#10 CSR**

```http
POST /api/v1/certificates/requests
Authorization: Bearer <jwt-token>
Content-Type: application/json

Request Body:
{
  "csr": "-----BEGIN CERTIFICATE REQUEST-----\nMIIC...\n-----END CERTIFICATE REQUEST-----",
  "template_id": "email-cert-template",
  "justification": "Email signing and encryption",
  "requested_validity_days": 365,
  "additional_sans": [
    "kablu.backup@company.com"
  ]
}

Response (201 Created):
{
  "request_id": "req-uuid-67890",
  "status": "PENDING_APPROVAL",
  "submitted_at": "2026-01-12T10:20:00Z",
  "csr_details": {
    "subject_dn": "CN=Kablu, E=kablu@company.com, OU=Engineering, O=Company",
    "public_key_algorithm": "RSA",
    "key_size": 2048,
    "signature_algorithm": "SHA256withRSA",
    "subject_dn_verified": true
  },
  "next_steps": "CSR submitted for RA Officer approval"
}

Response (400 Bad Request):
{
  "error": "invalid_csr",
  "error_description": "CSR subject DN does not match authenticated user profile",
  "details": {
    "csr_cn": "Different User",
    "ad_cn": "Kablu",
    "validation_failed": "subject_dn_mismatch"
  }
}

Response (403 Forbidden):
{
  "error": "unauthorized_template",
  "error_description": "User not authorized for requested certificate template"
}
```

**Endpoint 4: Check Request Status**

```http
GET /api/v1/certificates/requests/{request_id}
Authorization: Bearer <jwt-token>

Response (200 OK):
{
  "request_id": "req-uuid-67890",
  "status": "APPROVED",
  "submitted_at": "2026-01-12T10:20:00Z",
  "approved_at": "2026-01-12T11:00:00Z",
  "approved_by": "RA Officer Name",
  "certificate_available": true,
  "certificate_serial": "4A:3B:2C:1D"
}
```

**Endpoint 5: Download Certificate**

```http
GET /api/v1/certificates/requests/{request_id}/certificate
Authorization: Bearer <jwt-token>

Response (200 OK):
Content-Type: application/x-pem-file

-----BEGIN CERTIFICATE-----
MIID...
-----END CERTIFICATE-----
```

**Endpoint 6: Revoke Certificate**

```http
POST /api/v1/certificates/{certificate_id}/revoke
Authorization: Bearer <jwt-token>
Content-Type: application/json

Request Body:
{
  "reason": "KEY_COMPROMISE",
  "comment": "Private key potentially exposed"
}

Response (200 OK):
{
  "certificate_id": "cert-uuid-11111",
  "serial_number": "4A:3B:2C:1D",
  "status": "REVOKED",
  "revoked_at": "2026-01-12T12:00:00Z"
}
```

#### 4.1.4 Cryptographic Authentication Implementation

**Client-Side (End Entity) Implementation:**

```java
// Step 1: Request Challenge
public class RAClient {
    private static final String RA_BASE_URL = "https://ra.company.com/api/v1";

    public AuthToken authenticate(String username, String password) throws Exception {
        // 1. Request challenge
        ChallengeResponse challenge = requestChallenge(username);

        // 2. Derive key from password using PBKDF2
        SecretKey key = deriveKeyFromPassword(password,
            Base64.getDecoder().decode(challenge.getSalt()));

        // 3. Create response payload
        String responsePayload = challenge.getChallenge() + ":" + username + ":"
            + System.currentTimeMillis();

        // 4. Encrypt response using AES-256-GCM
        String encryptedResponse = encryptResponse(responsePayload, key);

        // 5. Submit authentication response
        return submitAuthResponse(challenge.getChallengeId(), username, encryptedResponse);
    }

    private SecretKey deriveKeyFromPassword(String password, byte[] salt) throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 10000, 256);
        SecretKey tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), "AES");
    }

    private String encryptResponse(String payload, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        byte[] iv = new byte[12]; // GCM standard IV size
        SecureRandom.getInstanceStrong().nextBytes(iv);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(128, iv);

        cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);
        byte[] encrypted = cipher.doFinal(payload.getBytes(StandardCharsets.UTF_8));

        // Combine IV + encrypted data
        byte[] combined = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    // Submit CSR
    public CertificateRequest submitCSR(String csrPem, String templateId,
                                        String jwtToken) throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        JsonObject requestBody = Json.createObjectBuilder()
            .add("csr", csrPem)
            .add("template_id", templateId)
            .add("justification", "API-based certificate request")
            .build();

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(RA_BASE_URL + "/certificates/requests"))
            .header("Authorization", "Bearer " + jwtToken)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
            .build();

        HttpResponse<String> response = client.send(request,
            HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 201) {
            return parseResponse(response.body(), CertificateRequest.class);
        } else {
            throw new RAException("CSR submission failed: " + response.body());
        }
    }
}
```

**Server-Side (RA) Implementation:**

```java
@RestController
@RequestMapping("/api/v1")
public class AuthenticationController {

    @Autowired
    private ActiveDirectoryService adService;

    @Autowired
    private ChallengeStore challengeStore;

    @Autowired
    private JWTTokenService tokenService;

    // Endpoint 1: Generate Challenge
    @PostMapping("/auth/challenge")
    public ResponseEntity<ChallengeResponse> requestChallenge(
            @RequestBody ChallengeRequest request) {

        // Validate username format
        if (!isValidUsername(request.getUsername())) {
            return ResponseEntity.badRequest().build();
        }

        // Generate random challenge (nonce)
        byte[] challenge = new byte[32];
        SecureRandom.getInstanceStrong().nextBytes(challenge);

        // Generate salt for key derivation
        byte[] salt = new byte[16];
        SecureRandom.getInstanceStrong().nextBytes(salt);

        // Store challenge temporarily (5 minutes expiry)
        String challengeId = UUID.randomUUID().toString();
        challengeStore.store(challengeId, challenge,
            Duration.ofMinutes(5), request.getUsername());

        ChallengeResponse response = ChallengeResponse.builder()
            .challengeId(challengeId)
            .challenge(Base64.getEncoder().encodeToString(challenge))
            .algorithm("AES-256-GCM")
            .salt(Base64.getEncoder().encodeToString(salt))
            .expiresAt(Instant.now().plus(5, ChronoUnit.MINUTES))
            .build();

        // Audit log
        auditLog("CHALLENGE_REQUESTED", request.getUsername());

        return ResponseEntity.ok(response);
    }

    // Endpoint 2: Verify Authentication Response
    @PostMapping("/auth/login")
    public ResponseEntity<AuthTokenResponse> authenticate(
            @RequestBody AuthRequest request) {

        try {
            // 1. Retrieve stored challenge
            StoredChallenge storedChallenge = challengeStore.get(request.getChallengeId());
            if (storedChallenge == null || storedChallenge.isExpired()) {
                auditLog("AUTH_FAILED", request.getUsername(), "Challenge expired");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AuthTokenResponse.error("challenge_expired"));
            }

            // 2. Authenticate against Active Directory
            LdapUser adUser = adService.authenticate(request.getUsername(),
                storedChallenge, request.getResponse());

            if (adUser == null) {
                auditLog("AUTH_FAILED", request.getUsername(), "Invalid credentials");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AuthTokenResponse.error("authentication_failed"));
            }

            // 3. Retrieve user roles from AD groups
            Set<String> roles = adService.getUserRoles(adUser);

            // 4. Generate JWT token
            String accessToken = tokenService.generateAccessToken(adUser, roles);
            String refreshToken = tokenService.generateRefreshToken(adUser);

            // 5. Create session
            sessionService.createSession(adUser.getUserId(), request.getClientInfo());

            // 6. Audit log
            auditLog("AUTH_SUCCESS", request.getUsername(),
                "IP: " + request.getClientInfo().getIpAddress());

            AuthTokenResponse response = AuthTokenResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(3600)
                .refreshToken(refreshToken)
                .user(UserInfo.from(adUser, roles))
                .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            auditLog("AUTH_ERROR", request.getUsername(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

@RestController
@RequestMapping("/api/v1/certificates")
public class CertificateRequestController {

    @Autowired
    private CSRValidationService csrValidator;

    @Autowired
    private ActiveDirectoryService adService;

    @Autowired
    private CertificateRequestService requestService;

    // Endpoint 3: Submit CSR
    @PostMapping("/requests")
    @PreAuthorize("hasAnyRole('END_ENTITY', 'RA_OPERATOR', 'RA_OFFICER', 'RA_ADMIN')")
    public ResponseEntity<CertificateRequestResponse> submitCSR(
            @RequestBody CSRSubmissionRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            // 1. Parse and validate PKCS#10 CSR
            PKCS10CertificationRequest csr = csrValidator.parsePKCS10(request.getCsr());

            // 2. Verify CSR signature (Proof of Possession)
            if (!csrValidator.verifySignature(csr)) {
                auditLog("CSR_REJECTED", userDetails.getUsername(),
                    "Invalid CSR signature");
                return ResponseEntity.badRequest()
                    .body(CertificateRequestResponse.error("invalid_csr_signature"));
            }

            // 3. Extract Subject DN from CSR
            X500Name subjectDN = csr.getSubject();
            CSRSubjectInfo csrSubject = csrValidator.extractSubjectInfo(subjectDN);

            // 4. Retrieve authenticated user's AD profile
            LdapUser adUser = adService.getUserByUsername(userDetails.getUsername());

            // 5. Validate CSR Subject DN against AD profile
            ValidationResult validation = csrValidator.validateSubjectDN(
                csrSubject, adUser);

            if (!validation.isValid()) {
                auditLog("CSR_REJECTED", userDetails.getUsername(),
                    "Subject DN mismatch: " + validation.getReason());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(CertificateRequestResponse.error("subject_dn_mismatch",
                        validation.getDetails()));
            }

            // 6. Check authorization for requested template
            CertificateTemplate template = templateService.getTemplate(
                request.getTemplateId());

            if (!authService.isAuthorizedForTemplate(adUser, template)) {
                auditLog("CSR_REJECTED", userDetails.getUsername(),
                    "Unauthorized template: " + request.getTemplateId());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(CertificateRequestResponse.error("unauthorized_template"));
            }

            // 7. Check for duplicate public key (replay attack prevention)
            String publicKeyHash = csrValidator.calculatePublicKeyHash(csr.getSubjectPublicKeyInfo());
            if (requestService.isDuplicatePublicKey(publicKeyHash)) {
                auditLog("CSR_REJECTED", userDetails.getUsername(),
                    "Duplicate public key");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(CertificateRequestResponse.error("duplicate_public_key"));
            }

            // 8. Create certificate request record
            CertificateRequest certRequest = requestService.createRequest(
                adUser.getUserId(),
                request.getCsr(),
                csrSubject,
                publicKeyHash,
                template,
                request.getJustification()
            );

            // 9. Trigger approval workflow
            workflowService.submitForApproval(certRequest);

            // 10. Audit log
            auditLog("CSR_SUBMITTED", userDetails.getUsername(),
                "Request ID: " + certRequest.getRequestId());

            CertificateRequestResponse response = CertificateRequestResponse.builder()
                .requestId(certRequest.getRequestId())
                .status(certRequest.getStatus().name())
                .submittedAt(certRequest.getCreatedAt())
                .csrDetails(CSRDetails.from(csr, validation))
                .nextSteps("CSR submitted for RA Officer approval")
                .build();

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            auditLog("CSR_ERROR", userDetails.getUsername(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
```

**Active Directory Service with Cryptographic Verification:**

```java
@Service
public class ActiveDirectoryService {

    @Autowired
    private LdapTemplate ldapTemplate;

    public LdapUser authenticate(String username, StoredChallenge challenge,
                                 String encryptedResponse) {
        try {
            // 1. Bind to AD with service account to retrieve user
            LdapUser user = getUserByUsername(username);
            if (user == null || !user.isActive()) {
                return null;
            }

            // 2. Attempt to bind with user credentials to verify password
            // Note: In production, use password verification mechanism appropriate
            // for your AD setup. This is a simplified example.

            // 3. If AD authentication succeeds, verify the challenge response
            // Derive key from user's password (retrieved from AD or verified via bind)
            SecretKey key = deriveKeyFromADPassword(user, challenge.getSalt());

            // 4. Decrypt the response
            String decryptedPayload = decryptResponse(encryptedResponse, key);

            // 5. Verify challenge matches
            String[] parts = decryptedPayload.split(":");
            if (parts.length < 3) {
                return null;
            }

            String receivedChallenge = parts[0];
            String receivedUsername = parts[1];
            long timestamp = Long.parseLong(parts[2]);

            // Verify challenge
            String expectedChallenge = Base64.getEncoder().encodeToString(
                challenge.getChallenge());

            if (!receivedChallenge.equals(expectedChallenge) ||
                !receivedUsername.equals(username)) {
                return null;
            }

            // Verify timestamp (prevent replay - within 5 minutes)
            if (Math.abs(System.currentTimeMillis() - timestamp) > 300000) {
                return null;
            }

            return user;

        } catch (Exception e) {
            log.error("Authentication failed for user: " + username, e);
            return null;
        }
    }

    public LdapUser getUserByUsername(String username) {
        try {
            return ldapTemplate.search(
                query().where("userPrincipalName").is(username),
                new LdapUserAttributeMapper()
            ).stream().findFirst().orElse(null);
        } catch (Exception e) {
            log.error("Failed to retrieve user from AD: " + username, e);
            return null;
        }
    }
}
```

#### 4.1.5 Complete API Flow Example

```bash
# Step 1: Request Authentication Challenge
curl -X POST https://ra.company.com/api/v1/auth/challenge \
  -H "Content-Type: application/json" \
  -d '{"username": "kablu@company.com"}'

# Response:
# {
#   "challenge_id": "abc-123",
#   "challenge": "base64-encoded-nonce",
#   "salt": "base64-salt",
#   "expires_at": "2026-01-12T10:20:00Z"
# }

# Step 2: Client encrypts response and submits
curl -X POST https://ra.company.com/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "challenge_id": "abc-123",
    "username": "kablu@company.com",
    "response": "encrypted-challenge-response",
    "client_info": {
      "ip_address": "192.168.1.100"
    }
  }'

# Response:
# {
#   "access_token": "eyJhbGc...",
#   "token_type": "Bearer",
#   "expires_in": 3600
# }

# Step 3: Submit CSR with JWT token
curl -X POST https://ra.company.com/api/v1/certificates/requests \
  -H "Authorization: Bearer eyJhbGc..." \
  -H "Content-Type: application/json" \
  -d '{
    "csr": "-----BEGIN CERTIFICATE REQUEST-----\nMIIC...\n-----END CERTIFICATE REQUEST-----",
    "template_id": "email-cert",
    "justification": "Email certificate for secure communication"
  }'

# Response:
# {
#   "request_id": "req-456",
#   "status": "PENDING_APPROVAL",
#   "csr_details": {
#     "subject_dn": "CN=Kablu, E=kablu@company.com",
#     "subject_dn_verified": true
#   }
# }

# Step 4: Check status
curl -X GET https://ra.company.com/api/v1/certificates/requests/req-456 \
  -H "Authorization: Bearer eyJhbGc..."

# Step 5: Download certificate (after approval)
curl -X GET https://ra.company.com/api/v1/certificates/requests/req-456/certificate \
  -H "Authorization: Bearer eyJhbGc..." \
  -o certificate.pem
```

#### 4.1.6 Security Considerations

**Transport Security:**
- All API endpoints MUST use HTTPS/TLS 1.3
- Certificate pinning recommended for client applications
- HSTS headers enforced

**Token Security:**
- JWT tokens signed with RS256 (asymmetric)
- Short-lived access tokens (1 hour)
- Refresh tokens with rotation
- Token revocation support via blacklist

**Rate Limiting:**
```
- Challenge requests: 10 per minute per IP
- Login attempts: 5 per 15 minutes per username
- CSR submissions: 10 per hour per user
- API calls: 100 per minute per token
```

**Input Validation:**
- CSR size limit: 10KB
- Request body size limit: 50KB
- Strict JSON schema validation
- SQL injection prevention (parameterized queries)
- XSS prevention (output encoding)

**Audit Logging:**
Every API call logged with:
- Timestamp
- Username
- IP address
- User agent
- Action performed
- Result (success/failure)
- Request/response hashes

## 4.2 Technical Architecture

### 4.2.1 Technology Stack Recommendations
- **Backend**: Java (Spring Boot), .NET Core, or Node.js
- **Frontend**: React, Angular, or Vue.js
- **Database**: PostgreSQL, MySQL, or MS SQL Server
- **AD Integration**: LDAP libraries (Spring LDAP, ldapjs, etc.)
- **CSR Processing**: Bouncy Castle (Java), OpenSSL libraries, or PKI.js (JavaScript)
- **Certificate Management**: Java Security API, .NET X509Certificate2, Node.js crypto
- **Security**: OAuth2/OpenID Connect optional for future SSO expansion

### 4.2.2 REST API Implementation Steps

**Phase 1: Setup REST API Infrastructure (Week 1)**

1. **Project Setup**
   ```bash
   # Spring Boot Example
   spring init --dependencies=web,security,data-jpa,ldap ra-web-api
   cd ra-web-api

   # Add dependencies in pom.xml
   - spring-boot-starter-web
   - spring-boot-starter-security
   - spring-security-ldap
   - spring-boot-starter-data-jpa
   - bouncycastle (for CSR processing)
   - jjwt (for JWT tokens)
   - postgresql-driver
   ```

2. **Configure Active Directory Connection**
   ```yaml
   # application.yml
   spring:
     ldap:
       urls: ldap://ad.company.com:389
       base: dc=company,dc=com
       username: CN=ra-service,OU=Services,DC=company,DC=com
       password: ${LDAP_PASSWORD}
   ```

3. **Create Database Schema**
   - Run DDL scripts for all tables (users, certificate_requests, etc.)
   - Set up connection pool
   - Configure Flyway/Liquibase for migrations

**Phase 2: Implement Authentication Endpoints (Week 1-2)**

1. **Challenge-Response Implementation**
   ```java
   // Step 1: Create Challenge Store (Redis/In-Memory)
   @Component
   public class ChallengeStore {
       private final Map<String, StoredChallenge> challenges = new ConcurrentHashMap<>();

       public void store(String id, byte[] challenge, Duration ttl, String username) {
           challenges.put(id, new StoredChallenge(challenge, username,
               Instant.now().plus(ttl)));
       }

       public StoredChallenge get(String id) {
           StoredChallenge challenge = challenges.get(id);
           if (challenge != null && challenge.isExpired()) {
               challenges.remove(id);
               return null;
           }
           return challenge;
       }
   }

   // Step 2: Implement Authentication Controller (as shown above)
   // Step 3: Implement JWT Token Service
   @Service
   public class JWTTokenService {
       private final String SECRET_KEY = // Load from config

       public String generateAccessToken(LdapUser user, Set<String> roles) {
           return Jwts.builder()
               .setSubject(user.getUsername())
               .claim("roles", roles)
               .claim("userId", user.getUserId())
               .setIssuedAt(new Date())
               .setExpiration(new Date(System.currentTimeMillis() + 3600000))
               .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
               .compact();
       }
   }
   ```

2. **AD Integration Service**
   - Implement LDAP connection
   - User attribute retrieval
   - Password verification (via LDAP bind)
   - Group membership resolution

**Phase 3: Implement CSR Processing (Week 2-3)**

1. **CSR Validation Service**
   ```java
   @Service
   public class CSRValidationService {

       public PKCS10CertificationRequest parsePKCS10(String csrPem) throws IOException {
           PEMParser parser = new PEMParser(new StringReader(csrPem));
           Object parsedObj = parser.parseObject();
           return (PKCS10CertificationRequest) parsedObj;
       }

       public boolean verifySignature(PKCS10CertificationRequest csr) {
           try {
               ContentVerifierProvider verifier =
                   new JcaContentVerifierProviderBuilder()
                       .build(csr.getSubjectPublicKeyInfo());
               return csr.isSignatureValid(verifier);
           } catch (Exception e) {
               return false;
           }
       }

       public ValidationResult validateSubjectDN(CSRSubjectInfo csrSubject,
                                                  LdapUser adUser) {
           if (!csrSubject.getCN().equals(adUser.getDisplayName())) {
               return ValidationResult.failed("CN mismatch");
           }
           if (!csrSubject.getEmail().equals(adUser.getEmail())) {
               return ValidationResult.failed("Email mismatch");
           }
           return ValidationResult.success();
       }

       public String calculatePublicKeyHash(SubjectPublicKeyInfo publicKeyInfo) {
           byte[] encoded = publicKeyInfo.getEncoded();
           MessageDigest digest = MessageDigest.getInstance("SHA-256");
           byte[] hash = digest.digest(encoded);
           return Base64.getEncoder().encodeToString(hash);
       }
   }
   ```

2. **Certificate Request Controller**
   - Implement POST /certificates/requests (as shown above)
   - Implement GET /certificates/requests/{id}
   - Implement GET /certificates/requests/{id}/certificate
   - Implement POST /certificates/{id}/revoke

**Phase 4: Implement Authorization & Security (Week 3-4)**

1. **Role-Based Access Control**
   ```java
   @Configuration
   @EnableWebSecurity
   @EnableGlobalMethodSecurity(prePostEnabled = true)
   public class SecurityConfig extends WebSecurityConfigurerAdapter {

       @Autowired
       private JWTAuthenticationFilter jwtFilter;

       @Override
       protected void configure(HttpSecurity http) throws Exception {
           http.csrf().disable()
               .authorizeRequests()
               .antMatchers("/api/v1/auth/**").permitAll()
               .antMatchers("/api/v1/certificates/**").authenticated()
               .and()
               .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
       }
   }

   @Component
   public class JWTAuthenticationFilter extends OncePerRequestFilter {
       @Override
       protected void doFilterInternal(HttpServletRequest request,
                                      HttpServletResponse response,
                                      FilterChain filterChain) {
           String token = extractToken(request);
           if (token != null && jwtTokenService.validateToken(token)) {
               UserDetails userDetails = loadUserFromToken(token);
               UsernamePasswordAuthenticationToken auth =
                   new UsernamePasswordAuthenticationToken(userDetails,
                       null, userDetails.getAuthorities());
               SecurityContextHolder.getContext().setAuthentication(auth);
           }
           filterChain.doFilter(request, response);
       }
   }
   ```

2. **Rate Limiting**
   ```java
   @Component
   public class RateLimitingFilter extends OncePerRequestFilter {
       private final RateLimiter rateLimiter = RateLimiter.create(100.0); // 100 req/min

       @Override
       protected void doFilterInternal(HttpServletRequest request,
                                      HttpServletResponse response,
                                      FilterChain filterChain) {
           if (!rateLimiter.tryAcquire()) {
               response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
               return;
           }
           filterChain.doFilter(request, response);
       }
   }
   ```

**Phase 5: CA Integration (Week 4-5)**

1. **CA Service Interface**
   ```java
   public interface CertificateAuthorityService {
       Certificate issueCertificate(PKCS10CertificationRequest csr,
                                   CertificateTemplate template);
       void revokeCertificate(String serialNumber, RevocationReason reason);
       CRLInfo getCRL();
   }

   // Implementations for specific CAs:
   @Service
   public class EJBCAService implements CertificateAuthorityService {
       // EJBCA REST API integration
   }

   @Service
   public class MicrosoftCAService implements CertificateAuthorityService {
       // Microsoft CA integration via certreq/certutil
   }
   ```

2. **Certificate Issuance Workflow**
   - RA Officer approval triggers CA submission
   - Poll/webhook for certificate availability
   - Store issued certificate in database
   - Notify end entity

**Phase 6: Audit Logging (Week 5)**

1. **Audit Interceptor**
   ```java
   @Aspect
   @Component
   public class AuditAspect {

       @Around("@annotation(Audited)")
       public Object auditMethod(ProceedingJoinPoint joinPoint) throws Throwable {
           Authentication auth = SecurityContextHolder.getContext().getAuthentication();
           String username = auth.getName();
           String action = joinPoint.getSignature().getName();

           try {
               Object result = joinPoint.proceed();
               auditLog.log(username, action, "SUCCESS", result);
               return result;
           } catch (Exception e) {
               auditLog.log(username, action, "FAILURE", e.getMessage());
               throw e;
           }
       }
   }
   ```

**Phase 7: Testing (Week 6)**

1. **Unit Tests**
   - Test CSR parsing and validation
   - Test Subject DN matching
   - Test authentication flow
   - Test JWT token generation/validation

2. **Integration Tests**
   ```java
   @SpringBootTest
   @AutoConfigureMockMvc
   public class CSRSubmissionIntegrationTest {

       @Autowired
       private MockMvc mockMvc;

       @Test
       public void testCSRSubmissionFlow() throws Exception {
           // 1. Request challenge
           String challengeResponse = mockMvc.perform(post("/api/v1/auth/challenge")
               .contentType(MediaType.APPLICATION_JSON)
               .content("{\"username\":\"test@company.com\"}"))
               .andExpect(status().isOk())
               .andReturn().getResponse().getContentAsString();

           // 2. Authenticate (mocked)
           // 3. Submit CSR
           // 4. Verify response
       }
   }
   ```

3. **Security Testing**
   - Penetration testing
   - OWASP Top 10 verification
   - Rate limiting validation
   - Token security testing

**Phase 8: Deployment (Week 6-7)**

1. **Containerization**
   ```dockerfile
   # Dockerfile
   FROM openjdk:17-jdk-slim
   COPY target/ra-web-api.jar /app/ra-web-api.jar
   EXPOSE 8443
   ENTRYPOINT ["java", "-jar", "/app/ra-web-api.jar"]
   ```

2. **Configuration**
   ```yaml
   # docker-compose.yml
   version: '3.8'
   services:
     ra-api:
       build: .
       ports:
         - "8443:8443"
       environment:
         - SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/radb
         - LDAP_URL=ldap://ad.company.com:389
       depends_on:
         - db
     db:
       image: postgres:15
       environment:
         POSTGRES_DB: radb
         POSTGRES_PASSWORD: ${DB_PASSWORD}
   ```

3. **Monitoring & Logging**
   - Configure Prometheus metrics
   - Set up ELK stack for log aggregation
   - Configure health check endpoints
   - Set up alerts for security events

### 4.2.3 Architecture Components
```
┌─────────────────┐
│   End Users     │
│  (Web Browser)  │
└────────┬────────┘
         │ HTTPS
         ▼
┌─────────────────┐
│   Web Server    │
│  (Frontend UI)  │
└────────┬────────┘
         │
         ▼
┌─────────────────┐      ┌──────────────┐
│  Application    │◄────►│   Active     │
│    Server       │      │  Directory   │
│  (RA Logic)     │      └──────────────┘
└────────┬────────┘
         │
         ├──────────────┐
         ▼              ▼
┌─────────────┐  ┌─────────────┐
│  Database   │  │ Certificate │
│             │  │  Authority  │
└─────────────┘  └─────────────┘
```

### 4.3 Database Schema (Key Tables)

**Core User & Role Tables:**
- **users**: User profiles synced from AD (user_id, username, email, display_name, department, ad_dn, is_active, last_sync)
- **roles**: Role definitions and permissions
- **user_roles**: User-to-role mapping
- **user_sessions**: Active user sessions for authentication tracking

**Certificate Template Tables:**
- **certificate_templates**: Certificate template definitions with auto-enrollment settings
- **template_group_mappings**: Mapping of AD groups to certificate templates
- **template_authentication_policies**: Authentication requirements per template (OTP required, face-to-face required, etc.)

**Certificate Request Tables:**
- **certificate_requests**: Certificate request records with authentication details
  - request_id (PK)
  - user_id (FK to users)
  - submitted_by_user_id (FK - for operator submissions on behalf)
  - csr_pem (PKCS#10 CSR content)
  - csr_subject_dn (extracted from CSR)
  - csr_public_key_hash (SHA-256 hash for duplicate detection)
  - authentication_method (AD_PASSWORD, OTP, SMART_CARD, FACE_TO_FACE)
  - subject_dn_verified (boolean - matches AD profile)
  - template_id (FK to certificate_templates)
  - status (DRAFT, PENDING_VERIFICATION, PENDING_APPROVAL, APPROVED, REJECTED, ISSUED)
  - ip_address (submission IP for audit)
  - geo_location (country/city for anomaly detection)
  - created_at, updated_at

**Authentication & Verification Tables:**
- **email_verifications**: Email verification tokens for CSR submissions
  - verification_id (PK)
  - request_id (FK to certificate_requests)
  - email_address (from AD)
  - token (unique verification token)
  - expires_at
  - verified_at
  - ip_address_verified_from

- **otp_verifications**: One-Time Password verifications
  - otp_id (PK)
  - request_id (FK to certificate_requests)
  - mobile_number (from AD)
  - otp_code (6-digit code)
  - sent_at
  - expires_at (5 minutes from sent_at)
  - verified_at
  - attempts (max 3 attempts)

- **identity_verifications**: Face-to-face identity verification records
  - verification_id (PK)
  - request_id (FK to certificate_requests)
  - verified_by_officer_id (FK to users - RA Officer who verified)
  - verification_date
  - id_document_type (PASSPORT, DRIVERS_LICENSE, EMPLOYEE_ID)
  - id_document_number
  - photo_verified (boolean)
  - verification_notes
  - verification_location

**Certificate Tables:**
- **certificates**: Issued certificate metadata
  - certificate_id (PK)
  - request_id (FK to certificate_requests)
  - user_id (FK to users)
  - serial_number (from CA)
  - subject_dn
  - issuer_dn
  - public_key_hash (SHA-256)
  - not_before, not_after (validity period)
  - status (ACTIVE, REVOKED, EXPIRED, SUSPENDED)
  - revocation_date, revocation_reason
  - issued_at

**Auto-Enrollment Tables:**
- **auto_enrollment_jobs**: Scheduled and completed auto-enrollment job records
- **enrollment_policies**: Auto-enrollment policy configurations

**Audit & Security Tables:**
- **audit_logs**: Comprehensive audit trail
  - log_id (PK)
  - timestamp
  - user_id (FK to users - who performed action)
  - action (LOGIN, CSR_UPLOAD, CSR_APPROVED, CERT_ISSUED, CERT_REVOKED, etc.)
  - resource_type (USER, CSR, CERTIFICATE, etc.)
  - resource_id
  - ip_address
  - user_agent
  - authentication_method
  - result (SUCCESS, FAILURE)
  - failure_reason
  - additional_data (JSON for extra context)

- **public_key_blacklist**: Blacklisted public keys (compromised keys)
  - blacklist_id (PK)
  - public_key_hash (SHA-256)
  - blacklisted_at
  - reason (KEY_COMPROMISE, ALGORITHM_WEAKNESS, etc.)
  - blacklisted_by_user_id (FK to users)

- **failed_authentication_attempts**: Track failed auth attempts for security monitoring
  - attempt_id (PK)
  - username
  - ip_address
  - attempted_at
  - failure_reason (INVALID_PASSWORD, ACCOUNT_LOCKED, etc.)

**Configuration Tables:**
- **configurations**: System configuration parameters
- **authentication_policies**: Global authentication policy settings
  - policy_id (PK)
  - max_certificate_quota_per_user
  - csr_expiry_days (default 30)
  - session_timeout_minutes
  - max_failed_login_attempts
  - otp_validity_minutes
  - email_verification_validity_hours
  - require_geo_location_check (boolean)
  - allowed_key_algorithms (JSON array: ["RSA-2048", "RSA-4096", "ECDSA-P256"])
  - blocked_countries (JSON array for geo-blocking)

---

## 5. MVP Development Phases

### Phase 1: Foundation (Weeks 1-2)
- Set up development environment
- Implement AD authentication and user synchronization
- Create role-based access control framework
- Design and implement database schema

### Phase 2: Core RA Operations (Weeks 3-4)
- Certificate request submission workflow
- Request approval/rejection interface
- Basic CA integration for certificate issuance
- Certificate search and view functionality

### Phase 3: Certificate Lifecycle (Weeks 5-6)
- Certificate renewal workflow
- Certificate revocation functionality
- Status tracking and notifications
- User dashboard implementation

### Phase 4: Security & Audit (Week 7)
- Comprehensive audit logging
- Security hardening (CSRF, XSS protection, input validation)
- Session management and timeout handling
- Basic reporting functionality

### Phase 5: Testing & Deployment (Week 8)
- Integration testing
- Security testing and vulnerability assessment
- User acceptance testing
- Production deployment and documentation

---

## 6. Success Criteria

### 6.1 Functional Requirements Met

**REST API Requirements:**
- [ ] REST API endpoints implemented for authentication and certificate operations
- [ ] Challenge-response authentication mechanism working with cryptographic verification
- [ ] JWT token generation and validation for session management
- [ ] POST /api/v1/auth/challenge endpoint functional
- [ ] POST /api/v1/auth/login endpoint with AD verification functional
- [ ] POST /api/v1/certificates/requests endpoint accepts PKCS#10 CSR
- [ ] GET /api/v1/certificates/requests/{id} returns request status
- [ ] GET /api/v1/certificates/requests/{id}/certificate downloads issued certificate
- [ ] POST /api/v1/certificates/{id}/revoke revokes certificates
- [ ] API rate limiting implemented and enforced
- [ ] API documentation (OpenAPI/Swagger) available

**Authentication & Authorization:**
- [ ] Users can authenticate via Active Directory through REST API
- [ ] Multi-layer authentication implemented for CSR submissions (AD auth + DN validation)
- [ ] Challenge-response cryptographic verification prevents password transmission
- [ ] CSR Subject DN validation against authenticated user's AD profile
- [ ] Optional additional verification methods (Email, OTP, Face-to-face, Smart Card) implemented
- [ ] Authorization checks verify user eligibility for requested certificate templates
- [ ] Role-based access control enforced across all operations
- [ ] JWT tokens properly signed and validated

**Certificate Operations:**
- [ ] Auto-enrollment enabled for eligible users based on AD group membership
- [ ] Certificate templates configured with auto-enrollment policies
- [ ] Automatic certificate issuance and renewal without user intervention
- [ ] PKCS#10 CSR upload and validation working correctly via API
- [ ] CSR signature verification and format validation implemented
- [ ] Proof of possession verified (CSR signature check)
- [ ] Public key blacklist checking implemented to prevent compromised key reuse
- [ ] Duplicate CSR/public key detection prevents replay attacks
- [ ] Support for multiple enrollment methods (CSR upload, server-side, client-side)
- [ ] Certificate requests can be submitted, approved, and issued (manual flow)
- [ ] Certificates can be revoked with proper authorization via API
- [ ] CA integration functional (EJBCA/Microsoft CA)

**Audit & Logging:**
- [ ] Audit logs capture all certificate operations including authentication events
- [ ] All API calls logged with timestamp, user, IP, action, and result
- [ ] Failed authentication attempts tracked and monitored
- [ ] Role-specific dashboards provide appropriate functionality

### 6.2 Non-Functional Requirements Met
- [ ] System supports concurrent users (target: 100+)
- [ ] Response time < 2 seconds for common operations
- [ ] API response time < 500ms for authentication, < 1s for CSR submission
- [ ] 99.5% uptime during business hours
- [ ] All communications encrypted (HTTPS/TLS 1.3)
- [ ] API rate limiting prevents abuse (configurable per endpoint)
- [ ] Audit logs retained per compliance requirements
- [ ] User interface is intuitive and requires minimal training
- [ ] REST API follows RESTful principles and best practices
- [ ] API versioning implemented (/api/v1, /api/v2)

### 6.3 Security Requirements Met
- [ ] OWASP Top 10 vulnerabilities addressed
- [ ] Sensitive data encrypted at rest and in transit
- [ ] Challenge-response authentication prevents password exposure
- [ ] Cryptographic verification (AES-256-GCM) for authentication
- [ ] JWT tokens properly signed with RS256 asymmetric algorithm
- [ ] Token expiration and refresh mechanisms implemented
- [ ] Multi-layer authentication prevents impersonation attacks
- [ ] Subject DN validation prevents users from requesting certificates for others
- [ ] Proof of possession verified via CSR signature
- [ ] Public key blacklist prevents compromised key reuse
- [ ] Replay attack prevention via duplicate detection and timestamp validation
- [ ] Challenge nonce expiration (5 minutes)
- [ ] Failed authentication attempts logged and monitored
- [ ] IP address and geo-location tracking for anomaly detection
- [ ] Session management with appropriate timeouts
- [ ] API input validation (CSR size limits, JSON schema validation)
- [ ] SQL injection prevention via parameterized queries
- [ ] XSS prevention via output encoding
- [ ] CSRF protection (not needed for stateless JWT API)
- [ ] Authentication and authorization properly implemented
- [ ] Security audit completed with no critical findings
- [ ] Incident response procedures documented
- [ ] Penetration testing completed

---

## 7. Future Enhancements (Post-MVP)
- Mobile application for certificate management
- Advanced workflow customization and approval chains
- Integration with Hardware Security Modules (HSM)
- Support for multiple CAs and certificate profiles
- Self-service certificate template selection
- Advanced analytics and dashboard visualizations
- REST API for third-party integrations
- Automated certificate deployment to servers/devices
- Certificate inventory and discovery tools
- Integration with SIEM systems for security monitoring

---

## 8. Documentation Deliverables
- System Architecture Document
- API Documentation
- User Guides (role-specific)
- Administrator Manual
- Security Configuration Guide
- Deployment Guide
- Audit and Compliance Guide

---

**Document Version**: 3.0
**Last Updated**: 2026-01-12
**Status**: Draft - MVP Requirements with REST API & Cryptographic Authentication
