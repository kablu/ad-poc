# Registration Authority (RA) - User Hierarchy Flow Diagram

**Date**: 2026-01-16
**Version**: 1.0
**Purpose**: Complete user hierarchy and role-based access control for RA Web Application

---

## 🏗️ User Hierarchy Structure

```
┌─────────────────────────────────────────────────────────────────┐
│                    Active Directory (AD)                        │
│                  Authentication Source                          │
│              (LDAP/LDAPS - ldap://ad.company.com)              │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             │ Authentication & Group Membership
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│              RA Web Application - Role Mapping                  │
│                                                                 │
│   AD Group                          →    Application Role      │
│   ─────────────────────────────────────────────────────────    │
│   "PKI-RA-Admins"                  →    RA Administrator      │
│   "PKI-RA-Officers"                →    RA Officer            │
│   "PKI-RA-Operators"               →    RA Operator           │
│   "PKI-Auditors"                   →    Auditor               │
│   "Domain Users" (default)         →    End Entity            │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             │ Role Assignment
                             │
                             ▼
        ┌────────────────────────────────────────────┐
        │                                            │
        │     5 User Roles in RA Application         │
        │                                            │
        └────────────────────────────────────────────┘
                             │
                ┌────────────┴────────────┐
                │                         │
                ▼                         ▼
    ┌──────────────────────┐    ┌──────────────────────┐
    │  RA Administrator    │    │    RA Officer        │
    │  (Full Control)      │    │  (Approval Power)    │
    │                      │    │                      │
    │  • System Config     │    │  • Approve CSR       │
    │  • User Management   │    │  • Revoke Certs      │
    │  • Template Config   │    │  • View All Requests │
    │  • Audit Logs        │    │  • Generate Reports  │
    │  • Policy Settings   │    │  • Identity Verify   │
    └──────────────────────┘    └──────────────────────┘
                │                         │
                └────────────┬────────────┘
                             │
                ┌────────────┴────────────┐
                │                         │
                ▼                         ▼
    ┌──────────────────────┐    ┌──────────────────────┐
    │   RA Operator        │    │     Auditor          │
    │  (Submission)        │    │   (Read-Only)        │
    │                      │    │                      │
    │  • Submit CSR        │    │  • View Operations   │
    │  • View Own Requests │    │  • Access Audit Logs │
    │  • Upload Documents  │    │  • Generate Reports  │
    │  • Download Certs    │    │  • Export Data       │
    │  • Update Pre-Approve│    │  • NO Modifications  │
    └──────────────────────┘    └──────────────────────┘
                │
                └─────────────┐
                              │
                              ▼
                    ┌──────────────────────┐
                    │    End Entity        │
                    │   (Self-Service)     │
                    │                      │
                    │  • Submit Own CSR    │
                    │  • View Own Status   │
                    │  • Download Own Cert │
                    │  • Renew Own Cert    │
                    │  • View Expiry Notice│
                    └──────────────────────┘
```

---

## 👥 Detailed Role Hierarchy

### Level 1: RA Administrator (Highest Authority)

