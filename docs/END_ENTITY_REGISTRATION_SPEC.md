# End Entity Registration - Functional Specification Document

## Document Information
- **Document Version**: 2.0
- **Date**: 2026-01-13
- **Status**: Draft
- **Classification**: Internal - Confidential
- **Target Audience**: RA System Developers, System Administrators, Security Teams
- **Purpose**: Define secure user registration process for RA management users (non-AD environment)

---

## 1. Executive Summary

This document specifies the secure registration process for **RA Management Users** (RA Administrators, Officers, Operators, and Auditors) in the Registration Authority (RA) Web Application. This is a standalone user registration system for organizations operating the RA system WITHOUT Active Directory or external HR systems.

**Key Principle**: RA management user registration must be highly secure, auditable, and prevent unauthorized access through multi-layered verification.

**Important Note**: This registration process is specifically for **RA management staff** who will operate the RA system. End entity users (certificate requesters) may use different authentication mechanisms as defined in the main system specification.

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

#### Step 2: Bootstrap Implementation Code

```java
@Component
public class BootstrapAdminService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CredentialGenerationService credentialService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private SMSService smsService;

    public BootstrapResult createSuperAdmin(BootstrapRequest request) {

        // 1. Check if bootstrap is allowed (database must be empty)
        long userCount = userRepository.count();
        if (userCount > 0) {
            throw new BootstrapNotAllowedException(
                "Bootstrap already completed. Database contains " + userCount + " users.");
        }

        // 2. Validate input
        validateBootstrapRequest(request);

        // 3. Generate secure credentials
        String username = request.getUsername() != null
            ? request.getUsername()
            : "superadmin";

        String temporaryPassword = credentialService.generateStrongPassword();
        String passwordHash = BCrypt.hashpw(temporaryPassword, BCrypt.gensalt(12));

        // 4. Create super admin user
        User superAdmin = User.builder()
            .userId(UUID.randomUUID().toString())
            .username(username)
            .email(request.getEmail())
            .fullName(request.getFullName())
            .mobileNumber(request.getMobileNumber())
            .passwordHash(passwordHash)
            .status(UserStatus.ACTIVE) // Bootstrap admin is immediately active
            .mustChangePassword(true) // Must change on first login
            .passwordExpiresAt(Instant.now().plus(90, ChronoUnit.DAYS))
            .accountCreatedAt(Instant.now())
            .createdBy("SYSTEM_BOOTSTRAP")
            .isBootstrapAdmin(true) // Special flag
            .build();

        userRepository.save(superAdmin);

        // 5. Assign SUPER_ADMIN role
        assignSuperAdminRole(superAdmin);

        // 6. Send credentials via email
        emailService.sendBootstrapAdminEmail(superAdmin.getEmail(),
            username, temporaryPassword);

        // 7. Send SMS notification
        smsService.sendSMS(superAdmin.getMobileNumber(),
            "RA System: Your super admin account has been created. " +
            "Username: " + username + ". Check email for password.");

        // 8. Audit log
        auditLog.log("BOOTSTRAP_ADMIN_CREATED", username,
            "Super admin account created via bootstrap",
            "server_hostname=" + getHostname());

        // 9. Disable bootstrap functionality permanently
        setBootstrapCompleted();

        return BootstrapResult.builder()
            .success(true)
            .username(username)
            .temporaryPassword(temporaryPassword) // Return once for console display
            .email(superAdmin.getEmail())
            .message("Super admin created successfully")
            .build();
    }

    private void validateBootstrapRequest(BootstrapRequest request) {
        if (request.getEmail() == null || !isValidEmail(request.getEmail())) {
            throw new ValidationException("Valid email required");
        }
        if (request.getMobileNumber() == null ||
            !isValidMobileNumber(request.getMobileNumber())) {
            throw new ValidationException("Valid mobile number required");
        }
        if (request.getFullName() == null || request.getFullName().trim().isEmpty()) {
            throw new ValidationException("Full name required");
        }
    }

    private void setBootstrapCompleted() {
        // Create a permanent flag in database to prevent future bootstrap
        SystemConfig config = new SystemConfig();
        config.setKey("bootstrap_completed");
        config.setValue("true");
        config.setCreatedAt(Instant.now());
        systemConfigRepository.save(config);
    }
}
```

#### Step 3: Super Admin First Login

**Super admin navigates to RA portal:**

```
https://ra.company.com/login
```

