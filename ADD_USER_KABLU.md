# Add User 'kablu' to Active Directory

## Quick Start

### Option 1: Run Script (Recommended)

**On Windows:**
```cmd
add-user-kablu.bat
```

**On Linux/Mac:**
```bash
./add-user-kablu.sh
```

### Option 2: Manual Execution

**Step 1: Compile**
```bash
javac -d target/classes src/main/java/com/company/ra/service/LDAPService.java
```

**Step 2: Run**
```bash
java -cp target/classes com.company.ra.service.LDAPService
```

---

## What This Will Do

The script will:

1. ✅ Connect to Active Directory at `ldap://localhost:389`
2. ✅ Authenticate as Administrator
3. ✅ Create OU "RA Users" (if not exists)
4. ✅ Add user "kablu" with password "mandal"
5. ✅ Set user attributes:
   - CN: kablu
   - sAMAccountName: kablu
   - userPrincipalName: kablu@corp.local
   - displayName: kablu
6. ✅ Enable the account
7. ✅ Verify user creation

---

## Expected Output

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
TEST 7: Authenticate user 'kablu' with correct password
==================================================
Waiting 2 seconds for password synchronization...

=== Authenticating User ===
Username: kablu
User DN: CN=kablu,OU=RA Users,DC=corp,DC=local
✓ Authentication successful!
✓ User 'kablu' credentials are valid
Result: Authentication with correct password = true

==================================================
TEST 8: Authenticate user 'kablu' with incorrect password
==================================================

=== Authenticating User ===
Username: kablu
User DN: CN=kablu,OU=RA Users,DC=corp,DC=local
✗ Authentication failed: Invalid username or password
  Error: [LDAP: error code 49 - Invalid Credentials]
Result: Authentication correctly rejected = true

==================================================
TEST 9: Authenticate and retrieve user information
==================================================

=== Authenticate and Retrieve User Info ===
Username: kablu

=== Authenticating User ===
Username: kablu
User DN: CN=kablu,OU=RA Users,DC=corp,DC=local
✓ Authentication successful!
✓ User 'kablu' credentials are valid

✓ User Information Retrieved:
  CN: kablu
  sAMAccountName: kablu
  DN: CN=kablu,OU=RA Users,DC=corp,DC=local
  Display Name: kablu
  UPN: kablu@corp.local
Result: User info retrieved = true

==================================================
SUMMARY
==================================================
✓ LDAP Connection: SUCCESS
✓ Administrator Check: FOUND
✓ OU Creation: SUCCESS
✓ User Addition: SUCCESS
✓ User Verification: SUCCESS
✓ User in OU Check: SUCCESS
✓ Authentication Test (Correct Password): SUCCESS
✓ Authentication Test (Incorrect Password): SUCCESS
✓ User Info Retrieval: SUCCESS

🎉 ALL TESTS PASSED! User 'kablu' successfully created, verified, and authenticated

✓ LDAP connection closed

==================================================
Test execution completed!
==================================================
```

---

## Verify User Creation

### Method 1: Using ldapsearch

```bash
ldapsearch -x -H ldap://localhost:389 \
  -D "cn=Administrator,cn=Users,dc=corp,dc=local" \
  -w "P@ssw0rd123!" \
  -b "ou=RA Users,dc=corp,dc=local" \
  "(cn=kablu)" cn sAMAccountName userPrincipalName
```

**Expected Output:**
```
# kablu, RA Users, corp.local
dn: CN=kablu,OU=RA Users,DC=corp,DC=local
cn: kablu
sAMAccountName: kablu
userPrincipalName: kablu@corp.local
```

### Method 2: Check in Active Directory Users and Computers

1. Open "Active Directory Users and Computers" (dsa.msc)
2. Navigate to: `corp.local` → `RA Users`
3. Look for user "kablu"

### Method 3: Test Login (if AD is configured)

```
Username: kablu
Password: mandal
Domain: corp.local
```

---

## User Details Created

| Attribute | Value |
|-----------|-------|
| **DN** | CN=kablu,OU=RA Users,DC=corp,DC=local |
| **CN** | kablu |
| **sAMAccountName** | kablu |
| **userPrincipalName** | kablu@corp.local |
| **displayName** | kablu |
| **Password** | mandal |
| **Account Status** | Enabled (userAccountControl: 512) |
| **OU** | RA Users |
| **Domain** | corp.local |

---

## Troubleshooting

### Issue 1: Connection Failed
```
✗ CONNECTION FAILED!
```

**Solution:**
- Verify LDAP server is running: `netstat -an | findstr 389`
- Check firewall: Allow port 389
- Test connectivity: `telnet localhost 389`

### Issue 2: Authentication Failed
```
✗ AUTHENTICATION FAILED!
```

**Solution:**
- Verify Administrator credentials
- Check DN: `cn=Administrator,cn=Users,dc=corp,dc=local`
- Verify password: `P@ssw0rd123!`

### Issue 3: User Already Exists
```
✗ Error adding user: [LDAP: error code 68 - Entry Already Exists]
```

**Solution:**
User 'kablu' already exists. To delete and recreate:

```bash
# Delete user
ldapdelete -x -H ldap://localhost:389 \
  -D "cn=Administrator,cn=Users,dc=corp,dc=local" \
  -w "P@ssw0rd123!" \
  "cn=kablu,ou=RA Users,dc=corp,dc=local"

