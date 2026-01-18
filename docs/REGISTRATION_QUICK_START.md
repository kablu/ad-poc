# RA User Registration - Quick Start Guide

## Version 2.0 | 2026-01-13

---

## Overview

This guide provides quick instructions for registering RA management users in a **standalone environment** (no Active Directory or HR system).

---

## Three Registration Methods

### 1. Bootstrap Super Admin (One-Time Only)

**When:** First-time system deployment
**Who:** System Administrator with server access
**Security Level:** Highest

**Steps:**
```bash
cd /opt/ra-application
java -jar ra-web.jar --bootstrap-admin

# Follow interactive prompts
# Save temporary password shown on screen
# Can only run once
```

---

### 2. Admin-Initiated Registration (Standard)

**When:** Regular user registration
**Who:** Existing RA Administrator
**Security Level:** High

**Steps:**
1. Login to RA Admin Portal
2. Navigate: **Users → Register New User**
3. Fill in:
   - Full Name
   - Email
   - Mobile Number
   - Username (auto-generated if blank)
   - Role(s): RA_ADMIN, RA_OFFICER, RA_OPERATOR, or AUDITOR
   - Verification Method (optional)
4. Click **Register User**
5. Credentials sent automatically via:
   - **Email**: Username + Activation link
   - **SMS**: Temporary password

---

### 3. Self-Registration (Optional)

**When:** External users need access
**Who:** Anyone with valid email
**Security Level:** Medium
**Requires:** Admin approval

**Steps:**
1. User visits: `https://ra.company.com/register`
2. Fills registration form
3. Verifies email with OTP
4. Admin reviews and approves
5. User receives credentials
6. User activates account

---

## User Activation Process

**All users must activate their accounts:**

1. **Receive Email**: Username + Activation link (valid 72 hours)
2. **Receive SMS**: Temporary password
3. **Click Activation Link**: Opens activation page
4. **Enter Temporary Password**: From SMS
5. **Create New Password**: Must meet requirements:
   - Minimum 12 characters
   - 1 uppercase, 1 lowercase, 1 digit, 1 special character
6. **Login**: Access RA portal with new credentials

---

## Credential Format

### Username
- Format: `firstname.lastname`
- Example: `saima.khan`
- Lowercase, letters/numbers/dot only

### Temporary Password
- Length: 16 characters
- Example: `Kx7$mPq2@Yn5#Zt9`
- Used once only, must change immediately

### Activation Link
- Valid: 72 hours
- Single-use only
- Format: `https://ra.company.com/activate?token=...`

---

## Security Features

✅ **Multi-Channel Delivery**
- Username via email
- Password via SMS
- Prevents single point of compromise

✅ **Mandatory Activation**
- All accounts start as PENDING_ACTIVATION
- Must activate within 72 hours
- Forced password change on first login

✅ **Account Protection**
- bcrypt password hashing (cost 12)
- 5 failed attempts = 30-minute lockout
- 10 failed attempts = permanent lock (admin unlock required)

✅ **Comprehensive Audit**
- Every action logged
- Immutable audit trail
- Full traceability

---

## Role Permissions

| Role | Description | Key Permissions |
|------|-------------|-----------------|
| **RA_ADMIN** | System Administrator | Full system access, user management, configuration |
| **RA_OFFICER** | Certificate Officer | Approve/reject requests, revoke certificates |
| **RA_OPERATOR** | Operator | Submit certificate requests, view own requests |
| **AUDITOR** | Auditor | Read-only access to audit logs and reports |

---

## Common Issues

### Bootstrap Fails
```
Error: "Bootstrap already completed"
→ Bootstrap runs only once. Use admin registration.
```

### Email Not Received
```
1. Check spam/junk folder
2. Verify email address
3. Check SMTP configuration
4. Admin can resend from user panel
```

### Activation Link Expired
```
1. Admin resends activation link
2. Or admin resets password
```

### Cannot Register Admin Users
```
Error: "Unauthorized"
→ Only RA_ADMIN or SUPER_ADMIN can register other admins
```

---

## API Endpoints (Quick Reference)

### Register User (Admin Only)
```http
POST /api/v1/admin/users/register
Authorization: Bearer <admin-jwt>
Content-Type: application/json

{
  "full_name": "Saima Khan",
  "email": "saima.khan@company.com",
  "mobile_number": "+923009876543",
  "username": "saima.khan",
  "roles": ["RA_OFFICER"],
  "verification_method": "IN_PERSON"
}
```

### Activate Account (Public)
```http
POST /api/v1/users/activate
Content-Type: application/json

{
  "activation_token": "base64-token",
  "temporary_password": "Temp@Pass123456",
  "new_password": "MyNewP@ssw0rd!",
  "confirm_password": "MyNewP@ssw0rd!"
}
```

---

## Best Practices

### ✓ DO
- Use multi-channel delivery (email + SMS)
- Verify identity for high-privilege roles
- Log all registration activities
- Use strong passwords (16+ characters)
- Set activation expiry (72 hours)
- Monitor suspicious patterns

### ✗ DON'T
- Store passwords in plain text
- Send username and password together
- Reuse activation tokens
- Skip email verification
- Allow unlimited attempts
- Grant admin role without verification

---

## Rollout Checklist

### Day 1: Bootstrap
- [ ] Deploy application to server
- [ ] Run bootstrap command
- [ ] Create super admin account
- [ ] Test email/SMS delivery
- [ ] Test super admin login
- [ ] Document credentials securely

### Week 1: Initial Users
- [ ] Train super admin on registration
- [ ] Register 2-3 RA Officers
- [ ] Register 2-3 RA Operators
- [ ] Register 1 Auditor
- [ ] Test activation flow
- [ ] Verify role permissions

### Week 2: Self-Registration (Optional)
- [ ] Enable self-registration portal
- [ ] Configure approval workflow
- [ ] Test email verification
- [ ] Implement CAPTCHA
- [ ] Monitor for abuse

### Week 3: Production
- [ ] Migrate to production
- [ ] Register all RA staff
- [ ] Provide user training
- [ ] Enable monitoring
- [ ] Conduct security audit

---

## Support

**For Issues:**
- Check troubleshooting section above
- Review full specification: `END_ENTITY_REGISTRATION_SPEC_v2.0.md`
- Contact: ra-admin@company.com

**Documentation:**
- Full Specification: `D:\ecc-dev\jdk-21-poc\ra-web\docs\END_ENTITY_REGISTRATION_SPEC_v2.0.md`
- Quick Start: `D:\ecc-dev\jdk-21-poc\ra-web\docs\REGISTRATION_QUICK_START.md`

---

**Document Version:** 2.0
**Last Updated:** 2026-01-13
**Related Documents:** END_ENTITY_REGISTRATION_SPEC_v2.0.md