```
┌─────────────────────────────────────────────────────────────┐
│                    RA Administrator                         │
│                   (PKI-RA-Admins Group)                     │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  🔧 SYSTEM CONFIGURATION                                    │
│  ├─ Configure CA integration                               │
│  ├─ Configure AD/LDAP settings                             │
│  ├─ Set system-wide parameters                             │
│  └─ Enable/disable features                                │
│                                                             │
│  👤 USER MANAGEMENT                                         │
│  ├─ Assign/revoke roles                                    │
│  ├─ Map AD groups to roles                                 │
│  ├─ Suspend/activate user accounts                         │
│  └─ Set user certificate quotas                            │
│                                                             │
│  📋 CERTIFICATE TEMPLATE MANAGEMENT                         │
│  ├─ Create/edit/delete templates                           │
│  ├─ Configure auto-enrollment policies                     │
│  ├─ Set template permissions                               │
│  ├─ Define key usage extensions                            │
│  └─ Set validity periods                                   │
│                                                             │
│  🔐 SECURITY & POLICY                                       │
│  ├─ Configure authentication policies                      │
│  ├─ Set password complexity rules                          │
│  ├─ Define approval workflows                              │
│  ├─ Configure MFA requirements                             │
│  └─ Set session timeout                                    │
│                                                             │
│  📊 AUDIT & COMPLIANCE                                      │
│  ├─ View complete audit logs                               │
│  ├─ Export compliance reports                              │
│  ├─ Configure log retention                                │
│  └─ Monitor system health                                  │
│                                                             │
│  ⚡ AUTO-ENROLLMENT                                         │
│  ├─ Enable/disable auto-enrollment globally                │
│  ├─ Configure enrollment triggers                          │
│  ├─ Set renewal thresholds                                 │
│  ├─ Trigger bulk enrollment                                │
│  └─ Monitor enrollment jobs                                │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Level 2: RA Officer (Approval Authority)

```
┌─────────────────────────────────────────────────────────────┐
│                      RA Officer                             │
│                 (PKI-RA-Officers Group)                     │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ✅ CERTIFICATE APPROVAL                                    │
│  ├─ Review pending CSR requests                            │
│  ├─ Approve certificate requests                           │
│  ├─ Reject requests with reason                            │
│  ├─ Request additional information                         │
│  └─ View request history                                   │
│                                                             │
│  🔴 CERTIFICATE REVOCATION                                  │
│  ├─ Revoke certificates                                    │
│  ├─ Select revocation reason                               │
│  ├─ Add revocation comments                                │
│  └─ Generate CRL                                           │
│                                                             │
│  🔍 IDENTITY VERIFICATION                                   │
│  ├─ Verify end entity identity (face-to-face)              │
│  ├─ Check government ID documents                          │
│  ├─ Record verification details                            │
│  └─ Approve high-security certificates                     │
│                                                             │
│  📂 REQUEST MANAGEMENT                                      │
│  ├─ View all certificate requests                          │
│  ├─ Search/filter requests                                 │
│  ├─ View certificate details                               │
│  ├─ Download certificates                                  │
│  └─ View certificate chains                                │
│                                                             │
│  📊 REPORTING                                               │
│  ├─ Generate issuance reports                              │
│  ├─ Generate revocation reports                            │
│  ├─ View audit logs                                        │
│  └─ Export reports (CSV, PDF)                              │
│                                                             │
│  ⚡ AUTO-ENROLLMENT OPERATIONS                              │
│  ├─ Trigger bulk auto-enrollment                           │
│  ├─ Review auto-enrollment jobs                            │
│  └─ Monitor enrollment status                              │
│                                                             │
│  ⚠️ CANNOT DO                                               │
│  ├─ ✗ Modify system configuration                          │
│  ├─ ✗ Manage user roles                                    │
│  ├─ ✗ Create certificate templates                         │
│  └─ ✗ Change security policies                             │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Level 3: RA Operator (Submission Authority)