**Login Screen:**
```
┌─────────────────────────────────────────────────┐
│          RA System - Administrator Login        │
├─────────────────────────────────────────────────┤
│                                                 │
│  Username: [kablu.admin____________]            │
│  Password: [••••••••••••••••••••]               │
│                                                 │
│  [ ] Remember me                                │
│                                                 │
│  [Login]                                        │
│                                                 │
│  Forgot password? | Need help?                  │
│                                                 │
└─────────────────────────────────────────────────┘
```

**After login, forced password change:**

```
┌─────────────────────────────────────────────────┐
│         Change Password (Required)              │
├─────────────────────────────────────────────────┤
│                                                 │
│  Welcome, Kablu Ahmed!                          │
│                                                 │
│  For security, you must change your temporary   │
│  password before accessing the system.          │
│                                                 │
│  Current Password: [••••••••••••••]             │
│  New Password:     [••••••••••••••]             │
│  Confirm Password: [••••••••••••••]             │
│                                                 │
│  Password Requirements:                         │
│  ✓ Minimum 12 characters                        │
│  ✓ At least 1 uppercase letter                  │
│  ✓ At least 1 lowercase letter                  │
│  ✓ At least 1 number                            │
│  ✓ At least 1 special character                 │
│  ✗ Cannot match temporary password              │
│                                                 │
│  [Change Password & Continue]                   │
│                                                 │
└─────────────────────────────────────────────────┘
```

#### Step 4: Post-Bootstrap Super Admin Dashboard

After password change, super admin sees dashboard:

```
┌─────────────────────────────────────────────────┐
│     RA Admin Dashboard - Welcome Kablu Ahmed    │
├─────────────────────────────────────────────────┤
│                                                 │
│  System Status: ✓ Operational                   │
│  Your Role: SUPER_ADMIN                         │
│                                                 │
│  Quick Actions:                                 │
│  ───────────────────────────────────            │
│  ➤ Register New RA Staff Member                 │
│  ➤ Configure System Settings                    │
│  ➤ View Audit Logs                              │
│  ➤ Enable Self-Registration (Optional)          │
│                                                 │
│  System Statistics:                             │
│  ───────────────────────────────────            │
│  Total Users: 1 (You)                           │
│  Active Certificates: 0                         │
│  Pending Requests: 0                            │
│                                                 │
│  Next Steps:                                    │
│  ───────────────────────────────────            │
│  1. Configure email/SMS gateway settings        │
│  2. Register RA Officers and Operators          │
│  3. Configure certificate templates             │
│  4. Review security policies                    │
│                                                 │
└─────────────────────────────────────────────────┘
```

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

#### Step 2: Admin Submits Registration Request

```http
POST /api/v1/admin/users/register
Authorization: Bearer <admin-jwt-token>
Content-Type: application/json

{
  "full_name": "Saima Khan",
  "email": "saima.khan@company.com",
  "mobile_number": "+923009876543",
  "username": "saima.khan",
  "roles": ["RA_OFFICER"],
  "verification_method": "IN_PERSON",
  "admin_notes": "New RA Officer - verified government ID and employee card"
}
```

#### Step 3: System Validation and User Creation

```java
@Service
public class UserRegistrationService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CredentialGenerationService credentialService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private SMSService smsService;

    @Autowired
    private AuditService auditLog;

    public RegistrationResult registerUser(RegistrationRequest request,
                                          String adminUserId) {

        // 1. Verify admin has permission to register users
        validateAdminPermission(adminUserId, request.getRoles());

        // 2. Duplicate check
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateUserException("Email already registered");
        }

        if (request.getUsername() != null &&
            userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateUserException("Username already taken");
        }

        // 3. Validate input
        validateRegistrationRequest(request);

        // 4. Generate secure credentials
        UserCredentials credentials = credentialService
            .generateSecureCredentials(request);

        // 5. Create user account (PENDING_ACTIVATION status)
        User user = createUserAccount(request, credentials, adminUserId);

        // 6. Send credentials securely via multiple channels
        deliverCredentialsSecurely(user, credentials);

        // 7. Audit log
        auditLog.log("USER_REGISTERED", request.getEmail(),
            "Registration initiated by admin: " + adminUserId,
            Map.of("roles", request.getRoles(),
                   "verification_method", request.getVerificationMethod()));

        return RegistrationResult.builder()
            .success(true)
            .userId(user.getUserId())
            .username(credentials.getUsername())
            .message("User registered. Credentials sent to email and mobile.")
            .build();
    }

    private User createUserAccount(RegistrationRequest request,
                                  UserCredentials credentials,
                                  String adminUserId) {
        User user = User.builder()
            .userId(UUID.randomUUID().toString())
            .username(credentials.getUsername())
            .email(request.getEmail())
            .fullName(request.getFullName())
            .mobileNumber(request.getMobileNumber())
            .passwordHash(credentials.getPasswordHash())
            .status(UserStatus.PENDING_ACTIVATION)
            .mustChangePassword(true)
            .passwordExpiresAt(Instant.now().plus(72, ChronoUnit.HOURS))
            .accountCreatedAt(Instant.now())
            .createdBy(adminUserId)
            .registrationSource("ADMIN_REGISTRATION")
            .build();

        userRepository.save(user);

        // Assign roles
        assignRoles(user.getUserId(), request.getRoles());

        return user;
    }

    private void validateAdminPermission(String adminUserId, List<String> roles) {
        User admin = userRepository.findById(adminUserId)
            .orElseThrow(() -> new UnauthorizedException());

        // Only admins can register other admins
        if (roles.contains("RA_ADMIN") &&
            !admin.hasRole("RA_ADMIN") && !admin.hasRole("SUPER_ADMIN")) {
            throw new UnauthorizedException(
                "Only administrators can register other administrators");
        }
    }
}
```

