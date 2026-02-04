# Registration Authority (RA) - Complete User Hierarchy & Flow Diagram

**Date**: 2026-01-16
**Version**: 2.0 - Complete MVP Requirements
**Purpose**: End-to-end user hierarchy, authentication, and certificate lifecycle

---

## 🏗️ Complete RA System Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    REGISTRATION AUTHORITY (RA) SYSTEM                   │
│                         PKI Certificate Lifecycle                       │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
        ┌───────────────────────────┼───────────────────────────┐
        │                           │                           │
        ▼                           ▼                           ▼
┌───────────────┐         ┌──────────────────┐       ┌──────────────────┐
│  Active       │         │   RA User        │       │  Certificate     │
│  Directory    │◄────────┤   Database       │───────┤  Authority (CA)  │
│  (LDAP/AD)    │         │   (PostgreSQL)   │       │  (EJBCA/MS CA)   │
└───────────────┘         └──────────────────┘       └──────────────────┘
        │                           │                           │
        │ Authentication            │ User Management           │ Certificate
        │ & Attributes              │ & Role Mapping            │ Operations
        │                           │                           │
        └───────────────────────────┼───────────────────────────┘
                                    │
                                    ▼
                    ┌───────────────────────────┐
                    │   5-Level User Hierarchy  │
                    └───────────────────────────┘
```

---

## 👥 Complete User Hierarchy with Responsibilities

### 📊 Hierarchy Structure (Top to Bottom)

```
                        ┌─────────────────────────┐
                        │   SUPER ADMINISTRATOR   │
                        │    (Bootstrap Only)     │
                        │                         │
                        │  • Initial system setup │
                        │  • Create RA Admins     │
                        │  • One-time use only    │
                        └────────────┬────────────┘
                                     │
                                     │ Creates
                                     ▼
        ┌────────────────────────────────────────────────────────┐
        │                  LEVEL 1: RA ADMINISTRATOR             │
        │                   (Full System Control)                │
        ├────────────────────────────────────────────────────────┤
        │                                                        │
        │  🔧 SYSTEM CONFIGURATION                              │
        │  ├─ CA Integration (SCEP, CMP, REST)                  │
        │  ├─ AD/LDAP Configuration                             │
        │  ├─ Certificate Templates                             │
        │  ├─ Auto-Enrollment Policies                          │
        │  ├─ Email/SMS Templates                               │
        │  └─ Security Policies (TLS, CORS, Rate Limiting)      │
        │                                                        │
        │  👤 USER MANAGEMENT                                   │
        │  ├─ Create/Edit/Delete Users                          │
        │  ├─ Assign/Revoke Roles                               │
        │  ├─ Map AD Groups to Roles                            │
        │  ├─ Set Certificate Quotas                            │
        │  └─ Manage Maker-Checker Workflows                    │
        │                                                        │
        │  📊 MONITORING & REPORTS                              │
        │  ├─ System Health Dashboard                           │
        │  ├─ CA Connection Status                              │
        │  ├─ Database Status                                   │
        │  ├─ Enrollment Statistics                             │
        │  └─ Complete Audit Logs                               │
        │                                                        │
        │  ⚡ AUTO-ENROLLMENT                                    │
        │  ├─ Enable/Disable Globally                           │
        │  ├─ Configure Policies & Triggers                     │
        │  ├─ Set Auto-Approval Rules                           │
        │  └─ Monitor Enrollment Jobs                           │
        │                                                        │
        └────────────────┬───────────────────────────────────────┘
                         │
                         │ Manages
                         ▼
        ┌────────────────────────────────────────────────────────┐
        │                  LEVEL 2: RA OFFICER                   │
        │               (Approval & Verification)                │
        ├────────────────────────────────────────────────────────┤
        │                                                        │
        │  ✅ CERTIFICATE APPROVAL (Maker-Checker)              │
        │  ├─ Review Pending Requests (Checker)                 │
        │  ├─ Approve Valid Requests                            │
        │  ├─ Reject Invalid Requests                           │
        │  ├─ Request Additional Information                    │
        │  └─ View Request History & Details                    │
        │                                                        │
        │  🔴 CERTIFICATE REVOCATION                            │
        │  ├─ Revoke Compromised Certificates                   │
        │  ├─ Select Revocation Reason (RFC 5280)               │
        │  ├─ Add Revocation Comments                           │
        │  └─ Submit to CA for CRL Update                       │
        │                                                        │
        │  🔍 IDENTITY VERIFICATION                             │
        │  ├─ In-Person Verification (Photo ID)                 │
        │  ├─ Record ID Document Details                        │
        │  ├─ Confirm Face-to-Face Match                        │
        │  └─ High-Value Certificate Approval                   │
        │                                                        │
        │  📊 REPORTING & AUDIT                                 │
        │  ├─ View All Certificate Operations                   │
        │  ├─ Generate Issuance Reports                         │
        │  ├─ Generate Revocation Reports                       │
        │  ├─ Access Audit Logs                                 │
        │  └─ Export Compliance Data                            │
        │                                                        │
        │  ⚠️ CANNOT DO                                         │
        │  ✗ System Configuration                               │
        │  ✗ User/Role Management                               │
        │  ✗ Certificate Template Creation                      │
        │                                                        │
        └────────────────┬───────────────────────────────────────┘
                         │
                         │ Approves work from
                         ▼
        ┌────────────────────────────────────────────────────────┐
        │                 LEVEL 3: RA OPERATOR                   │
        │             (Submission & Assistance)                  │
        ├────────────────────────────────────────────────────────┤
        │                                                        │
        │  📝 CERTIFICATE REQUEST SUBMISSION (Maker)            │
        │  ├─ Submit CSR on Behalf of End Entities              │
        │  ├─ Upload PKCS#10 CSR Files                          │
        │  ├─ Fill Certificate Request Forms                    │
        │  ├─ Select Certificate Templates                      │
        │  ├─ Provide Business Justification                    │
        │  └─ Submit for Approval (Maker-Checker)               │
        │                                                        │
        │  📎 DOCUMENT MANAGEMENT                               │
        │  ├─ Upload Supporting Documents                       │
        │  ├─ Attach Identity Proofs                            │
        │  ├─ Upload Authorization Letters                      │
        │  └─ Manage Document Attachments                       │
        │                                                        │
        │  👁️ VIEW & TRACK                                     │
        │  ├─ View Own Submitted Requests                       │
        │  ├─ Track Request Status                              │
        │  ├─ View Approval/Rejection Reasons                   │
        │  └─ View Request History                              │
        │                                                        │
        │  ✏️ UPDATE REQUESTS (Pre-Approval Only)              │
        │  ├─ Update Pending Requests                           │
        │  ├─ Add Missing Information                           │
        │  ├─ Correct Errors                                    │
        │  └─ Cancel Draft Requests                             │
        │                                                        │
        │  📥 CERTIFICATE DELIVERY                              │
        │  ├─ Download Issued Certificates                      │
        │  ├─ Download Certificate Chains                       │
        │  ├─ Export in Multiple Formats (PEM, DER, P12)        │
        │  └─ Deliver to End Entities                           │
        │                                                        │
        │  ⚠️ CANNOT DO                                         │
        │  ✗ Approve/Reject Requests (Needs Officer)            │
        │  ✗ Revoke Certificates                                │
        │  ✗ View All Requests (Only Own)                       │
        │  ✗ Access Audit Logs                                  │
        │                                                        │
        └────────────────┬───────────────────────────────────────┘
                         │
                         │ Parallel to (Independent)
                         ▼
        ┌────────────────────────────────────────────────────────┐
        │                   LEVEL 4: AUDITOR                     │
        │                (Read-Only Compliance)                  │
        ├────────────────────────────────────────────────────────┤
        │                                                        │
        │  👁️ VIEW ALL OPERATIONS                              │
        │  ├─ View All Certificate Requests                     │
        │  ├─ View All Issued Certificates                      │
        │  ├─ View Revocation Records                           │
        │  ├─ View Approval/Rejection History                   │
        │  └─ View All User Activities                          │
        │                                                        │
        │  📜 AUDIT LOG ACCESS                                  │
        │  ├─ Access Complete Audit Trail                       │
        │  ├─ Search by User/Date/Action                        │
        │  ├─ View Authentication Logs                          │
        │  ├─ View Failed Access Attempts                       │
        │  ├─ Track Certificate Lifecycle                       │
        │  └─ Export Audit Data (Tamper-Proof)                  │
        │                                                        │
        │  📊 COMPLIANCE REPORTING                              │
        │  ├─ Generate Compliance Reports                       │
        │  ├─ Export Data (CSV, JSON, PDF)                      │
        │  ├─ Create Custom Reports                             │
        │  ├─ Schedule Automated Reports                        │
        │  └─ Analyze Trends & Patterns                         │
        │                                                        │
        │  🔒 SECURITY MONITORING                               │
        │  ├─ Review Security Events                            │
        │  ├─ Track Failed Authentication                       │
        │  ├─ Monitor Policy Violations                         │
        │  └─ Identify Suspicious Activities                    │
        │                                                        │
        │  ⚠️ CANNOT DO (Strictly Read-Only)                   │
        │  ✗ Submit Certificate Requests                        │
        │  ✗ Approve/Reject Requests                            │
        │  ✗ Revoke Certificates                                │
        │  ✗ Modify Any Records                                 │
        │  ✗ Delete Audit Logs                                  │
        │  ✗ Change System Configuration                        │
        │                                                        │
        └────────────────┬───────────────────────────────────────┘
                         │
                         │ Monitors activities of
                         ▼
        ┌────────────────────────────────────────────────────────┐
        │                  LEVEL 5: END ENTITY                   │
        │                  (Self-Service Users)                  │
        ├────────────────────────────────────────────────────────┤
        │                                                        │
        │  📝 CERTIFICATE REQUEST (Self-Service)                │
        │  ├─ Submit Own Certificate Requests                   │
        │  ├─ Upload Own PKCS#10 CSR                            │
        │  ├─ Generate Key Pair (Client/Server)                 │
        │  ├─ Select Available Templates                        │
        │  ├─ Provide Business Justification                    │
        │  └─ Track Request Status                              │
        │                                                        │
        │  🔐 IDENTITY VERIFICATION (Self-Service)              │
        │  ├─ Email Verification (24-hour link)                 │
        │  ├─ Phone/SMS Verification (6-digit OTP, 5 min)       │
        │  ├─ Multi-Factor Authentication (MFA)                 │
        │  └─ Subject DN Validation (Auto-check vs AD)          │
        │                                                        │
        │  👁️ VIEW & MANAGE OWN CERTIFICATES                   │
        │  ├─ View Own Certificate Requests                     │
        │  ├─ View Own Issued Certificates                      │
        │  ├─ Check Certificate Validity                        │
        │  ├─ View Expiration Dates                             │
        │  └─ View Certificate Details                          │
        │                                                        │
        │  📥 DOWNLOAD OWN CERTIFICATES                         │
        │  ├─ Download Issued Certificates                      │
        │  ├─ Download Private Keys (if server-generated)       │
        │  ├─ Download PKCS#12 Bundles                          │
        │  ├─ Download Certificate Chains                       │
        │  └─ Export in Multiple Formats                        │
        │                                                        │
        │  🔄 CERTIFICATE RENEWAL                               │
        │  ├─ Renew Expiring Certificates (30-day alert)        │
        │  ├─ Generate New Key Pair for Renewal                 │
        │  ├─ Submit Renewal Requests                           │
        │  └─ Track Renewal Status                              │
        │                                                        │
        │  🔔 NOTIFICATIONS & ALERTS                            │
        │  ├─ Certificate Issuance Notices                      │
        │  ├─ Expiration Warnings (30/15/7 days)                │
        │  ├─ Renewal Reminders                                 │
        │  ├─ Request Status Updates                            │
        │  └─ View Notification History                         │
        │                                                        │
        │  ⚡ AUTO-ENROLLMENT (Automatic)                       │
        │  ├─ Automatic Certificate Issuance (if eligible)      │
        │  ├─ Automatic Renewal (if configured)                 │
        │  ├─ Receive Auto-Enrolled Certificates                │
        │  ├─ Download Auto-Issued Certificates                 │
        │  └─ No Manual Action Required                         │
        │                                                        │
        │  📊 DASHBOARD & STATISTICS                            │
        │  ├─ Active Certificates Count                         │
        │  ├─ Pending Requests Count                            │
        │  ├─ Expiring Certificates (within 30 days)            │
        │  ├─ Recent Requests (last 5)                          │
        │  └─ Recent Certificates (last 5)                      │
        │                                                        │
        │  ⚠️ CANNOT DO                                         │
        │  ✗ Submit Requests for Others                         │
        │  ✗ View Others' Certificates                          │
        │  ✗ Approve/Reject Requests                            │
        │  ✗ Revoke Certificates                                │
        │  ✗ Access Audit Logs                                  │
        │  ✗ View System Configuration                          │
        │                                                        │
        └────────────────────────────────────────────────────────┘
