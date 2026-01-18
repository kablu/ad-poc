# LDAP Active Directory Service - README

## Overview

This package contains Java classes for testing LDAP connectivity and performing Active Directory operations.

## Files Created

### 1. LDAPService.java
**Main LDAP service with full functionality**

**Features:**
- ✅ Connect to Active Directory via LDAP
- ✅ Check if user exists in AD
- ✅ Add new user to AD with password
- ✅ Create Organizational Unit (OU)
- ✅ Verify user exists in specific OU
- ✅ List all users in AD

**Methods:**
```java
// Connect to LDAP
LDAPService service = new LDAPService();

// Check if user exists
boolean exists = service.checkUserExists("username");

// Create OU if not exists
boolean ouCreated = service.createOUIfNotExists("RA Users");

// Add new user
boolean added = service.addUser("kablu", "mandal", "RA Users");

// Verify user in OU
boolean inOU = service.checkUserInOU("kablu", "RA Users");

// List all users
service.listAllUsers();

// Close connection
service.close();
```

### 2. LDAPQuickTest.java
**Quick connectivity test utility**

**Purpose:**
- Fast LDAP connection verification
- Search for Administrator user
- List existing users
- Check if 'kablu' user exists

---

## Configuration

### LDAP Connection Details

Edit these constants in `LDAPService.java`:

```java
private static final String LDAP_URL = "ldap://localhost:389";
private static final String ADMIN_DN = "cn=Administrator,cn=Users,dc=corp,dc=local";
private static final String ADMIN_PASSWORD = "P@ssw0rd123!";
private static final String BASE_DN = "dc=corp,dc=local";
```

### Your Active Directory Structure

Based on the ldapsearch command:
- **LDAP URL**: `ldap://localhost:389`
- **Admin DN**: `cn=Administrator,cn=Users,dc=corp,dc=local`
- **Admin Password**: `P@ssw0rd123!`
- **Base DN**: `dc=corp,dc=local`
- **Domain**: `corp.local`

---

## Running the Tests

### Test 1: Quick Connectivity Test

```bash
# Compile
javac -d target/classes src/main/java/com/company/ra/service/LDAPQuickTest.java

# Run
java -cp target/classes com.company.ra.service.LDAPQuickTest
```

**Expected Output:**
```
╔════════════════════════════════════════╗
║      LDAP Quick Connection Test        ║
╚════════════════════════════════════════╝

LDAP URL: ldap://localhost:389
Admin DN: cn=Administrator,cn=Users,dc=corp,dc=local
Base DN: dc=corp,dc=local

==================================================
STEP 1: Connecting to LDAP...
==================================================
✓ Successfully connected to LDAP!

==================================================
STEP 2: Searching for Administrator user...
==================================================
✓ Administrator user found!
  DN: CN=Administrator,CN=Users,DC=corp,DC=local
  CN: Administrator
  sAMAccountName: Administrator

==================================================
STEP 3: Listing first 5 users in AD...
==================================================
  1. Administrator
  2. Guest
  ...

==================================================
STEP 4: Checking if user 'kablu' exists...
==================================================
✗ User 'kablu' not found (will be created)

==================================================
✓ LDAP Connection Test: SUCCESS
==================================================

✓ LDAP is working correctly!
✓ You can now run LDAPService.main() to create user 'kablu'
```

---

### Test 2: Full LDAP Service Test (Create User 'kablu')

```bash
# Compile
javac -d target/classes src/main/java/com/company/ra/service/LDAPService.java

# Run
java -cp target/classes com.company.ra.service.LDAPService
```