# Then run the script again
```

### Issue 4: Password Policy Error
```
✗ Error setting password: [LDAP: error code 53 - Unwilling To Perform]
```

**Solution:**
Active Directory password policies may require:
- Minimum password length (default: 7 characters)
- Password complexity (uppercase, lowercase, number, special char)
- SSL/TLS for password operations

**Option A:** Change password to meet policy:
```java
String password = "M@ndal123!";  // Instead of "mandal"
```

**Option B:** Use LDAPS (SSL):
```java
private static final String LDAP_URL = "ldaps://localhost:636";
```

### Issue 5: Insufficient Permissions
```
✗ Error adding user: [LDAP: error code 50 - Insufficient Access Rights]
```

**Solution:**
- Verify Administrator account has permission to create users
- Check if OU "RA Users" allows user creation
- Verify you're authenticating as Administrator

---

## Configuration

If your LDAP settings are different, edit `LDAPService.java`:

```java
// LDAP Configuration (lines 19-22)
private static final String LDAP_URL = "ldap://localhost:389";
private static final String ADMIN_DN = "cn=Administrator,cn=Users,dc=corp,dc=local";
private static final String ADMIN_PASSWORD = "P@ssw0rd123!";
private static final String BASE_DN = "dc=corp,dc=local";
```

---

## After User Creation

Once user 'kablu' is created, you can:

### 1. Test SAML Authentication
If you have SAML IdP configured, user 'kablu' can authenticate via SAML.

### 2. Use in RA Application
User 'kablu' can now:
- Request certificates
- Login to RA Web Application
- Be assigned roles (RA_OFFICER, RA_OPERATOR, etc.)

### 3. Add to AD Groups
Assign user to groups for role-based access:

```bash
# Add to RA Officers group (example)
ldapmodify -x -H ldap://localhost:389 \
  -D "cn=Administrator,cn=Users,dc=corp,dc=local" \
  -w "P@ssw0rd123!" << EOF
dn: cn=PKI-RA-Officers,ou=Groups,dc=corp,dc=local
changetype: modify
add: member
member: cn=kablu,ou=RA Users,dc=corp,dc=local
EOF
```

### 4. Test Direct LDAP Authentication
```java
// Authenticate as kablu
String userDn = "cn=kablu,ou=RA Users,dc=corp,dc=local";
String password = "mandal";

Hashtable env = new Hashtable();
env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
env.put(Context.PROVIDER_URL, "ldap://localhost:389");
env.put(Context.SECURITY_AUTHENTICATION, "simple");
env.put(Context.SECURITY_PRINCIPAL, userDn);
env.put(Context.SECURITY_CREDENTIALS, password);

DirContext ctx = new InitialDirContext(env);
System.out.println("✓ Authentication successful!");
```

---

## Files Involved

| File | Purpose |
|------|---------|
| `LDAPService.java` | Main service to create user |
| `add-user-kablu.bat` | Windows script to run |
| `add-user-kablu.sh` | Linux/Mac script to run |
| `ADD_USER_KABLU.md` | This documentation |
| `LDAP_README.md` | Complete LDAP documentation |
| `LDAP_REFERRAL_FIX.md` | Fix for PartialResultException |

---

## Quick Commands

```bash
# Run the script
./add-user-kablu.sh   # Linux/Mac
add-user-kablu.bat    # Windows

# Verify user exists
ldapsearch -x -H ldap://localhost:389 \
  -D "cn=Administrator,cn=Users,dc=corp,dc=local" \
  -w "P@ssw0rd123!" \
  -b "dc=corp,dc=local" \
  "(cn=kablu)" cn

# Delete user (if needed)
ldapdelete -x -H ldap://localhost:389 \
  -D "cn=Administrator,cn=Users,dc=corp,dc=local" \
  -w "P@ssw0rd123!" \
  "cn=kablu,ou=RA Users,dc=corp,dc=local"

# List all users in OU
ldapsearch -x -H ldap://localhost:389 \
  -D "cn=Administrator,cn=Users,dc=corp,dc=local" \
  -w "P@ssw0rd123!" \
  -b "ou=RA Users,dc=corp,dc=local" \
  "(objectClass=user)" cn
```

---

## Success Criteria

✅ Script runs without errors
✅ User 'kablu' created in OU 'RA Users'
✅ Password 'mandal' set successfully
✅ Account enabled (userAccountControl: 512)
✅ ldapsearch confirms user exists
✅ User can authenticate (if AD authentication enabled)

---

**Ready to run!** Execute `add-user-kablu.bat` (Windows) or `./add-user-kablu.sh` (Linux/Mac) to create the user. 🚀