```

---

## 🔄 Complete Authentication Flow

### End Entity Authentication (Challenge-Response with AD Validation)

```
┌──────────────────────────────────────────────────────────────────┐
│                 STEP 1: REQUEST AUTHENTICATION                   │
└──────────────────────────────────────────────────────────────────┘
                                │
                                ▼
        ┌────────────────────────────────────────┐
        │  End Entity (Browser)                  │
        │                                        │
        │  User enters username                  │
        └────────────┬───────────────────────────┘
                     │
                     │ POST /api/v1/auth/challenge
                     │ { "username": "john@corp.local" }
                     ▼
        ┌────────────────────────────────────────┐
        │  RA Web Application                    │
        │  Authentication Controller             │
        └────────────┬───────────────────────────┘
                     │
                     │ Generate Challenge
                     ▼
        ┌────────────────────────────────────────┐
        │  Challenge Generation:                 │
        │  - Random 32-byte nonce                │
        │  - Random 16-byte salt (for PBKDF2)    │
        │  - Challenge ID (UUID)                 │
        │  - Expiration (5 minutes)              │
        └────────────┬───────────────────────────┘
                     │
                     │ Store in Redis/Memory
                     │ { challenge_id → nonce, salt, username, timestamp }
                     ▼
        ┌────────────────────────────────────────┐
        │  Response to Client:                   │
        │  {                                     │
        │    "challenge_id": "uuid-123",         │
        │    "challenge": "base64(nonce)",       │
        │    "salt": "base64(salt)",             │
        │    "algorithm": "AES-256-GCM",         │
        │    "expires_at": "ISO8601"             │
        │  }                                     │
        └────────────┬───────────────────────────┘
                     │
                     ▼