```
┌─────────────────────────────────────────────────────────────┐
│                     RA Operator                             │
│                (PKI-RA-Operators Group)                     │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  📝 CERTIFICATE REQUEST SUBMISSION                          │
│  ├─ Submit CSR on behalf of end entities                   │
│  ├─ Upload PKCS#10 CSR files                               │
│  ├─ Fill certificate request forms                         │
│  ├─ Select certificate template                            │
│  └─ Provide justification                                  │
│                                                             │
│  📎 DOCUMENT MANAGEMENT                                     │
│  ├─ Upload supporting documents                            │
│  ├─ Attach identity proofs                                 │
│  ├─ Upload authorization letters                           │
│  └─ Manage document attachments                            │
│                                                             │
│  👁️ VIEW OWN SUBMISSIONS                                   │
│  ├─ View own submitted requests                            │
│  ├─ Track request status                                   │
│  ├─ View approval/rejection reasons                        │
│  └─ View request history                                   │
│                                                             │
│  ✏️ UPDATE REQUESTS (Pre-Approval)                         │
│  ├─ Update pending requests                                │
│  ├─ Add missing information                                │
│  ├─ Correct errors                                         │
│  └─ Cancel draft requests                                  │
│                                                             │
│  📥 DOWNLOAD CERTIFICATES                                   │
│  ├─ Download issued certificates                           │
│  ├─ Download certificate chains                            │
│  ├─ Export in multiple formats                             │
│  └─ Deliver to end entities                                │
│                                                             │
│  ⚠️ CANNOT DO                                               │
│  ├─ ✗ Approve/reject requests                              │
│  ├─ ✗ Revoke certificates                                  │
│  ├─ ✗ View all requests                                    │
│  ├─ ✗ Access audit logs                                    │
│  └─ ✗ Modify system settings                               │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Level 4: Auditor (Read-Only Authority)

```
┌─────────────────────────────────────────────────────────────┐
│                        Auditor                              │
│                  (PKI-Auditors Group)                       │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  👁️ VIEW ALL OPERATIONS                                    │
│  ├─ View all certificate requests                          │
│  ├─ View all issued certificates                           │
│  ├─ View revocation records                                │
│  ├─ View approval/rejection history                        │
│  └─ View user activities                                   │
│                                                             │
│  📜 AUDIT LOG ACCESS                                        │
│  ├─ Access complete audit trail                            │
│  ├─ Search audit logs by user/date/action                  │
│  ├─ View authentication logs                               │
│  ├─ View failed access attempts                            │
│  └─ Track certificate lifecycle                            │
│                                                             │
│  📊 COMPLIANCE REPORTING                                    │
│  ├─ Generate compliance reports                            │
│  ├─ Export audit data (CSV, JSON, PDF)                     │
│  ├─ Create custom reports                                  │
│  ├─ Schedule automated reports                             │
│  └─ Analyze trends and patterns                            │
│                                                             │
│  📈 ANALYTICS                                               │
│  ├─ View certificate issuance statistics                   │
│  ├─ View revocation statistics                             │
│  ├─ Analyze approval/rejection rates                       │
│  ├─ Monitor system usage                                   │
│  └─ Identify anomalies                                     │
│                                                             │
│  🔒 SECURITY MONITORING                                     │
│  ├─ Review security events                                 │
│  ├─ Track failed authentication                            │
│  ├─ Monitor policy violations                              │
│  └─ Identify suspicious activities                         │
│                                                             │
│  ⚠️ CANNOT DO (Read-Only Role)                             │
│  ├─ ✗ Submit certificate requests                          │
│  ├─ ✗ Approve/reject requests                              │
│  ├─ ✗ Revoke certificates                                  │
│  ├─ ✗ Modify any records                                   │
│  ├─ ✗ Delete audit logs                                    │
│  └─ ✗ Change system configuration                          │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Level 5: End Entity (Self-Service)