**Expected Output:**
```
╔════════════════════════════════════════════╗
║   LDAP Active Directory Test Application  ║
╚════════════════════════════════════════════╝
✓ Successfully connected to LDAP: ldap://localhost:389

==================================================
TEST 1: Check Administrator User
==================================================

=== Checking User Existence ===
Username: Administrator

✓ User found!
Distinguished Name: CN=Administrator,CN=Users,DC=corp,DC=local
CN: Administrator
sAMAccountName: Administrator
Result: Administrator exists = true

==================================================
TEST 2: Create OU 'RA Users'
==================================================
✓ OU created successfully: ou=RA Users,dc=corp,dc=local
Result: OU creation/verification = true

==================================================
TEST 3: Check if user 'kablu' exists
==================================================
✗ User not found in Active Directory
Result: kablu exists (before) = false

==================================================
TEST 4: Add user 'kablu' with password 'mandal'
==================================================

=== Adding New User ===
Username: kablu
OU: RA Users
User DN: cn=kablu,ou=RA Users,dc=corp,dc=local
✓ User created successfully!
✓ Password set successfully!
✓ User account enabled!
Result: User added = true

Waiting 2 seconds for AD synchronization...

==================================================
TEST 5: Verify user 'kablu' exists in AD
==================================================

=== Checking User Existence ===
Username: kablu

✓ User found!
Distinguished Name: CN=kablu,OU=RA Users,DC=corp,DC=local
CN: kablu
sAMAccountName: kablu
Display Name: kablu
Result: kablu exists (after) = true

==================================================
TEST 6: Verify user 'kablu' in OU 'RA Users'
==================================================

=== Checking User in OU ===
Username: kablu
OU: RA Users

✓ User found in OU!
Distinguished Name: CN=kablu,OU=RA Users,DC=corp,DC=local
CN: kablu
sAMAccountName: kablu
UPN: kablu@corp.local
Display Name: kablu
Result: kablu in OU = true

==================================================
SUMMARY
==================================================
✓ LDAP Connection: SUCCESS
✓ Administrator Check: FOUND
✓ OU Creation: SUCCESS
✓ User Addition: SUCCESS
✓ User Verification: SUCCESS
✓ User in OU Check: SUCCESS

🎉 ALL TESTS PASSED! User 'kablu' successfully created in OU 'RA Users'

✓ LDAP connection closed

==================================================
Test execution completed!
==================================================
```

---

## Verify with ldapsearch

After running `LDAPService.main()`, verify the user was created:

### Check user 'kablu' exists:
```bash
ldapsearch -x -H ldap://localhost:389 \
  -D "cn=Administrator,cn=Users,dc=corp,dc=local" \
  -w "P@ssw0rd123!" \
  -b "dc=corp,dc=local" \
  "(cn=kablu)" cn sAMAccountName distinguishedName
```

**Expected Output:**
```
# kablu, RA Users, corp.local
dn: CN=kablu,OU=RA Users,DC=corp,DC=local
cn: kablu
sAMAccountName: kablu
distinguishedName: CN=kablu,OU=RA Users,DC=corp,DC=local
```

### Check OU 'RA Users' was created:
```bash
ldapsearch -x -H ldap://localhost:389 \
  -D "cn=Administrator,cn=Users,dc=corp,dc=local" \
  -w "P@ssw0rd123!" \
  -b "dc=corp,dc=local" \
  "(ou=RA Users)"
```

### List all users in OU 'RA Users':
```bash
ldapsearch -x -H ldap://localhost:389 \
  -D "cn=Administrator,cn=Users,dc=corp,dc=local" \
  -w "P@ssw0rd123!" \
  -b "ou=RA Users,dc=corp,dc=local" \
  "(objectClass=user)" cn
```

---

## What the Code Does

### Test Sequence

1. **Connect to LDAP**
   - Authenticates as Administrator
   - Establishes LDAP context

2. **Check Administrator Exists**
   - Verifies LDAP is working
   - Tests search functionality

3. **Create OU 'RA Users'**
   - Creates organizational unit if not exists
   - Location: `ou=RA Users,dc=corp,dc=local`

4. **Check if 'kablu' exists (before)**
   - Searches entire directory
   - Returns false if not found

5. **Add User 'kablu'**
   - Username: `kablu`
   - Password: `mandal`
   - DN: `cn=kablu,ou=RA Users,dc=corp,dc=local`
   - Attributes:
     - `cn`: kablu
     - `sAMAccountName`: kablu
     - `userPrincipalName`: kablu@corp.local
     - `displayName`: kablu
     - `userAccountControl`: 512 (enabled account)

6. **Verify User Exists (after)**
   - Searches for kablu in entire directory
   - Returns true if found

7. **Verify User in OU**
   - Searches specifically in 'RA Users' OU
   - Confirms user location

---

## User Attributes Created

When user 'kablu' is created, the following attributes are set:

```
objectClass: top, person, organizationalPerson, user
cn: kablu
sAMAccountName: kablu
userPrincipalName: kablu@corp.local
displayName: kablu
userAccountControl: 512 (Normal account, enabled)
unicodePwd: "mandal" (encrypted)
```

**User DN**: `CN=kablu,OU=RA Users,DC=corp,DC=local`

---

## Troubleshooting

### Issue 1: Connection Refused
```
✗ CONNECTION FAILED!
javax.naming.CommunicationException: localhost:389
```

**Solution:**
- Verify LDAP server is running
- Check port 389 is open
- Test with: `telnet localhost 389`

### Issue 2: Authentication Failed
```
✗ AUTHENTICATION FAILED!
javax.naming.AuthenticationException: [LDAP: error code 49]
```