#### Step 4: Secure Credential Generation

**Username Generation:**
- Format: `first_name.last_name` (e.g., kablu.ahmed)
- If duplicate exists, append number (kablu.ahmed2)
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

```java
@Service
public class CredentialGenerationService {

    private final SecureRandom secureRandom = new SecureRandom();

    public UserCredentials generateSecureCredentials(RegistrationRequest request) {
        // Generate username
        String username = generateUsername(request.getFirstName(),
            request.getLastName());

        // Generate strong password
        String temporaryPassword = generateStrongPassword();

        // Hash password (bcrypt with cost factor 12)
        String passwordHash = BCrypt.hashpw(temporaryPassword,
            BCrypt.gensalt(12));

        // Generate one-time activation token (valid for 72 hours)
        String activationToken = generateActivationToken();

        return UserCredentials.builder()
            .username(username)
            .temporaryPassword(temporaryPassword) // For delivery only
            .passwordHash(passwordHash) // Store in database
            .activationToken(activationToken)
            .expiresAt(Instant.now().plus(72, ChronoUnit.HOURS))
            .mustChangePassword(true)
            .build();
    }

    private String generateStrongPassword() {
        String uppercase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowercase = "abcdefghijklmnopqrstuvwxyz";
        String numbers = "0123456789";
        String special = "!@#$%^&*";

        StringBuilder password = new StringBuilder();

        // Ensure complexity requirements
        password.append(randomChar(uppercase, 4));
        password.append(randomChar(lowercase, 4));
        password.append(randomChar(numbers, 4));
        password.append(randomChar(special, 4));

        // Shuffle characters
        return shuffleString(password.toString());
    }

    private String generateActivationToken() {
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding()
            .encodeToString(tokenBytes);
    }
}
```

#### Step 5: Secure Credential Delivery (Multi-Channel Approach)

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

#### Step 6: User Account Creation in Database

```sql
-- Users table record
INSERT INTO users (
    user_id,
    username,
    email,
    full_name,
    mobile_number,
    password_hash,
    status,
    must_change_password,
    password_expires_at,
    account_created_at,
    created_by,
    registration_source,
    verification_method
) VALUES (
    'uuid-67890',
    'saima.khan@company.com',
    'Saima Khan',
    '+923009876543',
    '$2a$12$...', -- bcrypt hash
    'PENDING_ACTIVATION',
    TRUE,
    NOW() + INTERVAL '72 hours',
    NOW(),
    'uuid-admin-456', -- kablu.admin user ID
    'ADMIN_REGISTRATION',
    'IN_PERSON'
);

-- Role assignments
INSERT INTO user_roles (
    user_role_id,
    user_id,
    role_name,
    assigned_at,
    assigned_by
) VALUES (
    'uuid-role-1',
    'uuid-67890',
    'RA_OFFICER',
    NOW(),
    'uuid-admin-456'
);

-- Activation token record
INSERT INTO activation_tokens (
    token_id,
    user_id,
    token_hash,
    expires_at,
    created_at,
    used_at,
    delivery_method
) VALUES (
    'uuid-token-789',
    'uuid-67890',
    'sha256-hash-of-token',
    NOW() + INTERVAL '72 hours',
    NOW(),
    NULL, -- Not yet used
    'EMAIL_SMS'
);

-- Audit log
INSERT INTO audit_logs (
    log_id,
    timestamp,
    action,
    resource_type,
    resource_id,
    performed_by,
    ip_address,
    result,
    details
) VALUES (
    'uuid-audit-1',
    NOW(),
    'USER_REGISTERED',
    'USER',
    'uuid-67890',
    'uuid-admin-456', -- Admin who registered the user
    '192.168.1.100',
    'SUCCESS',
    '{"username": "saima.khan", "roles": ["RA_OFFICER"], "method": "ADMIN_REGISTRATION", "verification": "IN_PERSON"}'::jsonb
);
```