```
┌─────────────────────────────────────────────────────────────┐
│                      End Entity                             │
│                  (Domain Users Group)                       │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  📝 SELF-SERVICE CERTIFICATE REQUEST                        │
│  ├─ Submit own certificate requests                        │
│  ├─ Upload own PKCS#10 CSR                                 │
│  ├─ Generate key pair (client/server)                      │
│  ├─ Select available templates                             │
│  └─ Provide justification                                  │
│                                                             │
│  👁️ VIEW OWN CERTIFICATES                                  │
│  ├─ View own certificate requests                          │
│  ├─ Track own request status                               │
│  ├─ View own issued certificates                           │
│  ├─ Check certificate validity                             │
│  └─ View expiration dates                                  │
│                                                             │
│  📥 DOWNLOAD OWN CERTIFICATES                               │
│  ├─ Download issued certificates                           │
│  ├─ Download private keys (if server-generated)            │
│  ├─ Download PKCS#12 bundles                               │
│  ├─ Export in PEM/DER formats                              │
│  └─ Download certificate chains                            │
│                                                             │
│  🔄 CERTIFICATE RENEWAL                                     │
│  ├─ Renew expiring certificates                            │
│  ├─ Generate new key pair for renewal                      │
│  ├─ Submit renewal requests                                │
│  └─ Track renewal status                                   │
│                                                             │
│  📧 NOTIFICATIONS                                           │
│  ├─ Receive certificate issuance notices                   │
│  ├─ Receive expiration warnings                            │
│  ├─ Receive renewal reminders                              │
│  └─ View notification history                              │
│                                                             │
│  🔔 AUTO-ENROLLMENT (Automatic)                             │
│  ├─ Automatic certificate issuance (if eligible)           │
│  ├─ Automatic renewal (if configured)                      │
│  ├─ Receive auto-enrolled certificates                     │
│  └─ Download auto-issued certificates                      │
│                                                             │
│  ⚠️ CANNOT DO                                               │
│  ├─ ✗ Submit requests for others                           │
│  ├─ ✗ View others' certificates                            │
│  ├─ ✗ Approve/reject requests                              │
│  ├─ ✗ Revoke certificates                                  │
│  ├─ ✗ Access audit logs                                    │
│  └─ ✗ View system configuration                            │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 🔐 Access Control Matrix

### Certificate Request Operations

| Operation | Admin | Officer | Operator | Auditor | End Entity |
|-----------|-------|---------|----------|---------|------------|
| Submit CSR (for self) | ✅ | ✅ | ✅ | ❌ | ✅ |
| Submit CSR (for others) | ✅ | ✅ | ✅ | ❌ | ❌ |
| Upload PKCS#10 CSR | ✅ | ✅ | ✅ | ❌ | ✅ (own) |
| Approve CSR | ✅ | ✅ | ❌ | ❌ | ❌ |
| Reject CSR | ✅ | ✅ | ❌ | ❌ | ❌ |
| View All Requests | ✅ | ✅ | ❌ | ✅ | ❌ |
| View Own Requests | ✅ | ✅ | ✅ | ✅ | ✅ |
| Update Pre-Approval | ✅ | ✅ | ✅ | ❌ | ✅ (own) |
| Cancel Request | ✅ | ✅ | ✅ (own) | ❌ | ✅ (own) |

### Certificate Lifecycle Operations

| Operation | Admin | Officer | Operator | Auditor | End Entity |
|-----------|-------|---------|----------|---------|------------|
| Issue Certificate | ✅ | ✅ | ❌ | ❌ | ❌ |
| Revoke Certificate | ✅ | ✅ | ❌ | ❌ | ❌ |
| Suspend Certificate | ✅ | ✅ | ❌ | ❌ | ❌ |
| Reactivate Certificate | ✅ | ✅ | ❌ | ❌ | ❌ |
| Renew Certificate | ✅ | ✅ | ✅ (assist) | ❌ | ✅ (own) |
| Download Certificate | ✅ | ✅ | ✅ | ❌ | ✅ (own) |
| View Certificate Details | ✅ | ✅ | ✅ (own) | ✅ | ✅ (own) |
| Export Certificate | ✅ | ✅ | ✅ (own) | ✅ | ✅ (own) |

### System Administration

| Operation | Admin | Officer | Operator | Auditor | End Entity |
|-----------|-------|---------|----------|---------|------------|
| Configure System | ✅ | ❌ | ❌ | ❌ | ❌ |
| Manage Users/Roles | ✅ | ❌ | ❌ | ❌ | ❌ |
| Create Templates | ✅ | ❌ | ❌ | ❌ | ❌ |
| Edit Templates | ✅ | ❌ | ❌ | ❌ | ❌ |
| Delete Templates | ✅ | ❌ | ❌ | ❌ | ❌ |
| Configure CA Integration | ✅ | ❌ | ❌ | ❌ | ❌ |
| Configure AD/LDAP | ✅ | ❌ | ❌ | ❌ | ❌ |
| Set Security Policies | ✅ | ❌ | ❌ | ❌ | ❌ |

### Auto-Enrollment

| Operation | Admin | Officer | Operator | Auditor | End Entity |
|-----------|-------|---------|----------|---------|------------|
| Enable/Disable Globally | ✅ | ❌ | ❌ | ❌ | ❌ |
| Configure Policies | ✅ | ❌ | ❌ | ❌ | ❌ |
| Trigger Bulk Enrollment | ✅ | ✅ | ❌ | ❌ | ❌ |
| View Enrollment Jobs | ✅ | ✅ | ❌ | ✅ | ❌ |
| Receive Auto-Enrolled Cert | ✅ | ✅ | ✅ | ❌ | ✅ |

### Audit & Reporting

| Operation | Admin | Officer | Operator | Auditor | End Entity |
|-----------|-------|---------|----------|---------|------------|
| View Audit Logs | ✅ | ✅ | ❌ | ✅ | ❌ |
| Export Audit Logs | ✅ | ✅ | ❌ | ✅ | ❌ |
| Generate Reports | ✅ | ✅ | ❌ | ✅ | ❌ |
| Schedule Reports | ✅ | ❌ | ❌ | ✅ | ❌ |
| View System Health | ✅ | ✅ | ❌ | ✅ | ❌ |
| Export Compliance Data | ✅ | ✅ | ❌ | ✅ | ❌ |

---

## 🔄 Authentication & Authorization Flow

### Authentication Flow

```
┌──────────────┐
│  End User    │
│  (Browser)   │
└──────┬───────┘
       │
       │ 1. Enter credentials
       │    (username + password)
       ▼
┌──────────────────────┐
│   RA Web Portal      │
│   Login Page         │
└──────┬───────────────┘
       │
       │ 2. Submit credentials
       ▼
┌──────────────────────┐
│  RA Application      │
│  Auth Controller     │
└──────┬───────────────┘
       │
       │ 3. LDAP bind authentication
       ▼