┌──────────────────────────────────────────────────────────────────┐
│              STEP 2: CLIENT-SIDE PROCESSING                      │
└──────────────────────────────────────────────────────────────────┘
                     │
                     ▼
        ┌────────────────────────────────────────┐
        │  End Entity (Browser)                  │
        │                                        │
        │  Receives: challenge, salt             │
        │  User enters: password                 │
        └────────────┬───────────────────────────┘
                     │
                     │ Derive Key from Password
                     ▼
        ┌────────────────────────────────────────┐
        │  PBKDF2 Key Derivation:               │
        │                                        │
        │  key = PBKDF2(                        │
        │    password,      // User's password  │
        │    salt,          // From server      │
        │    iterations=10000,                   │
        │    keyLength=256 bits                  │
        │  )                                    │
        └────────────┬───────────────────────────┘
                     │
                     │ Create Response Payload
                     ▼
        ┌────────────────────────────────────────┐
        │  Response Payload:                     │
        │                                        │
        │  payload = challenge + ":" +           │
        │            username + ":" +            │
        │            timestamp                   │
        │                                        │
        │  Example:                              │
        │  "abc123:john@corp.local:1737456789"  │
        └────────────┬───────────────────────────┘
                     │
                     │ Encrypt Payload
                     ▼
        ┌────────────────────────────────────────┐
        │  AES-256-GCM Encryption:              │
        │                                        │
        │  - Generate random 12-byte IV          │
        │  - encrypted = AES-GCM.encrypt(        │
        │      payload, key, IV                  │
        │    )                                   │
        │  - result = IV || encrypted            │
        │  - response = base64(result)           │
        └────────────┬───────────────────────────┘
                     │
                     │ POST /api/v1/auth/login
                     ▼
        ┌────────────────────────────────────────┐
        │  Request Body:                         │
        │  {                                     │
        │    "challenge_id": "uuid-123",         │
        │    "username": "john@corp.local",      │
        │    "response": "base64(IV||encrypted)",│
        │    "client_info": {                    │
        │      "ip_address": "192.168.1.100"     │
        │    }                                   │
        │  }                                     │
        └────────────┬───────────────────────────┘
                     │
                     ▼

┌──────────────────────────────────────────────────────────────────┐
│           STEP 3: SERVER-SIDE VERIFICATION                       │
└──────────────────────────────────────────────────────────────────┘
                     │
                     ▼
        ┌────────────────────────────────────────┐
        │  RA Authentication Service             │
        │                                        │
        │  1. Retrieve stored challenge          │
        │  2. Validate challenge not expired     │
        │  3. Validate username matches          │
        └────────────┬───────────────────────────┘
                     │
                     │ Authenticate Against AD
                     ▼
        ┌────────────────────────────────────────┐
        │  Active Directory Validation:          │
        │                                        │
        │  1. LDAP Bind with Service Account    │
        │     ldapTemplate.search(               │
        │       "userPrincipalName=john@..."     │
        │     )                                  │
        │                                        │
        │  2. Retrieve User Attributes:          │
        │     - displayName                      │
        │     - mail                             │
        │     - department                       │
        │     - memberOf (group memberships)     │
        │     - employeeID                       │
        │                                        │
        │  3. Verify Account Status:             │
        │     - Account active?                  │
        │     - Account not locked?              │
        │     - Password not expired?            │
        │                                        │
        │  4. LDAP Bind with User Credentials:   │
        │     - Validates password               │
        │     - If bind succeeds → authenticated │
        │     - If bind fails → invalid password │
        └────────────┬───────────────────────────┘
                     │
                     │ If AD Auth Successful
                     ▼
        ┌────────────────────────────────────────┐
        │  Verify Challenge Response:            │
        │                                        │
        │  1. Derive same key using PBKDF2       │
        │     key = PBKDF2(password, salt, ...)  │
        │                                        │
        │  2. Decrypt client response:           │
        │     - Extract IV (first 12 bytes)      │
        │     - Decrypt: AES-GCM.decrypt(...)    │
        │                                        │
        │  3. Parse decrypted payload:           │
        │     received_challenge:username:time   │
        │                                        │
        │  4. Validate:                          │
        │     ✓ Decryption successful?           │
        │     ✓ Challenge matches?               │
        │     ✓ Username matches?                │
        │     ✓ Timestamp within 5 min?          │
        └────────────┬───────────────────────────┘
                     │
                     │ All Validations Pass
                     ▼

┌──────────────────────────────────────────────────────────────────┐
│               STEP 4: ROLE MAPPING & TOKEN ISSUANCE              │
└──────────────────────────────────────────────────────────────────┘
                     │
                     ▼
        ┌────────────────────────────────────────┐
        │  Map AD Groups to RA Roles:            │
        │                                        │
        │  User's AD Groups:                     │
        │  - Domain Users                        │
        │  - PKI-RA-Officers                     │
        │                                        │
        │  Role Mapping Table:                   │
        │  "PKI-RA-Admins"    → RA_ADMIN        │
        │  "PKI-RA-Officers"  → RA_OFFICER      │
        │  "PKI-RA-Operators" → RA_OPERATOR     │
        │  "PKI-Auditors"     → AUDITOR         │
        │  "Domain Users"     → END_ENTITY      │
        │                                        │
        │  Assigned Roles: [RA_OFFICER]          │
        └────────────┬───────────────────────────┘
                     │
                     │ Generate JWT Token
                     ▼
        ┌────────────────────────────────────────┐
        │  JWT Access Token (RS256):             │
        │  {                                     │
        │    "sub": "john@corp.local",           │
        │    "userId": "12345",                  │
        │    "displayName": "John Doe",          │
        │    "email": "john@corp.local",         │
        │    "department": "Engineering",        │
        │    "roles": ["RA_OFFICER"],            │
        │    "iat": 1737456789,                  │
        │    "exp": 1737460389 (1 hour)          │
        │  }                                     │
        │  Signed with RS256 Private Key         │
        └────────────┬───────────────────────────┘
                     │
                     │ Create Session
                     ▼
        ┌────────────────────────────────────────┐
        │  User Session Record:                  │
        │  - session_id                          │
        │  - user_id                             │
        │  - ip_address                          │
        │  - user_agent                          │
        │  - login_at                            │
        │  - expires_at (60 min)                 │
        │  - last_activity_at                    │
        └────────────┬───────────────────────────┘
                     │
                     │ Audit Log Entry
                     ▼
        ┌────────────────────────────────────────┐
        │  Audit Log:                            │
        │  - user_id                             │
        │  - action: "LOGIN_SUCCESS"             │
        │  - ip_address                          │
        │  - timestamp                           │
        │  - authentication_method: "AD+CHALLENGE"│
        └────────────┬───────────────────────────┘
                     │
                     │ Delete Used Challenge
                     ▼
        ┌────────────────────────────────────────┐
        │  Challenge Cleanup:                    │
        │  - Remove challenge from cache         │
        │  - Prevent replay attacks              │
        └────────────┬───────────────────────────┘
                     │
                     │ 200 OK Response
                     ▼

┌──────────────────────────────────────────────────────────────────┐
│                  STEP 5: CLIENT RECEIVES TOKEN                   │
└──────────────────────────────────────────────────────────────────┘
                     │
                     ▼
        ┌────────────────────────────────────────┐
        │  Response to Client:                   │
        │  {                                     │
        │    "access_token": "eyJhbGc...",       │
        │    "token_type": "Bearer",             │
        │    "expires_in": 3600,                 │
        │    "refresh_token": "opaque-token",    │
        │    "user": {                           │
        │      "user_id": "12345",               │
        │      "username": "john@corp.local",    │
        │      "display_name": "John Doe",       │
        │      "email": "john@corp.local",       │
        │      "department": "Engineering",      │
        │      "roles": ["RA_OFFICER"]           │
        │    }                                   │
        │  }                                     │
        └────────────┬───────────────────────────┘
                     │
                     │ Store Token
                     ▼
        ┌────────────────────────────────────────┐
        │  Browser:                              │
        │  - Store access_token in memory        │
        │  - Store refresh_token in httpOnly cookie│
        │  - Include in Authorization header:    │
        │    Authorization: Bearer eyJhbGc...    │
        └────────────┬───────────────────────────┘
                     │
                     │ Redirect to Dashboard
                     ▼
        ┌────────────────────────────────────────┐
        │  RA Officer Dashboard                  │
        │                                        │
        │  Welcome, John Doe                     │
        │  ─────────────────────────────         │
        │  📊 Dashboard Statistics               │
        │  ├─ Pending Approvals: 12              │
        │  ├─ Certificates Issued Today: 45      │
        │  └─ Active Certificates: 1,234         │
        │                                        │
        │  📋 Pending Requests (Top 10)          │
        │  ├─ Request #001 - Alice (Email Cert)  │
        │  ├─ Request #002 - Bob (VPN Cert)      │
        │  └─ ...                                │
        │                                        │
        │  [View All] [Search] [Reports]         │
        └────────────────────────────────────────┘