#### Step 6: User Activation Process

**User clicks activation link in email:**

```http
GET /activate?token=<activation-token>
```

**Activation Page Flow:**

```
┌─────────────────────────────────────────────────┐
│         RA System - Account Activation          │
├─────────────────────────────────────────────────┤
│                                                 │
│  Welcome, Kablu Ahmed!                          │
│                                                 │
│  Your account is ready for activation.          │
│                                                 │
│  Username: kablu.ahmed                          │
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

**Activation Request:**

```http
POST /api/v1/users/activate
Content-Type: application/json

{
  "activation_token": "base64-token-from-url",
  "temporary_password": "Kx7$mPq2@Yn5#Zt9",
  "new_password": "MyNewSecureP@ss2026!",
  "confirm_password": "MyNewSecureP@ss2026!"
}
```

**Server-Side Activation Logic:**

```java
@Service
public class AccountActivationService {

    public ActivationResult activateAccount(ActivationRequest request) {
        // 1. Validate activation token
        ActivationToken token = tokenRepository.findByToken(
            hashToken(request.getActivationToken()));

        if (token == null || token.isExpired()) {
            auditLog.log("ACTIVATION_FAILED", "Token expired or invalid");
            throw new InvalidTokenException("Activation token is invalid or expired");
        }

        // 2. Retrieve user
        User user = userRepository.findById(token.getUserId())
            .orElseThrow(() -> new UserNotFoundException());

        // 3. Verify temporary password
        if (!BCrypt.checkpw(request.getTemporaryPassword(),
            user.getPasswordHash())) {
            auditLog.log("ACTIVATION_FAILED", user.getUsername(),
                "Invalid temporary password");
            throw new InvalidCredentialsException();
        }

        // 4. Validate new password
        PasswordValidationResult validation =
            passwordValidator.validate(request.getNewPassword(), user);

        if (!validation.isValid()) {
            return ActivationResult.failed(validation.getErrors());
        }

        // 5. Update user account
        String newPasswordHash = BCrypt.hashpw(request.getNewPassword(),
            BCrypt.gensalt(12));

        user.setPasswordHash(newPasswordHash);
        user.setStatus(UserStatus.ACTIVE);
        user.setMustChangePassword(false);
        user.setActivatedAt(Instant.now());
        user.setPasswordLastChangedAt(Instant.now());
        user.setPasswordExpiresAt(Instant.now().plus(90, ChronoUnit.DAYS));

        userRepository.save(user);

        // 6. Delete activation token (single use)
        tokenRepository.delete(token);

        // 7. Assign default role
        assignDefaultRole(user);

        // 8. Send confirmation email
        emailService.sendActivationConfirmation(user);

        // 9. Audit log
        auditLog.log("ACCOUNT_ACTIVATED", user.getUsername(),
            "User successfully activated account");

        return ActivationResult.success();
    }
}
```

#### Step 7: Post-Activation

**Email Confirmation:**
```
To: kablu@company.com
Subject: RA Account Activated Successfully

Dear Kablu Ahmed,

Your RA account has been successfully activated!

Username: kablu.ahmed
Status: ACTIVE
Activated on: 2026-01-15 14:30:00 UTC

You can now log in to the RA portal at:
https://ra.company.com/login

Your password will expire on: 2026-04-15

Security Tips:
- Never share your password
- Enable two-factor authentication (recommended)
- Log out after each session
- Report suspicious activity immediately