┌──────────────────────┐
│  Active Directory    │
│  (LDAP Server)       │
└──────┬───────────────┘
       │
       │ 4. Validate credentials
       │    ✓ Success / ✗ Failure
       ▼
┌──────────────────────┐
│  Retrieve User       │
│  Attributes & Groups │
│                      │
│  - displayName       │
│  - mail              │
│  - department        │
│  - memberOf          │
└──────┬───────────────┘
       │
       │ 5. Map AD groups to roles
       ▼
┌──────────────────────┐
│   Role Mapping       │
│                      │
│  PKI-RA-Admins       │
│    → RA Admin        │
│                      │
│  PKI-RA-Officers     │
│    → RA Officer      │
│                      │
│  PKI-RA-Operators    │
│    → RA Operator     │
│                      │
│  PKI-Auditors        │
│    → Auditor         │
│                      │
│  Domain Users        │
│    → End Entity      │
└──────┬───────────────┘
       │
       │ 6. Create session with roles
       ▼
┌──────────────────────┐
│   Generate JWT       │
│   Access Token       │
│                      │
│  {                   │
│   "sub": "user@corp" │
│   "roles": [         │
│     "RA_OFFICER"     │
│   ]                  │
│  }                   │
└──────┬───────────────┘
       │
       │ 7. Return token + redirect
       ▼
┌──────────────────────┐
│  Role-Based          │
│  Dashboard           │
│                      │
│  RA Officer sees:    │
│  - Pending Approvals │
│  - Certificate List  │
│  - Revocation Tools  │
└──────────────────────┘
```

### Authorization Flow (Per Request)

```
┌──────────────┐
│  User Action │
│  (e.g., Approve CSR) │
└──────┬───────┘
       │
       │ 1. HTTP Request with JWT
       ▼
┌──────────────────────┐
│  API Endpoint        │
│  /api/csr/approve    │
└──────┬───────────────┘
       │
       │ 2. Validate JWT token
       ▼
┌──────────────────────┐
│  JWT Verification    │
│  - Signature valid?  │
│  - Not expired?      │
│  - Extract roles     │
└──────┬───────────────┘
       │
       │ 3. Check authorization
       ▼
┌──────────────────────┐
│  @PreAuthorize       │
│  Annotation          │
│                      │
│  hasRole('RA_OFFICER')│
│     OR               │
│  hasRole('RA_ADMIN') │
└──────┬───────────────┘
       │
       ├─────────┐
       │         │
       ▼         ▼
   ✅ Authorized  ❌ Denied
       │         │
       │         │ 403 Forbidden
       │         │ "Insufficient privileges"
       │         └──────────────────┐
       │                            │
       │ 4. Execute operation        │
       ▼                            ▼
┌──────────────────────┐    ┌──────────────┐
│  Approve CSR         │    │  Error       │
│  - Validate CSR      │    │  Response    │
│  - Submit to CA      │    └──────────────┘
│  - Update status     │
│  - Audit log entry   │
│  - Send notification │
└──────┬───────────────┘
       │
       │ 5. Return success
       ▼
┌──────────────────────┐
│  200 OK              │
│  {                   │
│   "status": "SUCCESS"│
│   "cert_id": "..."   │
│  }                   │
└──────────────────────┘
```

---

## 📊 Role Assignment Examples

### Example 1: IT Security Manager → RA Administrator

```
AD User: john.admin@corp.local
AD Groups:
  - Domain Users
  - IT-Security
  - PKI-RA-Admins  ← Matches mapping

RA Application Role: RA Administrator

Permissions:
  ✅ Full system configuration
  ✅ User and role management
  ✅ Template management
  ✅ Policy configuration
  ✅ All certificate operations
```

### Example 2: Certificate Officer → RA Officer

```
AD User: alice.officer@corp.local
AD Groups:
  - Domain Users
  - PKI-RA-Officers  ← Matches mapping

RA Application Role: RA Officer

Permissions:
  ✅ Approve/reject CSR
  ✅ Revoke certificates
  ✅ View all requests
  ✅ Generate reports
  ❌ System configuration
  ❌ User management
```

### Example 3: Help Desk Staff → RA Operator

```
AD User: bob.helpdesk@corp.local
AD Groups:
  - Domain Users
  - HelpDesk-Team
  - PKI-RA-Operators  ← Matches mapping

RA Application Role: RA Operator