```

---

## 🔐 Multi-Factor Authentication (MFA) Flow

### Email Verification (Required for Sensitive Certificates)

```
┌──────────────────────────────────────────────────────────────────┐
│                   EMAIL VERIFICATION FLOW                        │
└──────────────────────────────────────────────────────────────────┘

Step 1: End Entity Submits CSR
        │
        ▼
┌────────────────────────────────────────┐
│  RA receives CSR submission            │
│  - Validates CSR format                │
│  - Extracts Subject DN                 │
│  - Validates against AD profile        │
└────────────┬───────────────────────────┘
             │
             │ If certificate template requires email verification
             ▼
┌────────────────────────────────────────┐
│  Generate Email Verification:          │
│  - Unique token (UUID)                 │
│  - Verification link (24-hour expiry)  │
│  - Store in email_verifications table  │
└────────────┬───────────────────────────┘
             │
             │ Send Email
             ▼
┌────────────────────────────────────────┐
│  Email to: john@corp.local             │
│  Subject: Certificate Request Email    │
│           Verification                 │
│                                        │
│  Dear John Doe,                        │
│                                        │
│  Please verify your email for          │
│  certificate request #REQ-12345.       │
│                                        │
│  Click here to verify:                 │
│  https://ra.corp.local/verify?         │
│    token=uuid-abc-123                  │
│                                        │
│  Link expires in 24 hours.             │
│                                        │
│  Request Details:                      │
│  - Type: Email Certificate             │
│  - Submitted: 2026-01-16 10:30 AM      │
│                                        │
│  [VERIFY EMAIL] button                 │
└────────────┬───────────────────────────┘
             │
             │ User clicks link
             ▼
┌────────────────────────────────────────┐
│  RA validates token:                   │
│  - Token exists?                       │
│  - Not expired?                        │
│  - Not already used?                   │
│  - Matches request?                    │
└────────────┬───────────────────────────┘
             │
             │ If valid
             ▼
┌────────────────────────────────────────┐
│  Mark request as email_verified        │
│  Update email_verifications table:     │
│  - verified_at = NOW()                 │
│  - ip_address_verified_from            │
└────────────┬───────────────────────────┘
             │
             │ Show success page
             ▼
┌────────────────────────────────────────┐
│  ✓ Email Verified Successfully!       │
│                                        │
│  Your certificate request #REQ-12345   │
│  has been verified.                    │
│                                        │
│  Status: Pending RA Officer Approval   │
│                                        │
│  [View Request Status]                 │
└────────────────────────────────────────┘
```

### Phone/SMS Verification (For High-Value Certificates)

```
┌──────────────────────────────────────────────────────────────────┐
│                    SMS/OTP VERIFICATION FLOW                     │
└──────────────────────────────────────────────────────────────────┘

Step 1: End Entity Submits CSR
        │
        ▼
┌────────────────────────────────────────┐
│  RA determines OTP required            │
│  - High-value certificate template     │
│  - Code signing, admin certificates    │
└────────────┬───────────────────────────┘
             │
             │ Generate OTP
             ▼
┌────────────────────────────────────────┐
│  Generate 6-digit OTP:                 │
│  - Random number: 123456               │
│  - Validity: 5 minutes                 │
│  - Max attempts: 3                     │
│  - Store in otp_verifications table    │
└────────────┬───────────────────────────┘
             │
             │ Send SMS (via SMS Gateway)
             ▼
┌────────────────────────────────────────┐
│  SMS to: +1-234-567-8900               │
│                                        │
│  Your RA verification code: 123456     │
│                                        │
│  Valid for 5 minutes.                  │
│  Request: #REQ-12345                   │
│                                        │
│  Do not share this code.               │
└────────────┬───────────────────────────┘
             │
             │ User enters OTP in portal
             ▼
┌────────────────────────────────────────┐
│  RA Portal OTP Entry Screen:           │
│                                        │
│  Enter 6-digit code sent to:           │
│  +1-234-***-**00                       │
│                                        │
│  [_] [_] [_] [_] [_] [_]               │
│                                        │
│  Expires in: 04:32                     │
│                                        │
│  Didn't receive? [Resend Code]         │
│  (Available in 60 seconds)             │
└────────────┬───────────────────────────┘
             │
             │ Validate OTP
             ▼
┌────────────────────────────────────────┐
│  RA validates OTP:                     │
│  - Code matches?                       │
│  - Not expired?                        │
│  - Attempts < 3?                       │
│  - Request ID matches?                 │
└────────────┬───────────────────────────┘
             │
             ├─ If VALID ─────────────────┐
             │                            │
             ▼                            │
┌────────────────────────────────────┐   │
│  ✓ OTP Verified!                   │   │
│  Mark request: otp_verified = true │   │
│  Update otp_verifications:         │   │
│  - verified_at = NOW()             │   │
│  Proceed to approval workflow      │   │
└────────────────────────────────────┘   │
                                         │
             ├─ If INVALID ──────────────┘
             │
             ▼
┌────────────────────────────────────┐
│  ✗ Invalid Code                    │
│  Attempts remaining: 2/3           │
│  Please try again                  │
└────────────────────────────────────┘
             │
             │ If 3 attempts exhausted
             ▼
┌────────────────────────────────────┐
│  ✗ Maximum Attempts Exceeded       │
│  Request verification via:         │
│  - Contact RA Officer              │
│  - In-person verification          │
│  Request ID: #REQ-12345            │
└────────────────────────────────────┘
```

---

## 📝 Complete Certificate Request Lifecycle

```
┌──────────────────────────────────────────────────────────────────┐
│           CERTIFICATE REQUEST LIFECYCLE - DETAILED FLOW          │
└──────────────────────────────────────────────────────────────────┘

┌─────────────────┐
│  STATE: DRAFT   │  End Entity creating request
└────────┬────────┘
         │
         │ End Entity fills form / uploads CSR
         ▼