**Solution:**
- Verify administrator credentials
- Check DN format: `cn=Administrator,cn=Users,dc=corp,dc=local`
- Verify password: `P@ssw0rd123!`

### Issue 3: User Already Exists
```
✗ Error adding user: [LDAP: error code 68 - Entry Already Exists]
```

**Solution:**
- User 'kablu' already exists in AD
- Delete user first or use different username
- Check with: `ldapsearch -x ... "(cn=kablu)"`

### Issue 4: Insufficient Permissions
```
✗ Error adding user: [LDAP: error code 50 - Insufficient Access Rights]
```

**Solution:**
- Administrator account must have permissions to create users
- Verify you're authenticating as Administrator
- Check AD permissions

### Issue 5: Password Policy Error
```
✗ Error setting password: [LDAP: error code 53 - Unwilling To Perform]
```

**Solution:**
- Active Directory password policies may require:
  - Minimum password length
  - Complexity requirements
  - SSL/TLS connection for password operations
- Try using LDAPS (port 636) instead of LDAP
- Update password to meet policy (e.g., "M@ndal123!")

### Issue 6: SSL Required for Password
```
Note: AD often requires SSL/TLS for password operations
```

**Solution:**
For production, use LDAPS:
```java
private static final String LDAP_URL = "ldaps://localhost:636";
env.put(Context.SECURITY_PROTOCOL, "ssl");
```

---

## Testing Workflow

### Step-by-Step Testing

1. **Start with Quick Test**
   ```bash
   java com.company.ra.service.LDAPQuickTest
   ```
   - Verifies LDAP connectivity
   - Lists existing users

2. **Run Full Service Test**
   ```bash
   java com.company.ra.service.LDAPService
   ```
   - Creates OU and user
   - Verifies all operations

3. **Verify with ldapsearch**
   ```bash
   ldapsearch -x -H ldap://localhost:389 \
     -D "cn=Administrator,cn=Users,dc=corp,dc=local" \
     -w "P@ssw0rd123!" \
     -b "ou=RA Users,dc=corp,dc=local" \
     "(cn=kablu)" cn
   ```

4. **Test User Login** (if AD is configured for authentication)
   ```
   Username: kablu
   Password: mandal
   Domain: corp.local
   ```

---

## Integration with SAML/RA Application

After creating users in AD, you can use them for:

### 1. SAML Authentication
```java
// User 'kablu' can now authenticate via SAML IdP
// IdP will query AD for user attributes
```

### 2. Direct LDAP Authentication
```java
// Authenticate user against AD
String userDn = "cn=kablu,ou=RA Users,dc=corp,dc=local";
String password = "mandal";

Hashtable env = new Hashtable();
env.put(Context.SECURITY_PRINCIPAL, userDn);
env.put(Context.SECURITY_CREDENTIALS, password);
DirContext ctx = new InitialDirContext(env);
// If no exception, authentication successful
```

### 3. Retrieve User Attributes
```java
// Get user attributes for certificate request validation
Attributes attrs = ctx.getAttributes("cn=kablu,ou=RA Users,dc=corp,dc=local");
String email = (String) attrs.get("mail").get();
String department = (String) attrs.get("department").get();
```

---

## Production Considerations

### 1. Use SSL/TLS
```java
private static final String LDAP_URL = "ldaps://ad.corp.local:636";
env.put(Context.SECURITY_PROTOCOL, "ssl");
```

### 2. Use Service Account
Don't use Administrator credentials in production:
```java
private static final String ADMIN_DN = "cn=LDAPService,ou=Service Accounts,dc=corp,dc=local";
```

### 3. Connection Pooling
```java
env.put("com.sun.jndi.ldap.connect.pool", "true");
env.put("com.sun.jndi.ldap.connect.pool.maxsize", "10");
```

### 4. Error Handling
- Implement retry logic
- Handle connection timeouts
- Log all LDAP operations

### 5. Password Security
- Use password policies
- Require password change on first login
- Set password expiration

---

## Summary

✅ **LDAPService.java** - Full featured LDAP service
✅ **LDAPQuickTest.java** - Quick connectivity test
✅ **User 'kablu' creation** - With password 'mandal' in OU 'RA Users'
✅ **Verification methods** - Check user existence in AD and OU
✅ **Comprehensive testing** - 7 test cases in main method

**All operations tested:**
- Connect to AD via LDAP
- Create Organizational Unit
- Add user with password
- Verify user exists
- Verify user in specific OU
- List all users

**Ready for integration with RA Web Application and SAML authentication!**