Best regards,
RA System Team
```

**Audit Trail:**
```json
{
  "event_id": "evt-123",
  "timestamp": "2026-01-15T14:30:00Z",
  "action": "ACCOUNT_ACTIVATED",
  "user_id": "uuid-12345",
  "username": "kablu.ahmed",
  "ip_address": "192.168.1.50",
  "user_agent": "Mozilla/5.0...",
  "activation_method": "EMAIL_SMS_VERIFICATION",
  "registration_source": "HR_INTEGRATION",
  "time_to_activation": "2 hours 15 minutes"
}
```

---

## 5. Detailed Registration Process - Option 2: RA Administrator Registration

### 5.1 Overview
Manual registration performed by RA Administrator with mandatory identity verification.

### 5.2 Prerequisites
- Administrator has RA_ADMIN role
- Identity documents required (Employee ID, Photo ID, Email verification)

### 5.3 Step-by-Step Process

#### Step 1: Administrator Initiates Registration

**Admin Portal Interface:**
```
┌─────────────────────────────────────────────────┐
│        RA Admin - Register New User             │
├─────────────────────────────────────────────────┤
│                                                 │
│  Personal Information                           │
│  ───────────────────────────────────            │
│  First Name: [____________]                     │
│  Last Name:  [____________]                     │
│  Email:      [____________@company.com]         │
│  Mobile:     [+92__________]                    │
│                                                 │
│  Employment Information                         │
│  ───────────────────────────────────            │
│  Employee ID: [____________]                    │
│  Department:  [▼ Select Department]             │
│  Job Title:   [____________]                    │
│  Manager:     [▼ Select Manager]                │
│                                                 │
│  Identity Verification                          │
│  ───────────────────────────────────            │
│  ID Document Type: [▼ Passport/License/ID]     │
│  ID Number:        [____________]               │
│  Verified By:      [Admin Name] (Auto-filled)  │
│  Verification Date: [2026-01-15] (Auto-filled) │
│                                                 │
│  Initial Role Assignment                        │
│  ───────────────────────────────────            │
│  [ ] END_ENTITY (Default)                       │
│  [ ] RA_OPERATOR                                │
│  [ ] RA_OFFICER                                 │
│  [ ] AUDITOR                                    │
│                                                 │
│  Credential Delivery Method                     │
│  ───────────────────────────────────            │
│  [✓] Email + SMS (Recommended)                  │
│  [ ] Email only                                 │
│  [ ] In-person delivery (print credentials)     │
│                                                 │
│  [Register User]                                │
│                                                 │
└─────────────────────────────────────────────────┘
```

#### Step 2: Admin Submits Registration

```http
POST /api/v1/admin/users/register
Authorization: Bearer <admin-jwt-token>
Content-Type: application/json

{
  "first_name": "Kablu",
  "last_name": "Ahmed",
  "email": "kablu@company.com",
  "mobile_number": "+923001234567",
  "employee_id": "EMP-12345",
  "department": "Engineering",
  "job_title": "Software Engineer",
  "manager_user_id": "uuid-manager-123",
  "identity_verification": {
    "document_type": "EMPLOYEE_ID",
    "document_number": "ID-2024-12345",
    "verified_by_admin_id": "uuid-admin-456",
    "verification_date": "2026-01-15",
    "verification_method": "FACE_TO_FACE"
  },
  "initial_roles": ["END_ENTITY"],
  "delivery_method": "EMAIL_SMS",
  "notes": "New employee onboarded manually"
}
```

#### Step 3: Identity Verification Record

```sql
INSERT INTO identity_verifications (
    verification_id,
    user_id,
    verified_by_admin_id,
    verification_date,
    document_type,
    document_number,
    verification_method,
    photo_verified,
    verification_notes,
    verification_location
) VALUES (
    'uuid-verify-789',
    'uuid-12345',
    'uuid-admin-456',
    '2026-01-15',
    'EMPLOYEE_ID',
    'ID-2024-12345',
    'FACE_TO_FACE',
    TRUE,
    'Employee verified in person at IT office',
    'IT Security Office - Building A'
);
```

#### Step 4: Credential Generation and Delivery

Same as Option 1, Steps 3-7.

**Additional Security for Admin Registration:**
- Admin's identity logged in audit trail
- Verification details permanently stored
- Cannot be deleted (only deactivated for compliance)

---

## 6. Detailed Registration Process - Option 3: Self-Registration with Approval

### 6.1 Overview
End users can request accounts via self-service portal, subject to admin approval.

### 6.2 Step-by-Step Process

#### Step 1: User Submits Registration Request

**Public Registration Portal (No Authentication Required):**

```
┌─────────────────────────────────────────────────┐
│      RA System - Request Account Access         │
├─────────────────────────────────────────────────┤
│                                                 │
│  Personal Information                           │
│  ───────────────────────────────────            │
│  First Name: [____________]                     │
│  Last Name:  [____________]                     │
│  Email:      [____________@company.com]         │
│  Mobile:     [+92__________]                    │
│                                                 │
│  Employment Information                         │
│  ───────────────────────────────────            │
│  Employee ID: [____________]                    │
│  Department:  [____________]                    │
│  Reason for Access: [___________________]       │
│                                                 │
│  Email Verification                             │
│  ───────────────────────────────────            │
│  Verification Code: [______]                    │
│  [Send Code to Email]                           │
│                                                 │
│  [Submit Request]                               │
│                                                 │
│  Note: Your request will be reviewed by an      │
│  administrator. You will receive an email       │
│  once approved.                                 │
│                                                 │
└─────────────────────────────────────────────────┘
```

#### Step 2: Email Verification

**User clicks "Send Code to Email":**

```http
POST /api/v1/public/registration/verify-email
Content-Type: application/json