┌────────────────────────────────────────────────────────┐
│  1. REQUEST CREATION                                   │
├────────────────────────────────────────────────────────┤
│  End Entity Actions:                                   │
│  ├─ Login with AD credentials                          │
│  ├─ Select certificate template                        │
│  ├─ Option A: Upload PKCS#10 CSR (generated locally)   │
│  ├─ Option B: Fill form (server generates key pair)    │
│  ├─ Provide justification                              │
│  └─ Save as draft OR Submit                            │
│                                                        │
│  RA System Actions:                                    │
│  ├─ Validate CSR format (PKCS#10)                      │
│  ├─ Verify CSR signature (Proof of Possession)         │
│  ├─ Extract Subject DN from CSR                        │
│  ├─ Validate Subject DN vs AD profile                  │
│  │   • CN matches displayName?                         │
│  │   • Email matches mail?                             │
│  │   • OU matches department?                          │
│  ├─ Check public key algorithm (RSA/ECDSA)             │
│  ├─ Check key size (min 2048-bit RSA)                  │
│  ├─ Validate extensions (key usage, EKU)               │
│  ├─ Check user certificate quota                       │
│  ├─ Generate unique request ID                         │
│  └─ Store in database: status = DRAFT                  │
└────────────────┬───────────────────────────────────────┘
                 │
                 │ User clicks "Submit"
                 ▼
┌─────────────────┐
│ STATE: SUBMITTED│  Waiting for verification
└────────┬────────┘
         │
         ▼
┌────────────────────────────────────────────────────────┐
│  2. IDENTITY VERIFICATION (Multi-Layer)                │
├────────────────────────────────────────────────────────┤
│  Layer 1: Email Verification (If Required)             │
│  ├─ Generate unique verification token                 │
│  ├─ Send email with link (24-hour expiry)              │
│  ├─ User clicks link                                   │
│  ├─ RA validates token                                 │
│  └─ Mark: email_verified = TRUE                        │
│                                                        │
│  Layer 2: Phone/SMS Verification (If Required)         │
│  ├─ Generate 6-digit OTP                               │
│  ├─ Send SMS (5-minute expiry, 3 attempts)             │
│  ├─ User enters OTP                                    │
│  ├─ RA validates OTP                                   │
│  └─ Mark: otp_verified = TRUE                          │
│                                                        │
│  Layer 3: Subject DN Validation (Always)               │
│  ├─ Already done during submission                     │
│  ├─ CSR Subject DN vs AD attributes                    │
│  └─ Mark: subject_dn_verified = TRUE                   │
│                                                        │
│  Layer 4: In-Person Verification (High-Value Only)     │
│  ├─ Generate reference number                          │
│  ├─ Notify user to visit RA office                     │
│  ├─ RA Officer checks photo ID                         │
│  ├─ Officer records ID details                         │
│  ├─ Officer confirms face match                        │
│  └─ Mark: identity_verified = TRUE                     │
│                                                        │
│  All Required Verifications Complete?                  │
│  └─ If YES → status = PENDING_APPROVAL                 │
└────────────────┬───────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────┐
│ STATE: PENDING_     │  Waiting for RA Officer
│        APPROVAL     │
└────────┬────────────┘
         │
         │ RA Officer reviews
         ▼
┌────────────────────────────────────────────────────────┐
│  3. RA OFFICER REVIEW (Maker-Checker)                  │
├────────────────────────────────────────────────────────┤
│  RA Officer Dashboard:                                 │
│  ├─ View pending requests queue                        │
│  ├─ Sort by: date, priority, certificate type          │
│  └─ Select request for review                          │
│                                                        │
│  Request Details Screen:                               │
│  ├─ Requester Profile:                                 │
│  │   • Name: John Doe                                  │
│  │   • Department: Engineering                         │
│  │   • Employee ID: EMP12345                           │
│  │   • Manager: Jane Smith                             │
│  │   • Email: john@corp.local                          │
│  ├─ Certificate Details:                               │
│  │   • Template: Email Certificate                     │
│  │   • Validity: 365 days                              │
│  │   • Key Size: RSA 2048                              │
│  │   • Subject DN: CN=John Doe, E=john@...             │
│  ├─ Verification Status:                               │
│  │   • Email Verified: ✓                               │
│  │   • Subject DN Match: ✓                             │
│  │   • Quota Available: ✓                              │
│  ├─ Business Justification:                            │
│  │   "Need email certificate for secure communication  │
│  │    with external partners per project requirements."│
│  ├─ Certificate History:                               │
│  │   • Previously issued: 2 certificates               │
│  │   • Last issued: 2025-06-15                         │
│  │   • Revoked: 0                                      │
│  └─ Policy Checks:                                     │
│      • User authorized: ✓                              │
│      • Template allowed: ✓                             │
│      • No violations: ✓                                │
│                                                        │
│  Officer Actions:                                      │
│  ├─ [Approve] → Proceed to issuance                    │
│  ├─ [Reject] → Select reason + provide explanation     │
│  └─ [Request Info] → Ask for clarification             │
└────────────────┬───────────────────────────────────────┘
                 │
                 ├─ If APPROVED ──────────┐
                 │                        │
                 │                        ▼
                 │           ┌────────────────────┐
                 │           │ STATE: APPROVED    │
                 │           └────────┬───────────┘
                 │                    │
                 │                    │ Forward to CA
                 │                    ▼
                 │           ┌──────────────────────────────────┐
                 │           │  4. CERTIFICATE ISSUANCE         │
                 │           ├──────────────────────────────────┤
                 │           │  RA submits to CA:               │
                 │           │  ├─ SCEP / CMP / REST API        │
                 │           │  ├─ Send PKCS#10 CSR             │
                 │           │  └─ Include template parameters  │
                 │           │                                  │
                 │           │  CA processes:                   │
                 │           │  ├─ Validate CSR                 │
                 │           │  ├─ Sign certificate             │
                 │           │  ├─ Assign serial number         │
                 │           │  └─ Set validity period          │
                 │           │                                  │
                 │           │  RA receives certificate:        │
                 │           │  ├─ Validate certificate         │
                 │           │  ├─ Store certificate metadata   │
                 │           │  ├─ Extract public key hash      │
                 │           │  ├─ Store in certificates table  │
                 │           │  └─ Update request status        │
                 │           └────────┬─────────────────────────┘
                 │                    │
                 │                    ▼
                 │           ┌────────────────────┐
                 │           │ STATE: ISSUED      │
                 │           └────────┬───────────┘
                 │                    │
                 │                    │ Notify user
                 │                    ▼
                 │           ┌──────────────────────────────────┐
                 │           │  5. CERTIFICATE DELIVERY         │
                 │           ├──────────────────────────────────┤
                 │           │  Notification:                   │
                 │           │  ├─ Send email to user           │
                 │           │  ├─ Include download link        │
                 │           │  ├─ Include certificate details  │
                 │           │  └─ In-app notification          │
                 │           │                                  │
                 │           │  Download Options:               │
                 │           │  ├─ PEM format (certificate only)│
                 │           │  ├─ DER format                   │
                 │           │  ├─ PKCS#12 (cert + private key) │
                 │           │  └─ Certificate chain (PEM)      │
                 │           │                                  │
                 │           │  Portal View:                    │
                 │           │  ├─ Certificate details          │
                 │           │  ├─ Validity dates               │
                 │           │  ├─ Serial number                │
                 │           │  ├─ Download buttons             │
                 │           │  └─ Installation instructions    │
                 │           └──────────────────────────────────┘
                 │
                 ├─ If REJECTED ───────┐
                 │                     │
                 │                     ▼
                 │           ┌────────────────────┐
                 │           │ STATE: REJECTED    │
                 │           └────────┬───────────┘
                 │                    │
                 │                    │ Notify user
                 │                    ▼
                 │           ┌──────────────────────────────────┐
                 │           │  Rejection Notification:         │
                 │           │  ├─ Email to requester           │
                 │           │  ├─ Rejection reason             │
                 │           │  ├─ Detailed explanation         │
                 │           │  ├─ Officer comments             │
                 │           │  └─ Option to resubmit           │
                 │           └──────────────────────────────────┘
                 │
                 └─ If INFO NEEDED ───┐
                                      │
                                      ▼
                           ┌─────────────────────────┐
                           │ STATE: INFO_REQUIRED    │
                           └────────┬────────────────┘
                                    │
                                    │ User provides info
                                    ▼
                           ┌──────────────────────────────────┐
                           │  User Response:                  │
                           │  ├─ Receive notification         │
                           │  ├─ View officer's questions     │
                           │  ├─ Provide additional info      │
                           │  ├─ Upload new documents         │
                           │  └─ Resubmit                     │
                           └────────┬─────────────────────────┘
                                    │
                                    │ Returns to officer queue
                                    │
                                    └──► Back to PENDING_APPROVAL
```

---

## ⚡ Auto-Enrollment Workflow (Detailed)

```
┌──────────────────────────────────────────────────────────────────┐
│              AUTO-ENROLLMENT COMPLETE WORKFLOW                   │
└──────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│  PREREQUISITE: RA Administrator Configuration                   │
├─────────────────────────────────────────────────────────────────┤
│  1. Create Certificate Template:                                │
│     ├─ Name: "Employee Email Certificate"                       │
│     ├─ Type: Email/S/MIME                                       │
│     ├─ Key Size: RSA 2048                                       │
│     ├─ Validity: 365 days                                       │
│     ├─ Key Usage: Digital Signature, Key Encipherment           │
│     └─ EKU: Email Protection, Client Authentication             │
│                                                                 │
│  2. Create Auto-Enrollment Policy:                              │
│     ├─ Policy Name: "All Employees Auto Email Cert"             │
│     ├─ Certificate Template: "Employee Email Certificate"       │
│     ├─ Trigger: ON_LOGIN (when user logs in)                    │
│     ├─ Eligible Groups: ["All Employees", "Domain Users"]       │
│     ├─ Auto-Approve: YES (no manual approval)                   │
│     ├─ Auto-Renew: YES (30 days before expiry)                  │
│     ├─ Key Generation: Server-side                              │
│     ├─ Subject DN Template:                                     │
│     │   CN=${displayName}                                       │
│     │   E=${mail}                                               │
│     │   OU=${department}                                        │
│     │   O=Corp                                                  │
│     └─ SAN Template: email:${mail}                              │
│                                                                 │
│  3. Enable Policy:                                              │
│     └─ Status: ACTIVE                                           │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  STEP 1: END ENTITY LOGIN                                       │
├─────────────────────────────────────────────────────────────────┤
│  User: John Doe                                                 │
│  Username: john@corp.local                                      │
│  Password: ********                                             │
│                                                                 │
│  ├─ User enters credentials                                     │
│  ├─ RA sends to Active Directory                                │
│  ├─ AD validates credentials                                    │
│  └─ AD returns user attributes:                                 │
│      • displayName: "John Doe"                                  │
│      • mail: "john@corp.local"                                  │
│      • department: "Engineering"                                │
│      • employeeID: "EMP12345"                                   │
│      • memberOf: ["Domain Users", "All Employees"]              │
└────────────────┬────────────────────────────────────────────────┘
                 │
                 │ Authentication successful
                 ▼
┌─────────────────────────────────────────────────────────────────┐
│  STEP 2: AUTO-ENROLLMENT TRIGGER (Background Daemon)            │
├─────────────────────────────────────────────────────────────────┤
│  Trigger: ON_LOGIN event detected                               │
│                                                                 │
│  RA System Actions:                                             │
│  ├─ Query all auto-enrollment policies                          │
│  │   WHERE trigger = 'ON_LOGIN'                                 │
│  │   AND status = 'ACTIVE'                                      │
│  │                                                              │
│  ├─ Found: "All Employees Auto Email Cert" policy               │
│  │                                                              │
│  ├─ Check user eligibility:                                     │
│  │   • User's AD groups: ["Domain Users", "All Employees"]      │
│  │   • Policy eligible groups: ["All Employees", "Domain Users"]│
│  │   • Match found: ✓ User is eligible                         │
│  │                                                              │
│  ├─ Check existing certificates:                                │
│  │   SELECT * FROM certificates                                 │
│  │   WHERE user_id = 'john@corp.local'                          │
│  │   AND template_id = 'employee-email-cert'                    │
│  │   AND status = 'ACTIVE'                                      │
│  │   AND not_after > NOW() + INTERVAL '30 days'                │
│  │                                                              │
│  └─ Result: No valid certificate found                          │
│      → Initiate auto-enrollment!                                │
└────────────────┬────────────────────────────────────────────────┘
                 │
                 │ User needs certificate
                 ▼
┌─────────────────────────────────────────────────────────────────┐
│  STEP 3: AUTOMATIC CERTIFICATE GENERATION (Background)          │
├─────────────────────────────────────────────────────────────────┤
│  3.1. Generate Key Pair (Server-side):                          │
│       ├─ Algorithm: RSA                                         │
│       ├─ Key Size: 2048 bits                                    │
│       ├─ Private Key: Store securely (encrypted)                │
│       └─ Public Key: Extract for CSR                            │
│                                                                 │
│  3.2. Build Subject DN from AD attributes:                      │
│       Template: CN=${displayName}, E=${mail}, OU=${department}  │
│       Result:   CN=John Doe, E=john@corp.local, OU=Engineering, │
│                 O=Corp, C=US                                    │
│                                                                 │
│  3.3. Build Subject Alternative Names (SAN):                    │
│       Template: email:${mail}, UPN:${userPrincipalName}         │
│       Result:   email:john@corp.local                           │
│                                                                 │
│  3.4. Create PKCS#10 CSR:                                       │
│       ├─ Subject: CN=John Doe, E=john@corp.local, ...           │
│       ├─ Public Key: (from generated key pair)                  │
│       ├─ Extensions:                                            │
│       │   • Key Usage: Digital Signature, Key Encipherment      │
│       │   • Extended Key Usage: Email Protection                │
│       │   • Subject Alternative Name: email:john@corp.local     │
│       └─ Sign CSR with private key                              │
│                                                                 │
│  3.5. Create Internal Request Record:                           │
│       INSERT INTO certificate_requests (                        │
│         request_id,                                             │
│         user_id,                                                │
│         csr_pem,                                                │
│         subject_dn,                                             │
│         template_id,                                            │
│         status,                                                 │
│         submission_type,                                        │
│         auto_enrollment_policy_id,                              │
│         created_at                                              │
│       ) VALUES (                                                │
│         'AUTO-REQ-12345',                                       │
│         'john@corp.local',                                      │
│         '-----BEGIN CERTIFICATE REQUEST-----...',               │
│         'CN=John Doe, E=john@...',                              │
│         'employee-email-cert',                                  │
│         'AUTO_APPROVED',  ← Skip manual approval                │
│         'AUTO_ENROLLMENT',                                      │
│         'policy-uuid-123',                                      │
│         NOW()                                                   │
│       );                                                        │
└────────────────┬────────────────────────────────────────────────┘
                 │
                 │ CSR ready, auto-approved
                 ▼
┌─────────────────────────────────────────────────────────────────┐
│  STEP 4: SUBMIT TO CERTIFICATE AUTHORITY (Automatic)            │
├─────────────────────────────────────────────────────────────────┤
│  4.1. RA → CA Integration:                                      │
│       ├─ Method: REST API / SCEP / CMP                          │
│       ├─ Endpoint: https://ca.corp.local/api/certificate/issue  │
│       ├─ Authentication: mTLS (mutual TLS)                      │
│       └─ Payload:                                               │
│           {                                                     │
│             "csr": "-----BEGIN CERTIFICATE REQUEST-----...",    │
│             "profile": "email-certificate-profile",             │
│             "validity_days": 365,                               │
│             "requester": "RA-Auto-Enrollment-Service"           │
│           }                                                     │
│                                                                 │
│  4.2. CA Processing:                                            │
│       ├─ Validate CSR format                                    │
│       ├─ Verify CSR signature                                   │
│       ├─ Apply certificate profile                              │
│       ├─ Sign certificate with CA private key                   │
│       ├─ Assign serial number: 4A:3B:2C:1D:...                  │
│       ├─ Set validity: Not Before / Not After                   │
│       └─ Return signed certificate                              │
│                                                                 │
│  4.3. RA Receives Certificate:                                  │
│       ├─ Validate certificate signature                         │
│       ├─ Verify certificate chain                               │
│       ├─ Extract certificate details:                           │
│       │   • Serial Number                                       │
│       │   • Subject DN                                          │
│       │   • Validity Period                                     │
│       │   • Public Key Hash                                     │
│       └─ Store in database                                      │
│                                                                 │
│  4.4. Update Records:                                           │
│       UPDATE certificate_requests                               │
│       SET status = 'ISSUED',                                    │
│           issued_at = NOW(),                                    │
│           certificate_serial = '4A:3B:2C:1D:...'                │
│       WHERE request_id = 'AUTO-REQ-12345';                      │
│                                                                 │
│       INSERT INTO certificates (                                │
│         certificate_id,                                         │
│         user_id,                                                │
│         request_id,                                             │
│         serial_number,                                          │
│         subject_dn,                                             │
│         issuer_dn,                                              │
│         not_before,                                             │
│         not_after,                                              │
│         status,                                                 │
│         public_key_hash,                                        │
│         certificate_pem,                                        │
│         issued_at                                               │
│       ) VALUES (...);                                           │
└────────────────┬────────────────────────────────────────────────┘
                 │
                 │ Certificate issued
                 ▼
┌─────────────────────────────────────────────────────────────────┐
│  STEP 5: NOTIFICATION & DELIVERY (Automatic)                    │
├─────────────────────────────────────────────────────────────────┤
│  5.1. Email Notification:                                       │
│       To: john@corp.local                                       │
│       Subject: Your Email Certificate is Ready                  │
│                                                                 │
│       Dear John Doe,                                            │
│                                                                 │
│       Your email certificate has been automatically issued      │
│       and is ready for download.                                │
│                                                                 │
│       Certificate Details:                                      │
│       ├─ Type: Email Certificate                                │
│       ├─ Serial: 4A:3B:2C:1D:...                                │
│       ├─ Valid From: 2026-01-16                                 │
│       ├─ Valid Until: 2027-01-16                                │
│       └─ Subject: CN=John Doe, E=john@corp.local                │
│                                                                 │
│       Download: https://ra.corp.local/certificates/AUTO-REQ-... │
│                                                                 │
│       [DOWNLOAD CERTIFICATE] button                             │
│                                                                 │
│  5.2. In-App Notification (Dashboard):                          │
│       ┌─────────────────────────────────────────┐              │
│       │ 🔔 New Notification                     │              │
│       │                                         │              │
│       │ ✓ Certificate Issued                    │              │
│       │                                         │              │
│       │ Your email certificate (AUTO-REQ-12345) │              │
│       │ has been issued and is ready.           │              │
│       │                                         │              │
│       │ [Download Now] [View Details]           │              │
│       └─────────────────────────────────────────┘              │
│                                                                 │
│  5.3. Certificate Download Portal:                              │
│       ┌─────────────────────────────────────────────────────┐  │
│       │  Certificate Details                                │  │
│       │  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  │  │
│       │  Subject: CN=John Doe, E=john@corp.local            │  │
│       │  Serial: 4A:3B:2C:1D:...                            │  │
│       │  Valid: 2026-01-16 to 2027-01-16                    │  │
│       │  Type: Email Certificate                            │  │
│       │  Status: ✓ Active                                   │  │
│       │                                                     │  │
│       │  Download Formats:                                  │  │
│       │  [📄 PEM (Certificate Only)]                        │  │
│       │  [📄 DER Binary Format]                             │  │
│       │  [🔐 PKCS#12 (Cert + Private Key)] ← Password req'd │  │
│       │  [📦 Certificate Chain (PEM)]                       │  │
│       │                                                     │  │
│       │  Installation Instructions:                         │  │
│       │  • Outlook: Import PKCS#12 file                     │  │
│       │  • Thunderbird: Tools → Account Settings            │  │
│       │  • Apple Mail: Keychain Access                      │  │
│       └─────────────────────────────────────────────────────┘  │
│                                                                 │
│  5.4. Audit Log Entry:                                          │
│       INSERT INTO audit_logs (                                  │
│         action: "AUTO_ENROLLMENT_COMPLETE",                     │
│         user_id: "john@corp.local",                             │
│         request_id: "AUTO-REQ-12345",                           │
│         certificate_serial: "4A:3B:2C:1D:...",                  │
│         policy_id: "policy-uuid-123",                           │
│         timestamp: NOW(),                                       │
│         details: {                                              │
│           "trigger": "ON_LOGIN",                                │
│           "processing_time_ms": 2345,                           │
│           "auto_approved": true                                 │
│         }                                                       │
│       );                                                        │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ User logs in to dashboard
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  END ENTITY DASHBOARD (After Auto-Enrollment)                   │
├─────────────────────────────────────────────────────────────────┤
│  Welcome, John Doe                                              │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  │
│                                                                 │
│  📊 Dashboard Summary                                           │
│  ├─ Active Certificates: 1                                      │
│  ├─ Pending Requests: 0                                         │
│  ├─ Expiring Soon: 0                                            │
│  └─ Recent Activity: Certificate issued (2 min ago)             │
│                                                                 │
│  🔔 Notifications (1 new)                                       │
│  └─ ✓ Email Certificate Issued - Download Now                   │
│                                                                 │
│  📜 My Certificates                                             │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ Email Certificate                                       │   │
│  │ ─────────────────────────────────────────────────────   │   │
│  │ Serial: 4A:3B:2C:1D:...                                 │   │
│  │ Valid: Jan 16, 2026 - Jan 16, 2027                      │   │
│  │ Status: ✓ Active                                        │   │
│  │ Type: Auto-Enrolled                                     │   │
│  │                                                         │   │
│  │ [Download] [View Details] [Renew]                       │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  📋 Recent Requests                                             │
│  └─ AUTO-REQ-12345 - Email Certificate - ✓ Issued (2 min ago)  │
│                                                                 │
│  [View All Certificates] [Request New Certificate]              │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🔄 Automatic Certificate Renewal Flow

```
┌──────────────────────────────────────────────────────────────────┐
│            AUTOMATIC CERTIFICATE RENEWAL WORKFLOW                │
└──────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│  BACKGROUND JOB: Certificate Expiry Monitor (Runs Daily)         │
├─────────────────────────────────────────────────────────────────┤
│  Scheduled: 2:00 AM daily                                        │
│                                                                 │
│  Query: SELECT certificates                                      │
│         WHERE status = 'ACTIVE'                                  │
│         AND not_after <= NOW() + INTERVAL '30 days'              │
│         AND auto_renew_enabled = TRUE                            │
│                                                                 │
│  Results:                                                        │
│  ├─ Certificate #1: John Doe - Expires in 25 days               │
│  ├─ Certificate #2: Alice Smith - Expires in 15 days            │
│  └─ Certificate #3: Bob Johnson - Expires in 7 days             │
└────────────────┬────────────────────────────────────────────────┘
                 │
                 │ For each expiring certificate
                 ▼
┌─────────────────────────────────────────────────────────────────┐
│  STEP 1: ELIGIBILITY CHECK                                      │
├─────────────────────────────────────────────────────────────────┤
│  Certificate: John Doe - Email Certificate                      │
│  Serial: 4A:3B:2C:1D:...                                         │
│  Expires: 2026-02-10 (25 days from now)                         │
│                                                                 │
│  Checks:                                                         │
│  ├─ User still in AD? → YES                                     │
│  ├─ User account active? → YES                                  │
│  ├─ User still in eligible groups? → YES                        │
│  ├─ Auto-renew enabled for template? → YES                      │
│  ├─ Certificate not revoked? → YES                              │
│  ├─ Renewal not already in progress? → YES                      │
│  └─ User quota available? → YES                                 │
│                                                                 │
│  Result: ✓ Eligible for auto-renewal                            │
└────────────────┬────────────────────────────────────────────────┘
                 │
                 │ Initiate renewal
                 ▼
┌─────────────────────────────────────────────────────────────────┐
│  STEP 2: GENERATE NEW CERTIFICATE (Same Process as Enrollment)  │
├─────────────────────────────────────────────────────────────────┤
│  2.1. Generate new key pair (recommended for security)          │
│       ├─ Algorithm: RSA 2048                                    │
│       └─ Store new private key                                  │
│                                                                 │
│  2.2. Build Subject DN (from current AD attributes)             │
│       ├─ Query AD for latest user info                          │
│       └─ DN: CN=John Doe, E=john@corp.local, ...                │
│                                                                 │
│  2.3. Create renewal CSR                                        │
│       ├─ Mark as renewal request                                │
│       ├─ Link to original certificate                           │
│       └─ Sign with new private key                              │
│                                                                 │
│  2.4. Create renewal request record                             │
│       INSERT INTO certificate_requests (                        │
│         request_id: 'RENEW-REQ-67890',                          │
│         user_id: 'john@corp.local',                             │
│         submission_type: 'AUTO_RENEWAL',                        │
│         original_certificate_id: 'cert-12345',                  │
│         status: 'AUTO_APPROVED',                                │
│         ...                                                     │
│       );                                                        │
│                                                                 │
│  2.5. Submit to CA (same as new enrollment)                     │
│       └─ CA issues new certificate                              │
│                                                                 │
│  2.6. Receive new certificate                                   │
│       ├─ Serial: 5B:4C:3D:2E:...                                │
│       ├─ Valid: 2026-01-16 to 2027-01-16                        │
│       └─ Store in database                                      │
└────────────────┬────────────────────────────────────────────────┘
                 │
                 │ New certificate issued
                 ▼
┌─────────────────────────────────────────────────────────────────┐
│  STEP 3: GRACE PERIOD & OLD CERTIFICATE HANDLING                │
├─────────────────────────────────────────────────────────────────┤
│  Grace Period: 7 days (both certificates valid)                 │
│                                                                 │
│  Old Certificate:                                               │
│  ├─ Serial: 4A:3B:2C:1D:...                                     │
│  ├─ Status: ACTIVE (grace period)                               │
│  ├─ Expires: 2026-02-10                                         │
│  └─ Will auto-revoke: 2026-01-23 (after grace)                  │
│                                                                 │
│  New Certificate:                                               │
│  ├─ Serial: 5B:4C:3D:2E:...                                     │
│  ├─ Status: ACTIVE                                              │
│  ├─ Valid: 2026-01-16 to 2027-01-16                             │
│  └─ Linked to old cert for tracking                             │
│                                                                 │
│  User Experience:                                               │
│  ├─ Both certificates work during grace period                  │
│  ├─ User can transition gradually                               │
│  ├─ No service interruption                                     │
│  └─ Email clients can be updated one by one                     │
└────────────────┬────────────────────────────────────────────────┘
                 │
                 │ Notify user
                 ▼
┌─────────────────────────────────────────────────────────────────┐
│  STEP 4: USER NOTIFICATION                                      │
├─────────────────────────────────────────────────────────────────┤
│  Email Notification:                                            │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  │
│  To: john@corp.local                                            │
│  Subject: Certificate Automatically Renewed                     │
│                                                                 │
│  Dear John Doe,                                                 │
│                                                                 │
│  Your email certificate has been automatically renewed.         │
│                                                                 │
│  NEW Certificate Details:                                       │
│  ├─ Serial: 5B:4C:3D:2E:...                                     │
│  ├─ Valid: Jan 16, 2026 - Jan 16, 2027                          │
│  └─ Download: [DOWNLOAD NEW CERTIFICATE]                        │
│                                                                 │
│  OLD Certificate:                                               │
│  ├─ Serial: 4A:3B:2C:1D:...                                     │
│  ├─ Expires: Feb 10, 2026                                       │
│  ├─ Grace Period: 7 days (both valid)                           │
│  └─ Auto-revokes: Jan 23, 2026                                  │
│                                                                 │
│  Action Required:                                               │
│  1. Download new certificate                                    │
│  2. Install in your email client                                │
│  3. Test email signing/encryption                               │
│  4. Old certificate will be revoked automatically               │
│                                                                 │
│  Installation Instructions: [View Guide]                        │
│                                                                 │
│  Dashboard Notification:                                        │
│  ┌─────────────────────────────────────────┐                   │
│  │ 🔔 Certificate Renewed                  │                   │
│  │                                         │                   │
│  │ Your email certificate has been         │                   │
│  │ automatically renewed.                  │                   │
│  │                                         │                   │
│  │ Download new certificate before         │                   │
│  │ Jan 23, 2026.                           │                   │
│  │                                         │                   │
│  │ [Download] [View Details] [Dismiss]     │                   │
│  └─────────────────────────────────────────┘                   │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ After grace period (7 days)
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  STEP 5: AUTO-REVOKE OLD CERTIFICATE                            │
├─────────────────────────────────────────────────────────────────┤
│  Background Job: Grace Period Expiry Check                      │
│                                                                 │
│  Query: SELECT certificates                                      │
│         WHERE status = 'ACTIVE'                                  │
│         AND renewal_grace_expires_at <= NOW()                    │
│         AND renewed_by_certificate_id IS NOT NULL                │
│                                                                 │
│  Found: Certificate 4A:3B:2C:1D:... (John Doe's old cert)       │
│                                                                 │
│  Actions:                                                        │
│  ├─ Submit revocation to CA                                     │
│  │   • Reason: SUPERSEDED (replaced by new cert)                │
│  │   • Revocation Date: 2026-01-23                              │
│  ├─ Update database:                                            │
│  │   UPDATE certificates                                        │
│  │   SET status = 'REVOKED',                                    │
│  │       revocation_reason = 'SUPERSEDED',                      │
│  │       revoked_at = NOW()                                     │
│  │   WHERE serial_number = '4A:3B:2C:1D:...';                   │
│  ├─ Audit log entry                                             │
│  └─ Notification (optional):                                    │
│      Email: "Old certificate revoked as planned"                │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🔍 Summary - Complete Flow Highlights

### Key Features Implemented:

1. ✅ **5-Level User Hierarchy**
   - Super Admin → RA Admin → RA Officer → RA Operator → Auditor → End Entity

2. ✅ **Challenge-Response Authentication**
   - Password never transmitted
   - PBKDF2 key derivation
   - AES-256-GCM encryption
   - AD validation

3. ✅ **Multi-Layer Identity Verification**
   - Email verification (24-hour link)
   - SMS/OTP (6-digit, 5-minute expiry)
   - Subject DN validation (AD profile match)
   - In-person verification (high-value certs)

4. ✅ **Maker-Checker Workflow**
   - Operator submits (Maker)
   - Officer approves (Checker)
   - Separation of duties

5. ✅ **Auto-Enrollment**
   - ON_LOGIN trigger
   - AD group-based eligibility
   - Automatic approval
   - Background processing
   - No user action required

6. ✅ **Automatic Renewal**
   - 30-day expiry threshold
   - New key pair generation
   - Grace period (7 days)
   - Auto-revoke old certificate
   - Email notifications

7. ✅ **Complete Certificate Lifecycle**
   - Draft → Submitted → Verified → Pending Approval
   - → Approved → Issued → Active → Renewing → Revoked

8. ✅ **Comprehensive Audit Trail**
   - All actions logged
   - Tamper-proof storage
   - Full compliance reporting

---

**Document Status**: ✅ Complete
**Last Updated**: 2026-01-16
**Version**: 2.0 - Complete MVP with Auto-Enrollment
**Total Flow Diagrams**: 8 comprehensive workflows
