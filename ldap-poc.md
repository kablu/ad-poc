# LDAP/Active Directory POC - Complete Guide

**Date**: 2026-02-03
**Project**: RA Web Application - Registration Authority
**Purpose**: LDAP/AD Proof of Concept with LDIF file creation, user/group management, and memberOf handling

---

## Table of Contents

1. [AD POC Environment Setup](#1-ad-poc-environment-setup)
2. [LDIF File Basics](#2-ldif-file-basics)
3. [Creating Users via LDIF](#3-creating-users-via-ldif)
4. [Creating Groups via LDIF](#4-creating-groups-via-ldif)
5. [memberOf - How It Works](#5-memberof---how-it-works)
6. [Complete LDIF Examples for RA Application](#6-complete-ldif-examples-for-ra-application)
7. [Importing LDIF Files](#7-importing-ldif-files)
8. [samba-tool Commands (Alternative to LDIF)](#8-samba-tool-commands-alternative-to-ldif)
9. [Java LDAP Integration](#9-java-ldap-integration)
10. [Verifying memberOf with ldapsearch](#10-verifying-memberof-with-ldapsearch)
11. [Troubleshooting](#11-troubleshooting)
12. [Related Documents](#12-related-documents)

---

## 1. AD POC Environment Setup

### 1.1 Docker-based Samba AD

Our POC uses Samba AD running in Docker:

```
LDAP URL:       ldap://localhost:389
LDAPS URL:      ldaps://localhost:636
Base DN:        dc=corp,dc=local
Admin DN:       cn=Administrator,cn=Users,dc=corp,dc=local
Password:       P@ssw0rd123!
Domain:         corp.local
Realm:          CORP.LOCAL
NetBIOS:        CORP
Container:      samba-ad-dc
```

### 1.2 Start the AD Environment

```bash
# Start Samba AD + phpLDAPadmin
docker compose -f docker-compose-samba-ad.yml up -d

# Wait 2-3 minutes for initialization
docker logs -f samba-ad-dc

# Verify LDAP is working
ldapsearch -x -H ldap://localhost:389 \
  -D "cn=Administrator,cn=Users,dc=corp,dc=local" \
  -w "P@ssw0rd123!" \
  -b "dc=corp,dc=local" \
  "(objectClass=user)" cn
```

### 1.3 phpLDAPadmin Web UI

Access at: http://localhost:8080

Login:
- **Login DN**: `cn=Administrator,cn=Users,dc=corp,dc=local`
- **Password**: `P@ssw0rd123!`

---

## 2. LDIF File Basics

### 2.1 LDIF Format

LDIF (LDAP Data Interchange Format) is a standard text format for representing LDAP directory entries and update operations.

**Key Rules:**
- Each entry starts with `dn:` (Distinguished Name)
- Attributes are `key: value` pairs
- Entries are separated by blank lines
- Lines starting with `#` are comments
- `changetype: add` = add new entry
- `changetype: modify` = modify existing entry
- `changetype: delete` = delete entry

### 2.2 Basic LDIF Structure

```ldif
# Comment line
dn: cn=John Doe,cn=Users,dc=corp,dc=local
changetype: add
objectClass: top
objectClass: person
objectClass: organizationalPerson
objectClass: user
cn: John Doe
sAMAccountName: jdoe
userPrincipalName: jdoe@corp.local
givenName: John
sn: Doe
mail: john.doe@corp.local
department: Engineering
```

---

## 3. Creating Users via LDIF

### 3.1 Simple User

```ldif
dn: cn=Kablu Ahmed,cn=Users,dc=corp,dc=local
changetype: add
objectClass: top
objectClass: person
objectClass: organizationalPerson
objectClass: user
cn: Kablu Ahmed
sAMAccountName: kablu
userPrincipalName: kablu@corp.local
givenName: Kablu
sn: Ahmed
displayName: Kablu Ahmed
mail: kablu@corp.local
department: Engineering
title: Software Engineer
telephoneNumber: +923001234567
userAccountControl: 512
```

**Note:** `userAccountControl: 512` = Normal enabled account

### 3.2 User with Password (Samba AD)

In Samba AD, password is set via `unicodePwd` attribute:

```ldif
dn: cn=Kablu Ahmed,cn=Users,dc=corp,dc=local
changetype: add
objectClass: top
objectClass: person
objectClass: organizationalPerson
objectClass: user
cn: Kablu Ahmed
sAMAccountName: kablu
userPrincipalName: kablu@corp.local
givenName: Kablu
sn: Ahmed
displayName: Kablu Ahmed
mail: kablu@corp.local
department: Engineering
userAccountControl: 512
unicodePwd:: IgBQAEAAcwBzAHcAMAByAGQAMQAyADMAIQAiAA==
```

**Important:** `unicodePwd` requires:
- UTF-16LE encoding
- Wrapped in double quotes
- Base64 encoded (indicated by `::` after attribute name)
- LDAPS connection (port 636) in real AD

### 3.3 User in Custom OU

```ldif
# First create the OU
dn: ou=RA Users,dc=corp,dc=local
changetype: add
objectClass: top
objectClass: organizationalUnit
ou: RA Users
description: Registration Authority Users

# Then create user in that OU
dn: cn=Kablu Ahmed,ou=RA Users,dc=corp,dc=local
changetype: add
objectClass: top
objectClass: person
objectClass: organizationalPerson
objectClass: user
cn: Kablu Ahmed
sAMAccountName: kablu
userPrincipalName: kablu@corp.local
givenName: Kablu
sn: Ahmed
displayName: Kablu Ahmed
mail: kablu@corp.local
department: Engineering
userAccountControl: 512
```

---

## 4. Creating Groups via LDIF

### 4.1 Create a Security Group

```ldif
dn: cn=PKI-RA-Admins,cn=Users,dc=corp,dc=local
changetype: add
objectClass: top
objectClass: group
cn: PKI-RA-Admins
sAMAccountName: PKI-RA-Admins
description: RA Administrators - Full system access
groupType: -2147483646
```

**groupType Values:**
- `-2147483646` = Global Security Group (most common)
- `-2147483644` = Domain Local Security Group
- `-2147483640` = Universal Security Group
- `2` = Global Distribution Group
- `4` = Domain Local Distribution Group
- `8` = Universal Distribution Group

### 4.2 Create Group with Members (memberOf auto-populated)

```ldif
dn: cn=PKI-RA-Admins,cn=Users,dc=corp,dc=local
changetype: add
objectClass: top
objectClass: group
cn: PKI-RA-Admins
sAMAccountName: PKI-RA-Admins
description: RA Administrators
groupType: -2147483646
member: cn=Kablu Ahmed,cn=Users,dc=corp,dc=local
member: cn=Bob Admin,cn=Users,dc=corp,dc=local
```

**Jab aap `member` attribute group mein add karte ho, Active Directory / Samba AD automatically us user ka `memberOf` attribute update kar deta hai.**

### 4.3 Add Members to Existing Group (Modify)

```ldif
dn: cn=PKI-RA-Officers,cn=Users,dc=corp,dc=local
changetype: modify
add: member
member: cn=Alice Smith,cn=Users,dc=corp,dc=local
member: cn=John Doe,cn=Users,dc=corp,dc=local
```

### 4.4 Remove Member from Group

```ldif
dn: cn=PKI-RA-Officers,cn=Users,dc=corp,dc=local
changetype: modify
delete: member
member: cn=John Doe,cn=Users,dc=corp,dc=local
```

---

## 5. memberOf - How It Works

### 5.1 memberOf is a Back-Link Attribute

`memberOf` is **NOT** directly writable in most LDAP implementations. It is a **computed/back-link attribute** that is automatically managed by the directory server.

```
Relationship:
  Group has "member" attribute    →  Forward Link (you set this)
  User has "memberOf" attribute   →  Back Link (auto-populated)
```

### 5.2 How to Populate memberOf

**Method 1: Add `member` to the Group (Recommended)**

```ldif
# Group mein member add karo -> memberOf auto-populate hoga
dn: cn=PKI-RA-Admins,cn=Users,dc=corp,dc=local
changetype: modify
add: member
member: cn=Kablu Ahmed,cn=Users,dc=corp,dc=local
```

Result: Kablu ka `memberOf` automatically set ho jayega:
```
memberOf: cn=PKI-RA-Admins,cn=Users,dc=corp,dc=local
```

**Method 2: Create Group with Members Already Listed**

```ldif
dn: cn=PKI-RA-Officers,cn=Users,dc=corp,dc=local
changetype: add
objectClass: top
objectClass: group
cn: PKI-RA-Officers
sAMAccountName: PKI-RA-Officers
description: RA Officers
groupType: -2147483646
member: cn=Alice Smith,cn=Users,dc=corp,dc=local
member: cn=Kablu Ahmed,cn=Users,dc=corp,dc=local
```

### 5.3 memberOf Direct Set (OpenLDAP Only)

In OpenLDAP with `memberOf` overlay enabled, you can directly set `memberOf`:

```ldif
# ONLY works in OpenLDAP with memberOf overlay, NOT in AD/Samba
dn: cn=Kablu Ahmed,ou=Users,dc=corp,dc=local
changetype: modify
add: memberOf
memberOf: cn=PKI-RA-Admins,ou=Groups,dc=corp,dc=local
```

**Warning:** This does NOT work in Active Directory or Samba AD. Always use the group's `member` attribute instead.

### 5.4 Active Directory vs OpenLDAP

| Feature | Active Directory / Samba AD | OpenLDAP |
|---------|---------------------------|----------|
| memberOf auto-populate | Yes (built-in) | Requires memberOf overlay |
| Direct memberOf write | No | Yes (with overlay) |
| group objectClass | `group` | `groupOfNames` or `groupOfUniqueNames` |
| member attribute | `member` (DN) | `member` or `uniqueMember` (DN) |
| Empty group allowed | Yes | No (must have at least 1 member for groupOfNames) |

### 5.5 Visual Flow

```
Step 1: User exists
┌──────────────────────────────────────────┐
│ cn=Kablu Ahmed,cn=Users,dc=corp,dc=local │
│ sAMAccountName: kablu                     │
│ mail: kablu@corp.local                    │
│ memberOf: (empty)                         │
└──────────────────────────────────────────┘

Step 2: Add user as member of group
┌──────────────────────────────────────────────┐
│ cn=PKI-RA-Admins,cn=Users,dc=corp,dc=local   │
│ objectClass: group                            │
│ member: cn=Kablu Ahmed,cn=Users,dc=corp,...   │  ← You set this
└──────────────────────────────────────────────┘

Step 3: memberOf auto-populated on user
┌──────────────────────────────────────────────────────────┐
│ cn=Kablu Ahmed,cn=Users,dc=corp,dc=local                 │
│ sAMAccountName: kablu                                     │
│ mail: kablu@corp.local                                    │
│ memberOf: cn=PKI-RA-Admins,cn=Users,dc=corp,dc=local    │  ← Auto!
└──────────────────────────────────────────────────────────┘
```

---

## 6. Complete LDIF Examples for RA Application

### 6.1 Complete Setup LDIF (All Users + Groups + Memberships)

Save as `ra-setup.ldif`:

```ldif
# ============================================================
# RA Web Application - LDIF Setup File
# Domain: corp.local
# Base DN: dc=corp,dc=local
# ============================================================

# -----------------------------------------------------------
# STEP 1: Create Organizational Units
# -----------------------------------------------------------

dn: ou=RA Users,dc=corp,dc=local
changetype: add
objectClass: top
objectClass: organizationalUnit
ou: RA Users
description: Registration Authority Users

dn: ou=RA Groups,dc=corp,dc=local
changetype: add
objectClass: top
objectClass: organizationalUnit
ou: RA Groups
description: Registration Authority Groups

# -----------------------------------------------------------
# STEP 2: Create Users
# -----------------------------------------------------------

# RA Administrator
dn: cn=Bob Admin,cn=Users,dc=corp,dc=local
changetype: add
objectClass: top
objectClass: person
objectClass: organizationalPerson
objectClass: user
cn: Bob Admin
sAMAccountName: bobadmin
userPrincipalName: bobadmin@corp.local
givenName: Bob
sn: Admin
displayName: Bob Admin
mail: bob.admin@corp.local
department: IT Security
title: PKI Administrator
userAccountControl: 512

# RA Officer
dn: cn=Alice Smith,cn=Users,dc=corp,dc=local
changetype: add
objectClass: top
objectClass: person
objectClass: organizationalPerson
objectClass: user
cn: Alice Smith
sAMAccountName: alice
userPrincipalName: alice@corp.local
givenName: Alice
sn: Smith
displayName: Alice Smith
mail: alice@corp.local
department: Security Operations
title: Certificate Manager
userAccountControl: 512

# RA Operator
dn: cn=John Doe,cn=Users,dc=corp,dc=local
changetype: add
objectClass: top
objectClass: person
objectClass: organizationalPerson
objectClass: user
cn: John Doe
sAMAccountName: jdoe
userPrincipalName: jdoe@corp.local
givenName: John
sn: Doe
displayName: John Doe
mail: john.doe@corp.local
department: Help Desk
title: IT Support Specialist
userAccountControl: 512

# Auditor
dn: cn=Sara Auditor,cn=Users,dc=corp,dc=local
changetype: add
objectClass: top
objectClass: person
objectClass: organizationalPerson
objectClass: user
cn: Sara Auditor
sAMAccountName: sara
userPrincipalName: sara@corp.local
givenName: Sara
sn: Auditor
displayName: Sara Auditor
mail: sara@corp.local
department: Compliance
title: Compliance Officer
userAccountControl: 512

# End Entity User
dn: cn=Kablu Ahmed,cn=Users,dc=corp,dc=local
changetype: add
objectClass: top
objectClass: person
objectClass: organizationalPerson
objectClass: user
cn: Kablu Ahmed
sAMAccountName: kablu
userPrincipalName: kablu@corp.local
givenName: Kablu
sn: Ahmed
displayName: Kablu Ahmed
mail: kablu@corp.local
department: Engineering
title: Software Engineer
telephoneNumber: +923001234567
userAccountControl: 512

# -----------------------------------------------------------
# STEP 3: Create Groups
# -----------------------------------------------------------

# PKI-RA-Admins group with members
# memberOf will be AUTO-POPULATED on the users listed as member
dn: cn=PKI-RA-Admins,cn=Users,dc=corp,dc=local
changetype: add
objectClass: top
objectClass: group
cn: PKI-RA-Admins
sAMAccountName: PKI-RA-Admins
description: RA Administrators - Full system configuration access
groupType: -2147483646
member: cn=Bob Admin,cn=Users,dc=corp,dc=local

# PKI-RA-Officers group with members
dn: cn=PKI-RA-Officers,cn=Users,dc=corp,dc=local
changetype: add
objectClass: top
objectClass: group
cn: PKI-RA-Officers
sAMAccountName: PKI-RA-Officers
description: RA Officers - Approve/reject certificate requests
groupType: -2147483646
member: cn=Alice Smith,cn=Users,dc=corp,dc=local

# PKI-RA-Operators group with members
dn: cn=PKI-RA-Operators,cn=Users,dc=corp,dc=local
changetype: add
objectClass: top
objectClass: group
cn: PKI-RA-Operators
sAMAccountName: PKI-RA-Operators
description: RA Operators - Submit certificate requests
groupType: -2147483646
member: cn=John Doe,cn=Users,dc=corp,dc=local

# PKI-Auditors group with members
dn: cn=PKI-Auditors,cn=Users,dc=corp,dc=local
changetype: add
objectClass: top
objectClass: group
cn: PKI-Auditors
sAMAccountName: PKI-Auditors
description: PKI Auditors - Read-only access to audit logs
groupType: -2147483646
member: cn=Sara Auditor,cn=Users,dc=corp,dc=local

# -----------------------------------------------------------
# STEP 4: Add Kablu to multiple groups
# memberOf will auto-populate for each group membership
# -----------------------------------------------------------

dn: cn=PKI-RA-Operators,cn=Users,dc=corp,dc=local
changetype: modify
add: member
member: cn=Kablu Ahmed,cn=Users,dc=corp,dc=local
```

### 6.2 Role Mapping After LDIF Import

After importing the above LDIF, the role mapping will be:

| User | AD Group | Application Role |
|------|----------|-----------------|
| Bob Admin (bobadmin) | PKI-RA-Admins | RA_ADMIN |
| Alice Smith (alice) | PKI-RA-Officers | RA_OFFICER |
| John Doe (jdoe) | PKI-RA-Operators | RA_OPERATOR |
| Sara Auditor (sara) | PKI-Auditors | AUDITOR |
| Kablu Ahmed (kablu) | PKI-RA-Operators | RA_OPERATOR |
| Kablu Ahmed (kablu) | Domain Users (default) | END_ENTITY |

### 6.3 Verify memberOf After Import

```bash
# Check Kablu's memberOf
ldapsearch -x -H ldap://localhost:389 \
  -D "cn=Administrator,cn=Users,dc=corp,dc=local" \
  -w "P@ssw0rd123!" \
  -b "dc=corp,dc=local" \
  "(sAMAccountName=kablu)" memberOf

# Expected output:
# memberOf: cn=PKI-RA-Operators,cn=Users,dc=corp,dc=local
```

---

## 7. Importing LDIF Files

### 7.1 Using ldapadd (from Host)

```bash
# Add new entries
ldapadd -x -H ldap://localhost:389 \
  -D "cn=Administrator,cn=Users,dc=corp,dc=local" \
  -w "P@ssw0rd123!" \
  -f ra-setup.ldif

# Modify existing entries
ldapmodify -x -H ldap://localhost:389 \
  -D "cn=Administrator,cn=Users,dc=corp,dc=local" \
  -w "P@ssw0rd123!" \
  -f ra-modify.ldif
```

### 7.2 Using ldapadd (from inside Docker container)

```bash
# Copy LDIF file to container
docker cp ra-setup.ldif samba-ad-dc:/tmp/ra-setup.ldif

# Execute inside container
docker exec samba-ad-dc ldbadd \
  -H ldap://127.0.0.1 \
  -D "cn=Administrator,cn=Users,dc=corp,dc=local" \
  -w "P@ssw0rd123!" \
  -f /tmp/ra-setup.ldif
```

### 7.3 Using ldbmodify (Samba native tool)

```bash
# Inside the Samba AD container
docker exec samba-ad-dc ldbmodify \
  -H /var/lib/samba/private/sam.ldb \
  /tmp/ra-setup.ldif
```

### 7.4 Using phpLDAPadmin Web UI

1. Open http://localhost:8080
2. Login with Administrator credentials
3. Navigate to Import section
4. Paste or upload LDIF content
5. Click Import

### 7.5 Handling Import Errors

```bash
# Continue on error (skip entries that already exist)
ldapadd -x -H ldap://localhost:389 \
  -D "cn=Administrator,cn=Users,dc=corp,dc=local" \
  -w "P@ssw0rd123!" \
  -c \
  -f ra-setup.ldif

# -c flag = continue on errors
```

Common errors during import:
- `error code 68 - Entry Already Exists` - Entry already in directory, use `-c` to skip
- `error code 32 - No Such Object` - Parent entry doesn't exist (create OUs first)
- `error code 50 - Insufficient Access` - Admin credentials wrong
- `error code 53 - Unwilling to Perform` - Password operation needs LDAPS

---

## 8. samba-tool Commands (Alternative to LDIF)

For Samba AD, `samba-tool` is often easier than LDIF files:

### 8.1 Create Users

```bash
docker exec samba-ad-dc bash -c "
  samba-tool user create kablu Password@123 \
    --given-name=Kablu \
    --surname=Ahmed \
    --mail-address=kablu@corp.local \
    --department=Engineering \
    --company='Company Name'
"
```

### 8.2 Create Groups

```bash
docker exec samba-ad-dc bash -c "
  samba-tool group add 'PKI-RA-Admins' --description='RA Administrators'
  samba-tool group add 'PKI-RA-Officers' --description='RA Officers'
  samba-tool group add 'PKI-RA-Operators' --description='RA Operators'
  samba-tool group add 'PKI-Auditors' --description='PKI Auditors'
"
```

### 8.3 Add Members to Groups (memberOf auto-populated)

```bash
docker exec samba-ad-dc bash -c "
  # Add users to groups
  samba-tool group addmembers 'PKI-RA-Admins' bobadmin
  samba-tool group addmembers 'PKI-RA-Officers' alice
  samba-tool group addmembers 'PKI-RA-Operators' jdoe
  samba-tool group addmembers 'PKI-RA-Operators' kablu
  samba-tool group addmembers 'PKI-Auditors' sara
"
```

### 8.4 Verify Group Membership

```bash
# List members of a group
docker exec samba-ad-dc samba-tool group listmembers "PKI-RA-Admins"

# Show user details (includes memberOf)
docker exec samba-ad-dc samba-tool user show kablu
```

### 8.5 Complete Setup Script

Save as `setup-ra-users.sh`:

```bash
#!/bin/bash
echo "Setting up RA Application Users and Groups in Samba AD..."

docker exec samba-ad-dc bash -c "
  echo '=== Creating Users ==='
  samba-tool user create bobadmin Password@123 --given-name=Bob --surname=Admin --mail-address=bob.admin@corp.local
  samba-tool user create alice Password@123 --given-name=Alice --surname=Smith --mail-address=alice@corp.local
  samba-tool user create jdoe Password@123 --given-name=John --surname=Doe --mail-address=john.doe@corp.local
  samba-tool user create sara Password@123 --given-name=Sara --surname=Auditor --mail-address=sara@corp.local
  samba-tool user create kablu Password@123 --given-name=Kablu --surname=Ahmed --mail-address=kablu@corp.local

  echo '=== Creating Groups ==='
  samba-tool group add 'PKI-RA-Admins' --description='RA Administrators'
  samba-tool group add 'PKI-RA-Officers' --description='RA Officers'
  samba-tool group add 'PKI-RA-Operators' --description='RA Operators'
  samba-tool group add 'PKI-Auditors' --description='PKI Auditors'

  echo '=== Adding Members to Groups ==='
  samba-tool group addmembers 'PKI-RA-Admins' bobadmin
  samba-tool group addmembers 'PKI-RA-Officers' alice
  samba-tool group addmembers 'PKI-RA-Operators' jdoe
  samba-tool group addmembers 'PKI-RA-Operators' kablu
  samba-tool group addmembers 'PKI-Auditors' sara

  echo '=== Verification ==='
  echo 'Users:'
  samba-tool user list
  echo ''
  echo 'PKI-RA-Admins members:'
  samba-tool group listmembers 'PKI-RA-Admins'
  echo 'PKI-RA-Officers members:'
  samba-tool group listmembers 'PKI-RA-Officers'
  echo 'PKI-RA-Operators members:'
  samba-tool group listmembers 'PKI-RA-Operators'
  echo 'PKI-Auditors members:'
  samba-tool group listmembers 'PKI-Auditors'
"

echo "Done! All users and groups created."
```

---

## 9. Java LDAP Integration

### 9.1 Check memberOf in Java

```java
// After authenticating user, retrieve memberOf
Attributes attrs = ctx.getAttributes(
    "cn=Kablu Ahmed,cn=Users,dc=corp,dc=local",
    new String[]{"memberOf", "sAMAccountName", "mail"}
);

// Get memberOf attribute
Attribute memberOf = attrs.get("memberOf");
if (memberOf != null) {
    for (int i = 0; i < memberOf.size(); i++) {
        String groupDN = (String) memberOf.get(i);
        System.out.println("Member of: " + groupDN);
        // Output: Member of: cn=PKI-RA-Operators,cn=Users,dc=corp,dc=local
    }
}
```

### 9.2 Map memberOf to Application Roles

```java
// Role mapping from AD groups to application roles
Map<String, String> roleMapping = Map.of(
    "PKI-RA-Admins", "RA_ADMIN",
    "PKI-RA-Officers", "RA_OFFICER",
    "PKI-RA-Operators", "RA_OPERATOR",
    "PKI-Auditors", "AUDITOR"
);

// Extract group CN from full DN
// "cn=PKI-RA-Admins,cn=Users,dc=corp,dc=local" -> "PKI-RA-Admins"
String groupCN = groupDN.split(",")[0].replace("cn=", "").replace("CN=", "");
String appRole = roleMapping.getOrDefault(groupCN, "END_ENTITY");
```

### 9.3 Add User to Group via Java LDAP

```java
// Add member to group (memberOf will auto-populate on user)
ModificationItem[] mods = new ModificationItem[1];
mods[0] = new ModificationItem(
    DirContext.ADD_ATTRIBUTE,
    new BasicAttribute("member", "cn=Kablu Ahmed,cn=Users,dc=corp,dc=local")
);

ctx.modifyAttributes("cn=PKI-RA-Officers,cn=Users,dc=corp,dc=local", mods);
// Now Kablu's memberOf includes PKI-RA-Officers
```

---

## 10. Verifying memberOf with ldapsearch

### 10.1 Check User's Group Memberships

```bash
# Direct memberOf check
ldapsearch -x -H ldap://localhost:389 \
  -D "cn=Administrator,cn=Users,dc=corp,dc=local" \
  -w "P@ssw0rd123!" \
  -b "dc=corp,dc=local" \
  "(sAMAccountName=kablu)" memberOf
```

### 10.2 Check All Members of a Group

```bash
# List all members of PKI-RA-Admins
ldapsearch -x -H ldap://localhost:389 \
  -D "cn=Administrator,cn=Users,dc=corp,dc=local" \
  -w "P@ssw0rd123!" \
  -b "dc=corp,dc=local" \
  "(cn=PKI-RA-Admins)" member
```

### 10.3 Find All Groups a User Belongs To

```bash
# Using member attribute on groups
ldapsearch -x -H ldap://localhost:389 \
  -D "cn=Administrator,cn=Users,dc=corp,dc=local" \
  -w "P@ssw0rd123!" \
  -b "dc=corp,dc=local" \
  "(&(objectClass=group)(member=cn=Kablu Ahmed,cn=Users,dc=corp,dc=local))" cn
```

### 10.4 Get Full User Profile with Groups

```bash
ldapsearch -x -H ldap://localhost:389 \
  -D "cn=Administrator,cn=Users,dc=corp,dc=local" \
  -w "P@ssw0rd123!" \
  -b "dc=corp,dc=local" \
  "(sAMAccountName=kablu)" \
  cn sAMAccountName mail displayName department memberOf
```

---

## 11. Troubleshooting

### 11.1 memberOf Not Showing

**Problem:** User's `memberOf` attribute is empty even after adding to group.

**Causes & Solutions:**

1. **Samba AD not returning memberOf**: Some Samba versions need the attribute explicitly requested
   ```bash
   # Explicitly request memberOf
   ldapsearch ... "(sAMAccountName=kablu)" memberOf
   ```

2. **OpenLDAP missing memberOf overlay**: Enable it in `slapd.conf`:
   ```
   moduleload memberof.la
   overlay memberof
   ```

3. **Check using group's member attribute instead:**
   ```bash
   ldapsearch ... "(&(objectClass=group)(member=cn=Kablu Ahmed,cn=Users,dc=corp,dc=local))" cn
   ```

### 11.2 LDIF Import Error: "No Such Object"

Entries must be created in order - parent entries first:
```
1. Create OU (parent)
2. Create Users (children of OU)
3. Create Groups (after users exist, so member DNs are valid)
```

### 11.3 LDIF Import Error: "Entry Already Exists"

Use `-c` flag to continue on errors, or check first:
```bash
ldapsearch ... "(cn=Kablu Ahmed)" dn
```

### 11.4 LDAP Referral Error

Add `Context.REFERRAL = "ignore"` in Java or use `-e manageDSAIT` with ldapsearch:
```bash
ldapsearch -e manageDSAIT ...
```

See `LDAP_REFERRAL_FIX.md` for detailed Java fix.

---

## 12. Related Documents

| Document | Description |
|----------|-------------|
| `docker-compose-samba-ad.yml` | Docker setup for Samba AD + phpLDAPadmin |
| `AD-QUICK-REFERENCE.md` | Quick commands for AD management |
| `samba-ad-setup-guide.md` | Detailed Samba AD setup guide |
| `samba-ad-common-errors.md` | Common Samba AD errors and fixes |
| `samba-ad-port-conflict-fix.md` | Port 53 conflict resolution |
| `ad-setup-README.md` | AD setup overview |
| `ADD_USER_KABLU.md` | Guide for adding user Kablu to AD |
| `docker-installation-ubuntu.md` | Docker installation for Ubuntu |
| `docs/ACTIVE_DIRECTORY_INTEGRATION_GUIDE.md` | Spring Boot AD integration guide |
| `src/.../LDAP_README.md` | Java LDAP service documentation |
| `src/.../LDAP_REFERRAL_FIX.md` | LDAP referral error fix |

---

## Summary

### Key Takeaways

1. **memberOf is auto-populated** - You NEVER directly set `memberOf` in AD/Samba
2. **Set `member` on the Group** - This triggers `memberOf` on the user
3. **LDIF order matters** - Create OUs first, then Users, then Groups
4. **samba-tool is easier** - For Samba AD, use `samba-tool group addmembers` instead of LDIF
5. **Always verify** - Use `ldapsearch` to confirm memberOf is populated

### Quick Reference

```
memberOf INSERT karna hai?
  -> Group mein member add karo
  -> memberOf automatically set ho jayega

LDIF mein:
  dn: cn=GROUP-NAME,cn=Users,dc=corp,dc=local
  changetype: modify
  add: member
  member: cn=USER-NAME,cn=Users,dc=corp,dc=local

samba-tool mein:
  samba-tool group addmembers "GROUP-NAME" username
```

---

**Document Version**: 1.0
**Last Updated**: 2026-02-03
**Status**: Ready for Development
