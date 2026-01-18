# End Entity Registration - Functional Specification Document

## Document Information
- **Document Version**: 2.0
- **Date**: 2026-01-13
- **Status**: Draft - Ready for Review
- **Classification**: Internal - Confidential
- **Target Audience**: RA System Developers, System Administrators, Security Teams
- **Purpose**: Define secure user registration process for RA management users (non-AD environment)

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Registration Approach](#2-registration-approach)
3. [Registration Architecture](#3-registration-architecture)
4. [Bootstrap Process - Initial Super Administrator Setup](#4-bootstrap-process---initial-super-administrator-setup)
5. [Admin-Initiated User Registration](#5-admin-initiated-user-registration-standard-process)
6. [Self-Registration with Admin Approval](#6-self-registration-with-admin-approval)
7. [Security Considerations](#7-security-considerations)
8. [Database Schema for Registration](#8-database-schema-for-registration)
9. [REST API Endpoints for Registration](#9-rest-api-endpoints-for-registration)
10. [Audit Requirements](#10-audit-requirements)
11. [Compliance and Best Practices](#11-compliance-and-best-practices)
12. [Testing Requirements](#12-testing-requirements)
13. [Rollout Plan](#13-rollout-plan)
14. [Summary of Registration Methods](#14-summary-of-registration-methods)
15. [Appendices](#15-appendices)

---

## 1. Executive Summary

This document specifies the secure registration process for **RA Management Users** (RA Administrators, Officers, Operators, and Auditors) in the Registration Authority (RA) Web Application. This is a standalone user registration system for organizations operating the RA system WITHOUT Active Directory or external HR systems.

**Key Principle**: RA management user registration must be highly secure, auditable, and prevent unauthorized access through multi-layered verification.

**Important Note**: This registration process is specifically for **RA management staff** who will operate the RA system. End entity users (certificate requesters) may use different authentication mechanisms as defined in the main system specification.

### Key Features

- **No External Dependencies**: Standalone user management without AD or HR integration
- **Bootstrap Security**: One-time super admin creation via server console
- **Multi-Channel Delivery**: Credentials split across email and SMS for security
- **Mandatory Activation**: All users must activate accounts and change passwords
- **Role-Based Access**: Granular role assignment (Admin, Officer, Operator, Auditor)
- **Comprehensive Audit**: Every action logged with full traceability

---

## 2. Registration Approach

### RA-Managed User Registration (Standalone)
The RA system maintains its own user database with a secure registration workflow designed for organizations without Active Directory or HR system integration.

**Target Users:**
- RA Administrators (highest privilege)
- RA Officers (approve/reject certificate requests)
- RA Operators (submit requests on behalf of end entities)
- Auditors (read-only compliance access)
- Initial Super Administrator (bootstrap account)

**Key Characteristics:**
- No external identity provider dependency
- Self-contained user management
- Secure credential generation and delivery
- Multi-factor identity verification
- Role-based registration approval workflow
- Comprehensive audit trail

---

## 3. Registration Architecture

```
┌─────────────────────────────────────────────────────────────┐
│              RA Management User Registration Flow           │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Step 1: Bootstrap Super Admin (One-Time Setup)            │
│  ──────────────────────────────────────────────────────────│
│  System initialization → Create first admin account →       │
│  Manual credential setup → Super admin activated            │
│                                                             │
│  Step 2: Admin-Initiated Registration                       │
│  ──────────────────────────────────────────────────────────│
│  Existing admin creates new user → Identity verification →  │
│  Secure credential generation → Multi-channel delivery →    │
│  User first login (mandatory password change)               │
│                                                             │
│  Step 3: Self-Registration with Admin Approval             │
│  ──────────────────────────────────────────────────────────│
│  User submits request → Email verification →                │
│  Admin reviews & approves → Credential generation →         │
│  Secure delivery → User activation                          │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 4. Bootstrap Process - Initial Super Administrator Setup

### 4.1 Overview
**One-time setup** to create the first administrator account when the RA system is deployed for the first time. This is a special process that runs only once and is secured through system-level access.

### 4.2 Prerequisites
- RA application deployed and database initialized
- System administrator has direct server access (console/SSH)
- Secure communication channels available (email, SMS gateway configured)

### 4.3 Step-by-Step Bootstrap Process

#### Step 1: Run Bootstrap Command (Server Console Access Required)

**Via Command Line Interface:**

```bash
# Navigate to RA application directory
cd /opt/ra-application

# Run bootstrap command
java -jar ra-web.jar --bootstrap-admin

# Interactive prompts:
Enter Super Admin Full Name: Kablu Ahmed
Enter Super Admin Email: kablu.admin@company.com
Enter Super Admin Mobile Number: +923001234567
Enter Super Admin Username (default: superadmin): kablu.admin

Generating secure credentials...
✓ Super admin account created successfully!

Username: kablu.admin
Temporary Password: [Generated securely - shown once only]

IMPORTANT: Write down this temporary password immediately.
Temporary Password: Tz9#Kp7$Xm4@Qr2!

Activation instructions have been sent to:
- Email: kablu.admin@company.com
- SMS: +9230012xxxxx

This is the ONLY time the password will be displayed.
Press Enter to continue...
```

**Security Notes:**
- Bootstrap command can only run when user count in database = 0
- After first admin created, bootstrap command is permanently disabled
- Requires direct server console access (cannot be triggered via API)
- All bootstrap activities are logged with server hostname and timestamp

#### Step 2: Super Admin First Login

After receiving credentials, the super admin:

1. Navigates to: `https://ra.company.com/login`
2. Enters username and temporary password
3. Forced to change password immediately
4. Sets new strong password (min 12 characters)
5. Gains access to admin dashboard

#### Step 3: Post-Bootstrap Dashboard

After password change, super admin sees dashboard with:
- System status overview
- User management options
- Quick action to register new RA staff
- Configuration access
- Audit log access

---

## 5. Admin-Initiated User Registration (Standard Process)

### 5.1 Overview
After super admin is created, they can register additional RA staff members (Officers, Operators, Auditors, or more Admins).

### 5.2 Prerequisites
- Existing administrator logged into RA system
- User information available (name, email, mobile, role)
- Identity verification method determined based on role

### 5.3 Step-by-Step Registration Process

#### Step 1: Administrator Navigates to User Registration

**Admin Portal Interface:**
```
┌─────────────────────────────────────────────────┐
│        RA Admin - Register New User             │
├─────────────────────────────────────────────────┤
│                                                 │
│  Personal Information                           │
│  ───────────────────────────────────            │
│  Full Name:      [_________________________]    │
│  Email:          [_________________________]    │
│  Mobile Number:  [+92_____________________]     │
│  Username:       [_________________________]    │
│                  (auto-generated if blank)      │
│                                                 │
│  Role Assignment (Select one or more)           │
│  ───────────────────────────────────            │
│  [ ] RA_ADMIN      - Full system access         │
│  [ ] RA_OFFICER    - Approve/reject certs       │
│  [ ] RA_OPERATOR   - Submit requests            │
│  [ ] AUDITOR       - Read-only audit access     │
│                                                 │
│  Identity Verification (Optional but Recommended)│
│  ───────────────────────────────────            │
│  Verification Method: [▼ Select Method]         │
│    - None (Low security)                        │
│    - Email verification                         │
│    - In-person verification                     │
│    - Document verification                      │
│                                                 │
│  Notes/Justification:                           │
│  [________________________________________]      │
│  [________________________________________]      │
│                                                 │
│  [Register User]  [Cancel]                      │
│                                                 │
└─────────────────────────────────────────────────┘
```

#### Step 2: Secure Credential Generation

**Username Generation:**
- Format: `firstname.lastname` (e.g., saima.khan)
- If duplicate exists, append number (saima.khan2)
- Lowercase, no special characters except dot
- Minimum 5 characters, maximum 30 characters

**Password Generation:**
- **Length**: 16 characters (exceeds minimum security requirements)
- **Complexity**:
  - Uppercase letters (A-Z): 4 characters
  - Lowercase letters (a-z): 4 characters
  - Numbers (0-9): 4 characters
  - Special characters (!@#$%^&*): 4 characters
- **Algorithm**: Cryptographically secure random number generator (CSRNG)
- **Entropy**: Minimum 77 bits of entropy
- **Example**: `Kx7$mPq2@Yn5#Zt9`

**Activation Token:**
- 32 bytes (256 bits) cryptographically random
- Base64 URL-safe encoding
- SHA-256 hash stored in database
- Single-use only (deleted after activation)
- 72-hour expiration

#### Step 3: Multi-Channel Credential Delivery

**Security Principle**: Split credentials across multiple channels to prevent single point of compromise.

**Channel 1: Email (Username + Activation Link)**
```
To: saima.khan@company.com
Subject: RA System Account Created - Activation Required
From: noreply@ra.company.com

Dear Saima Khan,

Your Registration Authority (RA) System account has been created!

Account Details:
Username: saima.khan
Role: RA Officer
Created by: Admin (kablu.admin)
Created on: 2026-01-13 15:30:00 UTC

To activate your account, please click the activation link below:
https://ra.company.com/activate?token=<activation-token>

This link is valid for 72 hours and will expire on 2026-01-16 15:30:00 UTC.

For security reasons, your temporary password has been sent separately
to your registered mobile number via SMS.

Login Instructions:
1. Click the activation link above
2. Enter the temporary password from SMS
3. Create your new secure password
4. Log in to the RA portal

If you did not expect this account creation, please contact the
RA Administrator immediately at ra-admin@company.com

Security Notice: Never share your username or password with anyone.

Best regards,
RA System Team
```

**Channel 2: SMS (Temporary Password)**
```
SMS to: +923009876543

RA System: Your temporary password is: Kx7$mPq2@Yn5#Zt9

Use this for FIRST LOGIN ONLY. You must change it immediately.

Valid for 72 hours.

Username sent via email. Do NOT share this password.
```

**Channel 3: Admin Confirmation (Always Sent)**
```
To: kablu.admin@company.com
Subject: User Registration Confirmation - Saima Khan

Dear Administrator,

A new RA user account has been successfully created:

User Details:
Name: Saima Khan
Email: saima.khan@company.com
Username: saima.khan
Role(s): RA_OFFICER
Status: PENDING_ACTIVATION

Credentials Delivered:
✓ Email sent to: saima.khan@company.com
✓ SMS sent to: +9230099xxxxx

Registration Details:
Created by: kablu.admin (You)
Created on: 2026-01-13 15:30:00 UTC
Verification Method: In-person verification
Notes: New RA Officer - verified government ID and employee card

The user has 72 hours to activate their account.

If this registration was NOT authorized, please:
1. Immediately disable the account in Admin Panel
2. Report the incident to security@company.com

View User Details: https://ra.company.com/admin/users/uuid-12345

Best regards,
RA System
```

#### Step 4: User Activation Process

**User clicks activation link in email:**

```
┌─────────────────────────────────────────────────┐
│         RA System - Account Activation          │
├─────────────────────────────────────────────────┤
│                                                 │
│  Welcome, Saima Khan!                           │
│                                                 │
│  Your account is ready for activation.          │
│                                                 │
│  Username: saima.khan                           │
│                                                 │
│  Step 1: Enter Temporary Password               │
│  ─────────────────────────────────────          │
│  [Enter password from SMS]                      │
│                                                 │
│  Step 2: Create New Password                    │
│  ─────────────────────────────────              │
│  New Password: [••••••••••]                     │
│  Confirm Password: [••••••••••]                 │
│                                                 │
│  Password Requirements:                         │
│  ✓ Minimum 12 characters                        │
│  ✓ At least 1 uppercase letter                  │
│  ✓ At least 1 lowercase letter                  │
│  ✓ At least 1 number                            │
│  ✓ At least 1 special character                 │
│  ✗ Cannot match temporary password              │
│  ✗ Cannot contain username or email             │
│                                                 │
│  [Activate Account]                             │
│                                                 │
└─────────────────────────────────────────────────┘
```

---

## 6. Self-Registration with Admin Approval

### 6.1 Overview
Optional feature allowing users to request RA accounts via self-service portal, subject to admin approval.

### 6.2 Process Flow

1. **User submits registration request** via public portal
2. **Email verification** required (OTP sent to email)
3. **Admin receives notification** of pending request
4. **Admin reviews** user details and justification
5. **Admin approves or rejects** request
6. **If approved**: Credentials generated and sent via email + SMS
7. **User activates** account within 72 hours

### 6.3 Security Considerations

- CAPTCHA required to prevent automated abuse
- Rate limiting: Maximum 5 requests per email per day
- Email domain validation (optional - restrict to company domains)
- Admin must provide justification for approval/rejection
- All self-registration attempts logged in audit trail

---

## 7. Security Considerations

### 7.1 Password Security

**Password Storage:**
- NEVER store passwords in plain text
- Use bcrypt with cost factor 12 (minimum)
- Salt is generated automatically by bcrypt

**Password Policy:**
```
Minimum Requirements:
- Length: 12 characters minimum
- Complexity:
  * At least 1 uppercase letter (A-Z)
  * At least 1 lowercase letter (a-z)
  * At least 1 digit (0-9)
  * At least 1 special character (!@#$%^&*_-+=)
- Cannot contain username or email
- Cannot match any of last 5 passwords (password history)
- Must be changed every 90 days
- Temporary passwords valid for 72 hours only
```

### 7.2 Credential Delivery Security

**Multi-Channel Delivery (Defense in Depth):**
- **Email**: Username + Activation link
- **SMS**: Temporary password only
- **Rationale**: Attacker must compromise both channels to gain access

**Transport Security:**
- All emails sent via encrypted SMTP (STARTTLS or SMTPS)
- SMS sent via secure SMS gateway (HTTPS API)
- Activation links use HTTPS only

**Token Security:**
- Activation tokens are cryptographically random (32 bytes)
- Stored as SHA-256 hash in database
- Single use only (deleted after activation)
- Time-limited (72 hours expiration)

### 7.3 Identity Verification Levels

| Registration Method | Verification Level | Suitable For |
|---------------------|-------------------|--------------|
| Bootstrap Admin | Highest (server access) | Initial setup |
| Admin Registration (In-Person) | High | RA Officers, Admins |
| Admin Registration (Email Only) | Medium | RA Operators |
| Self-Registration | Low-Medium | External auditors |

**Recommended Verification by Role:**

```
Role                Verification Required
────────────────────────────────────────────
RA_ADMIN            Face-to-face + ID document + Security clearance
RA_OFFICER          Face-to-face + ID document + Manager approval
RA_OPERATOR         Email verification + Admin approval
AUDITOR             Email verification + Compliance approval
```

### 7.4 Protection Against Attacks

**1. Brute Force Protection:**
- Account locked after 5 failed login attempts
- Lockout duration: 30 minutes
- Permanent lock after 10 failed attempts (admin unlock required)

**2. Account Enumeration Prevention:**
- Same error message for invalid username and invalid password
- Response time constant (no timing attacks)
- No indication whether username exists

**3. Activation Token Protection:**
- Rate limit activation attempts (5 per hour per IP)
- Token invalidated after 3 failed password attempts
- No token reuse (single activation only)

**4. Session Security:**
- Secure session cookies (Secure, HttpOnly, SameSite flags)
- Session timeout: 30 minutes of inactivity
- Force re-authentication for sensitive operations

---

## 8. Database Schema for Registration

### 8.1 Core Tables

```sql
-- Users table
CREATE TABLE users (
    user_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(30) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    full_name VARCHAR(200) NOT NULL,
    mobile_number VARCHAR(20) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL, -- ACTIVE, PENDING_ACTIVATION, DISABLED, LOCKED
    must_change_password BOOLEAN DEFAULT TRUE,
    password_expires_at TIMESTAMP,
    password_last_changed_at TIMESTAMP,
    account_created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by VARCHAR(255), -- User ID or SYSTEM_BOOTSTRAP
    registration_source VARCHAR(50), -- BOOTSTRAP, ADMIN_REGISTRATION, SELF_REGISTRATION
    verification_method VARCHAR(50), -- NONE, EMAIL, IN_PERSON, DOCUMENT
    is_bootstrap_admin BOOLEAN DEFAULT FALSE,
    last_login_at TIMESTAMP,
    failed_login_attempts INTEGER DEFAULT 0,
    locked_until TIMESTAMP,
    CONSTRAINT check_status CHECK (status IN ('ACTIVE', 'PENDING_ACTIVATION', 'DISABLED', 'LOCKED'))
);

-- User roles
CREATE TABLE user_roles (
    user_role_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    role_name VARCHAR(50) NOT NULL, -- RA_ADMIN, RA_OFFICER, RA_OPERATOR, AUDITOR
    assigned_at TIMESTAMP NOT NULL DEFAULT NOW(),
    assigned_by UUID REFERENCES users(user_id),
    CONSTRAINT unique_user_role UNIQUE (user_id, role_name)
);

-- Activation tokens
CREATE TABLE activation_tokens (
    token_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL, -- SHA-256 hash
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    used_at TIMESTAMP,
    delivery_method VARCHAR(50), -- EMAIL_SMS, EMAIL_ONLY, IN_PERSON
    CONSTRAINT unique_active_token UNIQUE (user_id)
        WHERE used_at IS NULL
);

-- Password history (prevent reuse)
CREATE TABLE password_history (
    history_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    password_hash VARCHAR(255) NOT NULL,
    changed_at TIMESTAMP NOT NULL DEFAULT NOW(),
    changed_by VARCHAR(50), -- SELF, ADMIN_RESET, SYSTEM
    changed_ip_address INET
);

-- Failed login attempts
CREATE TABLE failed_login_attempts (
    attempt_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(255) NOT NULL,
    attempted_at TIMESTAMP NOT NULL DEFAULT NOW(),
    ip_address INET,
    user_agent TEXT,
    failure_reason VARCHAR(100)
);

CREATE INDEX idx_failed_attempts_username ON failed_login_attempts(username, attempted_at);

-- Account locks
CREATE TABLE account_locks (
    lock_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    locked_at TIMESTAMP NOT NULL DEFAULT NOW(),
    locked_until TIMESTAMP NOT NULL,
    lock_reason VARCHAR(100), -- TOO_MANY_ATTEMPTS, ADMIN_LOCK, SECURITY_INCIDENT
    locked_by UUID REFERENCES users(user_id), -- NULL if automatic
    unlocked_at TIMESTAMP,
    unlocked_by UUID REFERENCES users(user_id)
);

-- Registration requests (for self-registration)
CREATE TABLE registration_requests (
    request_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL,
    full_name VARCHAR(200) NOT NULL,
    mobile_number VARCHAR(20),
    reason TEXT,
    status VARCHAR(50) NOT NULL, -- PENDING, APPROVED, REJECTED
    email_verified BOOLEAN DEFAULT FALSE,
    email_verification_code VARCHAR(10),
    email_verification_expires_at TIMESTAMP,
    submitted_at TIMESTAMP NOT NULL DEFAULT NOW(),
    reviewed_by_admin_id UUID REFERENCES users(user_id),
    reviewed_at TIMESTAMP,
    rejection_reason TEXT,
    created_ip_address INET,
    CONSTRAINT unique_email_pending UNIQUE (email, status)
);

-- System configuration
CREATE TABLE system_config (
    config_key VARCHAR(100) PRIMARY KEY,
    config_value TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP
);

-- Audit logs
CREATE TABLE audit_logs (
    log_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    timestamp TIMESTAMP NOT NULL DEFAULT NOW(),
    action VARCHAR(100) NOT NULL,
    resource_type VARCHAR(50),
    resource_id VARCHAR(255),
    performed_by VARCHAR(255),
    ip_address INET,
    user_agent TEXT,
    result VARCHAR(50), -- SUCCESS, FAILURE
    failure_reason TEXT,
    details JSONB
);

CREATE INDEX idx_audit_timestamp ON audit_logs(timestamp);
CREATE INDEX idx_audit_action ON audit_logs(action);
CREATE INDEX idx_audit_performed_by ON audit_logs(performed_by);
```

---

## 9. REST API Endpoints for Registration

### 9.1 Admin Registration Endpoints (Requires RA_ADMIN role)

```http
POST /api/v1/admin/users/register
Authorization: Bearer <admin-jwt-token>
Content-Type: application/json

Request Body:
{
  "full_name": "Saima Khan",
  "email": "saima.khan@company.com",
  "mobile_number": "+923009876543",
  "username": "saima.khan",
  "roles": ["RA_OFFICER"],
  "verification_method": "IN_PERSON",
  "admin_notes": "New RA Officer - verified government ID"
}

Response: 201 Created
{
  "user_id": "uuid-67890",
  "username": "saima.khan",
  "status": "PENDING_ACTIVATION",
  "activation_expires_at": "2026-01-16T15:30:00Z",
  "credentials_sent_to": {
    "email": "saima.khan@company.com",
    "mobile": "+9230099xxxxx"
  }
}
```

### 9.2 Self-Registration Endpoints (Public - No Auth)

```http
POST /api/v1/public/registration/verify-email
Content-Type: application/json

Request Body:
{
  "email": "user@company.com"
}

Response: 200 OK
{
  "message": "Verification code sent to email",
  "code_expires_in_minutes": 10
}
```

```http
POST /api/v1/public/registration/request
Content-Type: application/json

Request Body:
{
  "full_name": "John Doe",
  "email": "john.doe@company.com",
  "mobile_number": "+923001111111",
  "reason": "Need access for audit purposes",
  "email_verification_code": "847293"
}

Response: 201 Created
{
  "request_id": "REQ-2026-001",
  "status": "PENDING_APPROVAL",
  "message": "Registration request submitted. You will receive an email once reviewed."
}
```

### 9.3 User Activation Endpoint (Public - Token Auth)

```http
POST /api/v1/users/activate
Content-Type: application/json

Request Body:
{
  "activation_token": "base64-encoded-token",
  "temporary_password": "Kx7$mPq2@Yn5#Zt9",
  "new_password": "MyNewSecureP@ss2026!",
  "confirm_password": "MyNewSecureP@ss2026!"
}

Response: 200 OK
{
  "message": "Account activated successfully",
  "username": "saima.khan",
  "password_expires_at": "2026-04-15T00:00:00Z",
  "login_url": "https://ra.company.com/login"
}
```

---

## 10. Audit Requirements

Every registration action must be logged:

```json
{
  "event_id": "evt-abc-123",
  "timestamp": "2026-01-13T15:30:00Z",
  "action": "USER_REGISTERED",
  "resource_type": "USER",
  "resource_id": "uuid-67890",
  "performed_by": "uuid-admin-456",
  "registration_method": "ADMIN_REGISTRATION",
  "user_details": {
    "username": "saima.khan",
    "email": "saima.khan@company.com",
    "roles": ["RA_OFFICER"]
  },
  "credentials_delivered_via": ["EMAIL", "SMS"],
  "verification_method": "IN_PERSON",
  "ip_address": "192.168.1.100",
  "result": "SUCCESS"
}
```

**Key Audit Events:**
- `BOOTSTRAP_ADMIN_CREATED` - Super admin created via bootstrap
- `USER_REGISTERED` - New user registered
- `USER_ACTIVATED` - User activated their account
- `PASSWORD_CHANGED` - User changed password
- `ACCOUNT_LOCKED` - Account locked due to failed attempts
- `ACCOUNT_UNLOCKED` - Account unlocked by admin
- `REGISTRATION_REQUEST_SUBMITTED` - Self-registration request submitted
- `REGISTRATION_REQUEST_APPROVED` - Admin approved registration
- `REGISTRATION_REQUEST_REJECTED` - Admin rejected registration

---

## 11. Compliance and Best Practices

### 11.1 GDPR Compliance
- User consent for data processing
- Right to access data (user can view their registration details)
- Right to erasure (account deletion with audit trail retention)
- Data minimization (collect only necessary information)

### 11.2 NIST Guidelines
- Follow NIST SP 800-63B for password requirements
- Multi-factor authentication recommended (not mandatory for registration)
- Identity proofing levels aligned with NIST 800-63A

### 11.3 Security Best Practices
- Principle of least privilege (users get minimum necessary role)
- Separation of duties (admin cannot activate their own account)
- Defense in depth (multiple security layers)
- Zero trust (verify every registration request)

---

## 12. Testing Requirements

### 12.1 Functional Tests
- [ ] Bootstrap creates super admin successfully
- [ ] Bootstrap fails if users already exist
- [ ] Admin can register new users
- [ ] Self-registration requires approval
- [ ] Email verification works correctly
- [ ] SMS delivery successful
- [ ] Activation link works within 72 hours
- [ ] Activation link expires after 72 hours
- [ ] Temporary password works for first login
- [ ] Mandatory password change enforced
- [ ] Duplicate registrations prevented

### 12.2 Security Tests
- [ ] Passwords hashed with bcrypt (cost 12)
- [ ] Activation tokens single-use only
- [ ] Account locked after 5 failed attempts
- [ ] No password visible in logs or database
- [ ] No account enumeration possible
- [ ] Token timing attack prevention
- [ ] Rate limiting on activation attempts
- [ ] HTTPS enforced for all endpoints

### 12.3 Integration Tests
- [ ] Email delivery successful (SMTP)
- [ ] SMS delivery successful (SMS gateway)
- [ ] Database transactions properly handled
- [ ] Audit logs correctly generated

---

## 13. Rollout Plan

### Phase 1: Bootstrap Super Admin (Day 1)
- Deploy RA application to server
- Run bootstrap command via server console
- Create first super administrator account
- Verify email and SMS delivery working
- Test super admin login and password change
- Document super admin credentials securely

### Phase 2: Admin-Initiated Registration (Week 1)
- Train super admin on user registration process
- Register initial RA Officers (2-3 users)
- Register initial RA Operators (2-3 users)
- Register initial Auditor (1 user)
- Test activation flow with real users
- Verify role-based access control working

### Phase 3: Self-Registration Portal (Week 2 - Optional)
- Enable public self-registration portal
- Configure admin approval workflow
- Test email verification process
- Monitor for abuse/spam registrations
- Implement CAPTCHA if needed

### Phase 4: Production Rollout (Week 3)
- Migrate to production environment
- Register all RA management staff
- Provide training to all registered users
- Enable 24/7 monitoring and support
- Conduct security audit

---

## 14. Summary of Registration Methods

### 14.1 Comparison Matrix

| Feature | Bootstrap Admin | Admin-Initiated | Self-Registration |
|---------|----------------|-----------------|-------------------|
| **When to Use** | First-time setup only | Standard registration | Optional - external users |
| **Who Initiates** | System Administrator | Existing Admin | User themselves |
| **Access Required** | Server console access | Admin portal access | Public portal |
| **Identity Verification** | None (server access = trust) | Optional (recommended) | Email verification + Admin approval |
| **Credential Delivery** | Console + Email + SMS | Email + SMS | Email + SMS (after approval) |
| **Activation Required** | Yes (password change) | Yes (password change) | Yes (password change) |
| **Approval Workflow** | None | None | Admin approval required |
| **Security Level** | Highest (server access) | High | Medium |
| **Use Frequency** | Once only | Regular | As needed |

### 14.2 Quick Reference Guide

**For System Administrators:**
```bash
# Initial deployment - Create first admin
cd /opt/ra-application
java -jar ra-web.jar --bootstrap-admin

# Follow prompts, save credentials securely
# Bootstrap can only run once
```

**For RA Administrators:**
```
1. Log into RA Admin Portal
2. Navigate to: Users → Register New User
3. Fill in user details (name, email, mobile, username)
4. Select role(s): RA_ADMIN, RA_OFFICER, RA_OPERATOR, or AUDITOR
5. Optional: Verify identity in person
6. Click "Register User"
7. Credentials automatically sent via email and SMS
```

**For New Users:**
```
1. Receive email with username and activation link
2. Receive SMS with temporary password
3. Click activation link (valid 72 hours)
4. Enter temporary password
5. Create new strong password
6. Log in to RA portal
```

### 14.3 Security Best Practices Summary

**✓ DO:**
- Always use multi-channel credential delivery (email + SMS)
- Enforce mandatory password change on first login
- Use strong passwords (minimum 16 characters for temporary passwords)
- Store passwords as bcrypt hashes with cost factor 12
- Verify identity in person for high-privilege roles (RA_ADMIN, RA_OFFICER)
- Set activation token expiry (72 hours recommended)
- Log all registration activities to audit trail
- Send confirmation emails to admins after registration
- Disable bootstrap command after first use
- Use HTTPS for all communication
- Implement rate limiting on registration endpoints
- Monitor for suspicious registration patterns

**✗ DON'T:**
- Never store passwords in plain text
- Never send username and password in same channel
- Never reuse activation tokens
- Never allow bootstrap after initial setup
- Never skip audit logging
- Never use weak passwords (< 12 characters)
- Never allow unlimited registration attempts
- Never skip email verification for self-registration
- Never grant admin role without proper verification
- Never share temporary passwords via insecure channels

### 14.4 Common Issues and Troubleshooting

**Issue: Bootstrap command fails**
```
Error: "Bootstrap already completed"
Solution: Bootstrap can only run once. Use admin-initiated registration instead.

Error: "Email/SMS delivery failed"
Solution: Check email/SMS gateway configuration in application.properties
```

**Issue: User not receiving activation email**
```
Troubleshooting steps:
1. Check spam/junk folder
2. Verify email address is correct
3. Check email service logs
4. Resend activation email from admin portal
5. Verify SMTP settings in configuration
```

**Issue: Activation link expired**
```
Solution:
1. Admin can resend activation link from user management panel
2. Or admin can reset user's password and generate new activation
```

**Issue: User cannot register other admins**
```
Error: "Unauthorized"
Solution: Only users with RA_ADMIN or SUPER_ADMIN role can register other administrators.
This is a security feature to prevent privilege escalation.
```

### 14.5 Credential Format Standards

**Username Format:**
- Pattern: `firstname.lastname` or custom
- Length: 5-30 characters
- Allowed characters: lowercase letters, numbers, dot (.), underscore (_)
- Examples: `kablu.ahmed`, `saima.khan`, `admin.user`

**Temporary Password Format:**
- Length: 16 characters minimum
- Composition: 4 uppercase + 4 lowercase + 4 digits + 4 special characters
- Special characters: `!@#$%^&*`
- Example: `Kx7$mPq2@Yn5#Zt9`

**Activation Token Format:**
- Length: 32 bytes (256 bits)
- Encoding: Base64 URL-safe
- Storage: SHA-256 hash in database
- Single-use: Deleted after successful activation
- Expiry: 72 hours from creation

### 14.6 Database Tables Quick Reference

**Essential Tables:**
```sql
users                    -- Core user accounts
user_roles               -- Role assignments
activation_tokens        -- Activation tokens (temporary)
password_history         -- Password history (prevent reuse)
failed_login_attempts    -- Track failed logins
account_locks            -- Account lockout records
registration_requests    -- Self-registration requests (pending approval)
audit_logs               -- Comprehensive audit trail
system_config            -- System configuration (bootstrap_completed flag)
```

---

## 15. Appendices

### Appendix A: Sample Email Templates
See email samples in Sections 4 and 5.

### Appendix B: SMS Message Templates
See SMS samples in Sections 4 and 5.

### Appendix C: Password Policy Configuration
See Section 7.1 for complete password policy.

### Appendix D: Error Messages and User Guidance
See Section 14.4 for troubleshooting guide.

### Appendix E: Admin Training Guide
To be developed during implementation phase.

---

**Document End**

**Approval:**
- [ ] IT Security Manager
- [ ] PKI Administrator
- [ ] Compliance Officer
- [ ] Development Team Lead

**Document Control:**
- Next Review Date: 2026-04-13
- Version History:
  - v1.0 (2026-01-13): Initial draft with HR system integration
  - v2.0 (2026-01-13): Updated for standalone RA user registration (no HR/AD dependency)
    - Removed HR system integration
    - Added bootstrap super admin process
    - Updated for open-ended RA management user registration
    - Added comprehensive summary and troubleshooting sections

---

**For Questions or Clarifications:**
Contact: RA Development Team
Email: ra-dev@company.com

**Document Location:**
D:\ecc-dev\jdk-21-poc\ra-web\docs\END_ENTITY_REGISTRATION_SPEC_v2.0.md