{
  "email": "kablu@company.com"
}
```

**Verification Email:**
```
To: kablu@company.com
Subject: RA Registration - Email Verification Code

Your verification code is: 847293

This code is valid for 10 minutes.

If you did not request this code, please ignore this email.
```

**User enters code and submits registration:**

```http
POST /api/v1/public/registration/request
Content-Type: application/json

{
  "first_name": "Kablu",
  "last_name": "Ahmed",
  "email": "kablu@company.com",
  "mobile_number": "+923001234567",
  "employee_id": "EMP-12345",
  "department": "Engineering",
  "reason": "Need access for certificate management",
  "email_verification_code": "847293"
}
```

#### Step 3: Admin Review and Approval

**Admin receives notification:**

```
To: ra-admins@company.com
Subject: New User Registration Request - Pending Approval

A new user registration request requires your review:

Name: Kablu Ahmed
Email: kablu@company.com
Employee ID: EMP-12345
Department: Engineering
Reason: Need access for certificate management

Request submitted: 2026-01-15 10:00:00 UTC

Review request: https://ra.company.com/admin/registrations/pending
```

**Admin Portal Review Interface:**

```
┌─────────────────────────────────────────────────┐
│     Pending Registration Request - Review       │
├─────────────────────────────────────────────────┤
│                                                 │
│  Request ID: REQ-2026-001                       │
│  Submitted: 2026-01-15 10:00:00 UTC             │
│                                                 │
│  Applicant Information                          │
│  ───────────────────────────────────            │
│  Name: Kablu Ahmed                              │
│  Email: kablu@company.com (verified ✓)          │
│  Mobile: +923001234567                          │
│  Employee ID: EMP-12345                         │
│  Department: Engineering                        │
│                                                 │
│  Verification Checks                            │
│  ───────────────────────────────────            │
│  [✓] Email verified                             │
│  [✓] No duplicate accounts found                │
│  [?] Employee ID verification required          │
│                                                 │
│  Reason for Access:                             │
│  "Need access for certificate management"       │
│                                                 │
│  Admin Actions                                  │
│  ───────────────────────────────────            │
│  Assign Role: [▼ END_ENTITY]                    │
│  Admin Notes: [_______________________]         │
│                                                 │
│  [Approve & Create Account]  [Reject Request]   │
│                                                 │
└─────────────────────────────────────────────────┘
```

#### Step 4: Admin Approves - Credential Generation

**Admin clicks "Approve & Create Account":**

```http
POST /api/v1/admin/registrations/{request_id}/approve
Authorization: Bearer <admin-jwt-token>
Content-Type: application/json

{
  "request_id": "REQ-2026-001",
  "assigned_roles": ["END_ENTITY"],
  "admin_notes": "Verified with HR department. Approved.",
  "delivery_method": "EMAIL_SMS"
}
```

**System proceeds with credential generation and delivery (same as Option 1, Steps 3-7).**

#### Step 5: User Notification

**Approval Email:**
```
To: kablu@company.com
Subject: RA Registration Approved - Account Activated

Dear Kablu Ahmed,

Your RA account registration has been approved!

Your account details:
Username: kablu.ahmed
Employee ID: EMP-12345

To activate your account, please click: https://ra.company.com/activate?token=...

Your temporary password will be sent separately via SMS.

This activation link is valid for 72 hours.

Best regards,
RA System Team
```

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

**Password Generation Best Practices:**
```java
public class SecurePasswordGenerator {

    private static final SecureRandom secureRandom = new SecureRandom();