Permissions:
  ✅ Submit CSR for users
  ✅ Upload documents
  ✅ View own submissions
  ✅ Download certificates
  ❌ Approve/reject
  ❌ Revoke certificates
```

### Example 4: Compliance Officer → Auditor

```
AD User: carol.audit@corp.local
AD Groups:
  - Domain Users
  - Compliance-Team
  - PKI-Auditors  ← Matches mapping

RA Application Role: Auditor

Permissions:
  ✅ View all operations
  ✅ Access audit logs
  ✅ Generate compliance reports
  ✅ Export data
  ❌ No modifications
  ❌ No certificate operations
```

### Example 5: Regular Employee → End Entity

```
AD User: dave.employee@corp.local
AD Groups:
  - Domain Users  ← Default mapping

RA Application Role: End Entity

Permissions:
  ✅ Submit own CSR
  ✅ View own certificates
  ✅ Download own certificates
  ✅ Renew own certificates
  ❌ Submit for others
  ❌ View others' data
```

---

## 🔗 Database Schema for User Hierarchy

```sql
-- Users table (synced from AD)
CREATE TABLE users (
    user_id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL,
    display_name VARCHAR(255),
    department VARCHAR(255),
    ad_distinguished_name VARCHAR(500),
    is_active BOOLEAN DEFAULT TRUE,
    last_sync_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Roles table
CREATE TABLE roles (
    role_id SERIAL PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL UNIQUE,
    role_description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert default roles
INSERT INTO roles (role_name, role_description) VALUES
('RA_ADMIN', 'RA Administrator - Full system control'),
('RA_OFFICER', 'RA Officer - Approval authority'),
('RA_OPERATOR', 'RA Operator - Submission authority'),
('AUDITOR', 'Auditor - Read-only access'),
('END_ENTITY', 'End Entity - Self-service');

-- User-Role mapping
CREATE TABLE user_roles (
    user_role_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(user_id),
    role_id INT NOT NULL REFERENCES roles(role_id),
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    assigned_by BIGINT REFERENCES users(user_id),
    UNIQUE(user_id, role_id)
);

-- AD Group to Role mapping
CREATE TABLE ad_group_role_mapping (
    mapping_id SERIAL PRIMARY KEY,
    ad_group_name VARCHAR(255) NOT NULL UNIQUE,
    role_id INT NOT NULL REFERENCES roles(role_id),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert default AD group mappings
INSERT INTO ad_group_role_mapping (ad_group_name, role_id) VALUES
('PKI-RA-Admins', (SELECT role_id FROM roles WHERE role_name = 'RA_ADMIN')),
('PKI-RA-Officers', (SELECT role_id FROM roles WHERE role_name = 'RA_OFFICER')),
('PKI-RA-Operators', (SELECT role_id FROM roles WHERE role_name = 'RA_OPERATOR')),
('PKI-Auditors', (SELECT role_id FROM roles WHERE role_name = 'AUDITOR')),
('Domain Users', (SELECT role_id FROM roles WHERE role_name = 'END_ENTITY'));

-- Permissions table (for fine-grained control)
CREATE TABLE permissions (
    permission_id SERIAL PRIMARY KEY,
    permission_name VARCHAR(100) NOT NULL UNIQUE,
    permission_description TEXT,
    resource_type VARCHAR(50),
    action VARCHAR(50)
);

-- Role-Permission mapping
CREATE TABLE role_permissions (
    role_permission_id SERIAL PRIMARY KEY,
    role_id INT NOT NULL REFERENCES roles(role_id),
    permission_id INT NOT NULL REFERENCES permissions(permission_id),
    UNIQUE(role_id, permission_id)
);
```

---

## 📝 Summary

### User Hierarchy Levels:
1. **RA Administrator** - Full control, system configuration
2. **RA Officer** - Approval authority, certificate operations
3. **RA Operator** - Submission authority, limited operations
4. **Auditor** - Read-only access, compliance reporting
5. **End Entity** - Self-service, own certificates only

### Key Principles:
✅ **Least Privilege** - Users get minimum permissions needed
✅ **Separation of Duties** - No single user can complete sensitive operations alone
✅ **Role-Based Access** - Permissions based on AD group membership
✅ **Audit Trail** - All actions logged with user identity
✅ **Auto-Enrollment** - Administrators and Officers can trigger bulk operations

---

**Document Status**: ✅ Complete
**Last Updated**: 2026-01-16
**Version**: 1.0