    // Use cryptographically secure random generator
    // NEVER use Math.random() for passwords

    public static String generate() {
        // Implementation using SecureRandom
        byte[] randomBytes = new byte[24];
        secureRandom.nextBytes(randomBytes);

        // Convert to password with required complexity
        return toPasswordFormat(randomBytes);
    }
}
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
| HR System Integration | Medium | Standard employees |
| Admin Registration (Face-to-Face) | High | Sensitive roles |
| Self-Registration | Low-Medium | External users, contractors |

**Recommended Verification by Role:**

```
Role                Verification Required
────────────────────────────────────────────
END_ENTITY          Email verification + Admin approval
RA_OPERATOR         Face-to-face + ID document + Manager approval
RA_OFFICER          Face-to-face + ID document + Background check
RA_ADMIN            Face-to-face + ID document + Security clearance
AUDITOR             Face-to-face + ID document + Compliance approval
```

### 7.4 Protection Against Attacks

**1. Brute Force Protection:**
```java
@Component
public class LoginAttemptService {

    private final Map<String, Integer> attemptCache = new ConcurrentHashMap<>();

    public void loginFailed(String username) {
        int attempts = attemptCache.getOrDefault(username, 0);
        attemptCache.put(username, attempts + 1);

        if (attempts + 1 >= 5) {
            // Lock account for 30 minutes
            userService.lockAccount(username, Duration.ofMinutes(30));
            auditLog.log("ACCOUNT_LOCKED", username, "Too many failed attempts");
        }
    }
}
```

**2. Account Enumeration Prevention:**
- Same error message for invalid username and invalid password
- Response time constant (no timing attacks)
- No indication whether username exists

**3. Activation Token Protection:**
- Rate limit activation attempts (5 per hour per IP)
- Token invalidated after 3 failed password attempts
- No token reuse (single activation only)

**4. Email/SMS Spoofing Prevention:**
- SPF, DKIM, DMARC configured for email domain
- SMS sender ID registered and verified
- Include verification details in message (partial username, timestamp)

---

## 8. Database Schema for Registration

```sql
-- User Registration Requests (for self-registration)
CREATE TABLE registration_requests (
    request_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    mobile_number VARCHAR(20),
    employee_id VARCHAR(50),
    department VARCHAR(100),
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

-- Activation Tokens
CREATE TABLE activation_tokens (
    token_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(user_id),
    token_hash VARCHAR(64) NOT NULL, -- SHA-256 hash
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    used_at TIMESTAMP,
    delivery_method VARCHAR(50), -- EMAIL_SMS, EMAIL_ONLY, IN_PERSON
    CONSTRAINT unique_active_token UNIQUE (user_id, used_at)
        WHERE used_at IS NULL
);

-- Password History (prevent reuse)
CREATE TABLE password_history (
    history_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(user_id),
    password_hash VARCHAR(255) NOT NULL,
    changed_at TIMESTAMP NOT NULL DEFAULT NOW(),
    changed_by VARCHAR(50), -- SELF, ADMIN_RESET, SYSTEM
    changed_ip_address INET
);

-- Failed Login Attempts
CREATE TABLE failed_login_attempts (
    attempt_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(255) NOT NULL,
    attempted_at TIMESTAMP NOT NULL DEFAULT NOW(),
    ip_address INET,
    user_agent TEXT,
    failure_reason VARCHAR(100)
);

CREATE INDEX idx_failed_attempts_username ON failed_login_attempts(username, attempted_at);

-- Account Locks
CREATE TABLE account_locks (
    lock_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(user_id),
    locked_at TIMESTAMP NOT NULL DEFAULT NOW(),
    locked_until TIMESTAMP NOT NULL,
    lock_reason VARCHAR(100), -- TOO_MANY_ATTEMPTS, ADMIN_LOCK, SECURITY_INCIDENT
    locked_by UUID REFERENCES users(user_id), -- NULL if automatic
    unlocked_at TIMESTAMP,
    unlocked_by UUID REFERENCES users(user_id)
);
```

---

## 9. REST API Endpoints for Registration

### 9.1 Self-Registration Endpoints (Public - No Auth)

```http
POST /api/v1/public/registration/verify-email
Content-Type: application/json

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

{
  "first_name": "Kablu",
  "last_name": "Ahmed",
  "email": "kablu@company.com",
  "mobile_number": "+923001234567",
  "employee_id": "EMP-12345",
  "department": "Engineering",
  "reason": "Certificate management access",
  "email_verification_code": "847293"
}

Response: 201 Created
{
  "request_id": "REQ-2026-001",
  "status": "PENDING_APPROVAL",
  "message": "Registration request submitted. You will receive an email once reviewed."
}
```

### 9.2 Admin Registration Endpoints (Requires RA_ADMIN role)

```http
POST /api/v1/admin/users/register
Authorization: Bearer <admin-jwt>
Content-Type: application/json

{
  "first_name": "Kablu",
  "last_name": "Ahmed",
  "email": "kablu@company.com",
  "mobile_number": "+923001234567",
  "employee_id": "EMP-12345",
  "department": "Engineering",
  "job_title": "Software Engineer",
  "identity_verification": {
    "document_type": "EMPLOYEE_ID",
    "document_number": "ID-2024-12345"
  },
  "initial_roles": ["END_ENTITY"],
  "delivery_method": "EMAIL_SMS"
}

Response: 201 Created
{
  "user_id": "uuid-12345",
  "username": "kablu.ahmed",
  "status": "PENDING_ACTIVATION",
  "activation_expires_at": "2026-01-18T10:30:00Z",
  "credentials_sent_to": {
    "email": "kablu@company.com",
    "mobile": "+9230012xxxxx"
  }
}
```

```http
GET /api/v1/admin/registrations/pending
Authorization: Bearer <admin-jwt>

Response: 200 OK
{
  "pending_requests": [
    {
      "request_id": "REQ-2026-001",
      "email": "kablu@company.com",
      "first_name": "Kablu",
      "last_name": "Ahmed",
      "submitted_at": "2026-01-15T10:00:00Z",
      "email_verified": true
    }
  ]
}
```

```http
POST /api/v1/admin/registrations/{request_id}/approve
Authorization: Bearer <admin-jwt>
Content-Type: application/json

{
  "assigned_roles": ["END_ENTITY"],
  "admin_notes": "Verified with HR",
  "delivery_method": "EMAIL_SMS"
}

Response: 200 OK
{
  "user_id": "uuid-12345",
  "username": "kablu.ahmed",
  "message": "User account created and credentials sent"
}
```

### 9.3 User Activation Endpoint (Public - Token Auth)

```http
POST /api/v1/users/activate
Content-Type: application/json

{
  "activation_token": "base64-encoded-token",
  "temporary_password": "Kx7$mPq2@Yn5#Zt9",
  "new_password": "MyNewSecureP@ss2026!",
  "confirm_password": "MyNewSecureP@ss2026!"
}

Response: 200 OK
{
  "message": "Account activated successfully",
  "username": "kablu.ahmed",
  "password_expires_at": "2026-04-15T00:00:00Z",
  "login_url": "https://ra.company.com/login"
}

Response: 400 Bad Request (Password validation failed)
{
  "error": "password_validation_failed",
  "details": [
    "Password must be at least 12 characters",
    "Password must contain at least 1 special character"
  ]
}
```

---

## 10. Audit Requirements

Every registration action must be logged:

```json
{
  "event_id": "evt-abc-123",
  "timestamp": "2026-01-15T10:30:00Z",
  "action": "USER_REGISTERED",
  "resource_type": "USER",
  "resource_id": "uuid-12345",
  "performed_by": "HR_SYSTEM",
  "registration_method": "HR_INTEGRATION",
  "user_details": {
    "username": "kablu.ahmed",
    "email": "kablu@company.com",
    "employee_id": "EMP-12345"
  },
  "credentials_delivered_via": ["EMAIL", "SMS"],
  "ip_address": "192.168.1.100",
  "result": "SUCCESS"
}
```

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
- [ ] HR integration triggers successful registration
- [ ] Admin can manually register users
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
- [ ] HR system API integration working
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
### Appendix B: SMS Message Templates
### Appendix C: Password Policy Configuration
### Appendix D: Error Messages and User Guidance
### Appendix E: Admin Training Guide

---

**Document End**

**Approval:**
- [ ] IT Security Manager
- [ ] PKI Administrator
- [ ] Compliance Officer

**Document Control:**
- Next Review Date: 2026-04-13
- Version History:
  - v1.0 (2026-01-13): Initial draft with HR system integration
  - v2.0 (2026-01-13): Updated for standalone RA user registration (no HR/AD dependency)
    - Removed HR system integration
    - Added bootstrap super admin process
    - Updated for open-ended RA management user registration
    - Added comprehensive summary and troubleshooting sections
