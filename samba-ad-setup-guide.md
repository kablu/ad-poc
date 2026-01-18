# Samba Active Directory Setup Guide
## Docker Compose Installation

**Date**: 2026-01-15
**Purpose**: Local Active Directory for RA Web Application Development and Testing

---

## Table of Contents

1. [Quick Start](#1-quick-start)
2. [Configuration Details](#2-configuration-details)
3. [Post-Installation Setup](#3-post-installation-setup)
4. [Creating Users and Groups](#4-creating-users-and-groups)
5. [LDAP Connection Testing](#5-ldap-connection-testing)
6. [Integration with RA Application](#6-integration-with-ra-application)
7. [Troubleshooting](#7-troubleshooting)

---

## 1. Quick Start

### 1.1 Prerequisites

```bash
# Ensure Docker and Docker Compose are installed
docker --version
docker-compose --version

# If not installed, install Docker Desktop (Windows/Mac) or Docker Engine (Linux)
```

### 1.2 Start Samba AD

```bash
# Navigate to project directory
cd D:\ecc-dev\jdk-21-poc\ra-web

# Start containers
docker-compose -f docker-compose-samba-ad.yml up -d

# Check container status
docker-compose -f docker-compose-samba-ad.yml ps

# View logs
docker-compose -f docker-compose-samba-ad.yml logs -f samba-ad
```

**Expected Output**:
```
Creating network "ra-web_ad-network" with driver "bridge"
Creating volume "ra-web_samba-ad-data" with local driver
Creating volume "ra-web_samba-ad-config" with local driver
Creating volume "ra-web_samba-ad-log" with local driver
Creating samba-ad-dc ... done
Creating ldap-admin  ... done
```

### 1.3 Wait for Initialization

**Important**: Samba AD takes 2-3 minutes to fully initialize on first start.

```bash
# Monitor logs until you see "samba: ready to serve connections"
docker logs -f samba-ad-dc
```

**Successful Initialization Indicators**:
- `Checking smb.conf`
- `provisioning Samba4 AD`
- `samba: ready to serve connections`

### 1.4 Access LDAP Admin Interface

**URL**: http://localhost:8080

**Login Credentials**:
- **Login DN**: `cn=Administrator,cn=Users,dc=sambaad,dc=local`
- **Password**: `CompanyAdmin@123`

---

## 2. Configuration Details

### 2.1 Domain Configuration

| Parameter | Value | Description |
|-----------|-------|-------------|
| **Domain Name** | `sambaad.local` | Full DNS domain name |
| **NetBIOS Name** | `CORP` | Short domain name for Windows (max 15 chars) |
| **Realm** | `SAMBAAD.LOCAL` | Kerberos realm (uppercase, auto-generated from domain name) |
| **Base DN** | `dc=sambaad,dc=local` | LDAP base distinguished name |
| **Admin User** | `Administrator` | Default domain administrator |
| **Admin Password** | `CompanyAdmin@123` | Default admin password (change in production!) |
| **Domain Controller** | `dc1.sambaad.local` | Hostname of DC |
| **IP Address** | `172.20.0.10` | Static IP in Docker network |

**Important Note**: The NetBIOS name (`CORP`) must be different from the realm's first component (`SAMBAAD`). The realm is automatically generated from the domain name (sambaad.local → SAMBAAD.LOCAL).

### 2.2 Network Ports

| Port | Protocol | Service | Purpose |
|------|----------|---------|---------|
| 53 | TCP/UDP | DNS | Domain Name Service |
| 88 | TCP/UDP | Kerberos | Authentication |
| 389 | TCP | LDAP | Lightweight Directory Access Protocol |
| 636 | TCP | LDAPS | LDAP over SSL/TLS |
| 445 | TCP | SMB | Server Message Block (file sharing) |
| 464 | TCP/UDP | Kerberos | Password change |
| 3268 | TCP | Global Catalog | LDAP Global Catalog |
| 3269 | TCP | Global Catalog SSL | LDAP GC over SSL |
| 8080 | TCP | HTTP | phpLDAPadmin web interface |

### 2.3 Docker Volumes

| Volume | Mount Point | Purpose |
|--------|-------------|---------|
| `samba-ad-data` | `/var/lib/samba` | AD database, user data |
| `samba-ad-config` | `/etc/samba/external` | Configuration files |
| `samba-ad-log` | `/var/log/samba` | Log files |

---

## 3. Post-Installation Setup

### 3.1 Verify Domain Controller

```bash
# Enter container
docker exec -it samba-ad-dc bash

# Check domain information
samba-tool domain info 127.0.0.1

# Expected output:
# Forest           : company.local
# Domain           : company.local
# Netbios domain   : COMPANY
# DC name          : dc1.company.local
# DC netbios name  : DC1
# Server site      : Default-First-Site-Name
# Client site      : Default-First-Site-Name

# Check DNS
nslookup company.local 127.0.0.1

# Check LDAP
ldapsearch -x -H ldap://127.0.0.1 -D "cn=Administrator,cn=Users,dc=sambaad,dc=local" \
  -w CompanyAdmin@123 -b "dc=sambaad,dc=local" "(objectClass=*)" dn

# Exit container
exit
```

### 3.2 Change Administrator Password (Recommended)

```bash
# Enter container
docker exec -it samba-ad-dc bash

# Change password
samba-tool user setpassword Administrator
# Enter new password when prompted

# Exit container
exit
```

**Note**: Update `docker-compose-samba-ad.yml` with new password after changing.

---

## 4. Creating Users and Groups

### 4.1 Create Organizational Units (OUs)

```bash
# Enter container
docker exec -it samba-ad-dc bash

# Create OUs for organization structure
samba-tool ou create "OU=Employees,DC=company,DC=local"
samba-tool ou create "OU=Groups,DC=company,DC=local"
samba-tool ou create "OU=IT,OU=Employees,DC=company,DC=local"
samba-tool ou create "OU=Engineering,OU=Employees,DC=company,DC=local"
samba-tool ou create "OU=HR,OU=Employees,DC=company,DC=local"
```

### 4.2 Create User Accounts

```bash
# Create test users
samba-tool user create jdoe \
  --given-name=John \
  --surname=Doe \
  --mail-address=john.doe@company.com \
  --use-username-as-cn \
  --job-title="Software Engineer" \
  --department="Engineering"
# Password: TestPassword@123

samba-tool user create asmith \
  --given-name=Alice \
  --surname=Smith \
  --mail-address=alice.smith@company.com \
  --use-username-as-cn \
  --job-title="IT Security Officer" \
  --department="IT"
# Password: TestPassword@123

samba-tool user create bobadmin \
  --given-name=Bob \
  --surname=Admin \
  --mail-address=bob.admin@company.com \
  --use-username-as-cn \
  --job-title="RA Administrator" \
  --department="IT"
# Password: AdminPassword@123
```

### 4.3 Create Groups for RA Roles

```bash
# Create RA role groups
samba-tool group add "PKI-RA-Admins" \
  --description="Registration Authority Administrators"

samba-tool group add "PKI-RA-Officers" \
  --description="Registration Authority Officers"

samba-tool group add "PKI-RA-Operators" \
  --description="Registration Authority Operators (Help Desk)"

samba-tool group add "PKI-Auditors" \
  --description="PKI Compliance Auditors"

samba-tool group add "VPN-Users" \
  --description="Users authorized for VPN certificates"

samba-tool group add "Email-Users" \
  --description="Users authorized for email certificates"

samba-tool group add "Developers" \
  --description="Software developers (code signing certificates)"
```

### 4.4 Add Users to Groups

```bash
# Assign Bob as RA Administrator
samba-tool group addmembers "PKI-RA-Admins" bobadmin

# Assign Alice as RA Officer
samba-tool group addmembers "PKI-RA-Officers" asmith

# Assign John to standard user groups
samba-tool group addmembers "VPN-Users" jdoe
samba-tool group addmembers "Email-Users" jdoe
samba-tool group addmembers "Developers" jdoe

# Verify group membership
samba-tool group listmembers "PKI-RA-Admins"
samba-tool group listmembers "VPN-Users"

# Exit container
exit
```

### 4.5 Set User Attributes

```bash
# Enter container
docker exec -it samba-ad-dc bash

# Use ldapmodify to add additional attributes
cat <<EOF > /tmp/user-attributes.ldif
dn: CN=John Doe,CN=Users,DC=company,DC=local
changetype: modify
replace: telephoneNumber
telephoneNumber: +1-555-1234
-
replace: employeeID
employeeID: EMP001
-
replace: title
title: Senior Software Engineer

dn: CN=Alice Smith,CN=Users,DC=company,DC=local
changetype: modify
replace: telephoneNumber
telephoneNumber: +1-555-5678
-
replace: employeeID
employeeID: EMP002
-
replace: title
title: IT Security Officer

dn: CN=Bob Admin,CN=Users,DC=company,DC=local
changetype: modify
replace: telephoneNumber
telephoneNumber: +1-555-9999
-
replace: employeeID
employeeID: EMP003
-
replace: title
title: RA Administrator
EOF

# Apply changes
ldapmodify -x -H ldap://127.0.0.1 \
  -D "cn=Administrator,cn=Users,dc=sambaad,dc=local" \
  -w CompanyAdmin@123 \
  -f /tmp/user-attributes.ldif

# Verify
ldapsearch -x -H ldap://127.0.0.1 \
  -D "cn=Administrator,cn=Users,dc=sambaad,dc=local" \
  -w CompanyAdmin@123 \
  -b "cn=John Doe,cn=Users,dc=sambaad,dc=local" \
  telephoneNumber employeeID title

exit
```

---

## 5. LDAP Connection Testing

### 5.1 Test with ldapsearch (Linux/Mac/WSL)

```bash
# Basic connection test
ldapsearch -x -H ldap://localhost:389 \
  -D "cn=Administrator,cn=Users,dc=sambaad,dc=local" \
  -w CompanyAdmin@123 \
  -b "dc=sambaad,dc=local" \
  "(objectClass=user)" cn mail department

# Test LDAPS (secure)
ldapsearch -x -H ldaps://localhost:636 \
  -D "cn=Administrator,cn=Users,dc=sambaad,dc=local" \
  -w CompanyAdmin@123 \
  -b "dc=sambaad,dc=local" \
  "(objectClass=user)" cn

# Search for specific user
ldapsearch -x -H ldap://localhost:389 \
  -D "cn=Administrator,cn=Users,dc=sambaad,dc=local" \
  -w CompanyAdmin@123 \
  -b "dc=sambaad,dc=local" \
  "(sAMAccountName=jdoe)" *
```

### 5.2 Test with Apache Directory Studio

1. **Download**: https://directory.apache.org/studio/
2. **Create New Connection**:
   - Connection name: `Samba AD Local`
   - Hostname: `localhost`
   - Port: `389`
   - Encryption: `No encryption` (or `Use SSL` for port 636)
3. **Authentication**:
   - Bind DN: `cn=Administrator,cn=Users,dc=sambaad,dc=local`
   - Password: `CompanyAdmin@123`
4. **Test Connection**: Click "Check Network Parameter"
5. **Browse**: Navigate tree to see users and groups

### 5.3 Test with Python (ldap3 library)

```python
from ldap3 import Server, Connection, ALL

# Connect to Samba AD
server = Server('localhost', port=389, get_info=ALL)
conn = Connection(
    server,
    user='cn=Administrator,cn=Users,dc=sambaad,dc=local',
    password='CompanyAdmin@123',
    auto_bind=True
)

# Search for users
conn.search(
    search_base='dc=sambaad,dc=local',
    search_filter='(objectClass=user)',
    attributes=['cn', 'mail', 'department', 'memberOf']
)

# Print results
for entry in conn.entries:
    print(entry.entry_dn)
    print(f"  Name: {entry.cn}")
    print(f"  Email: {entry.mail}")
    print(f"  Department: {entry.department}")
    print(f"  Groups: {entry.memberOf}")
    print()

conn.unbind()
```

### 5.4 Test with Java (Spring LDAP)

```java
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.ldap.query.LdapQueryBuilder;

public class LdapTest {
    public static void main(String[] args) {
        // Configure LDAP connection
        LdapContextSource contextSource = new LdapContextSource();
        contextSource.setUrl("ldap://localhost:389");
        contextSource.setBase("dc=sambaad,dc=local");
        contextSource.setUserDn("cn=Administrator,cn=Users,dc=sambaad,dc=local");
        contextSource.setPassword("CompanyAdmin@123");
        contextSource.afterPropertiesSet();

        // Create LDAP template
        LdapTemplate ldapTemplate = new LdapTemplate(contextSource);

        // Search for user
        List<String> users = ldapTemplate.search(
            LdapQueryBuilder.query().where("objectClass").is("user"),
            (AttributesMapper<String>) attrs ->
                attrs.get("cn").get().toString()
        );

        System.out.println("Users found:");
        users.forEach(System.out::println);
    }
}
```

---

## 6. Integration with RA Application

### 6.1 Spring Boot LDAP Configuration

**application.yml**:
```yaml
spring:
  ldap:
    urls: ldap://localhost:389
    base: dc=sambaad,dc=local
    username: cn=Administrator,cn=Users,dc=sambaad,dc=local
    password: CompanyAdmin@123

ra:
  ad:
    domain: company.local
    user-search-base: cn=Users,dc=sambaad,dc=local
    user-search-filter: (sAMAccountName={0})
    group-search-base: cn=Users,dc=sambaad,dc=local
    group-search-filter: (member={0})

  role-mapping:
    - ad-group: cn=PKI-RA-Admins,cn=Users,dc=sambaad,dc=local
      app-role: RA_ADMIN
    - ad-group: cn=PKI-RA-Officers,cn=Users,dc=sambaad,dc=local
      app-role: RA_OFFICER
    - ad-group: cn=PKI-RA-Operators,cn=Users,dc=sambaad,dc=local
      app-role: RA_OPERATOR
    - ad-group: cn=PKI-Auditors,cn=Users,dc=sambaad,dc=local
      app-role: AUDITOR
```

### 6.2 Java Authentication Service

```java
@Service
public class ActiveDirectoryAuthenticationService {

    @Autowired
    private LdapTemplate ldapTemplate;

    public User authenticateUser(String username, String password) {
        try {
            // Bind with user credentials
            String userDn = findUserDn(username);
            ldapTemplate.authenticate(userDn, "(objectClass=*)", password);

            // Retrieve user attributes
            return ldapTemplate.search(
                LdapQueryBuilder.query()
                    .where("sAMAccountName").is(username),
                new UserAttributesMapper()
            ).stream().findFirst().orElse(null);

        } catch (Exception e) {
            throw new AuthenticationException("Invalid credentials", e);
        }
    }

    private String findUserDn(String username) {
        return ldapTemplate.search(
            LdapQueryBuilder.query()
                .where("sAMAccountName").is(username),
            (AttributesMapper<String>) attrs ->
                attrs.get("distinguishedName").get().toString()
        ).stream().findFirst()
         .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    public Set<String> getUserGroups(String userDn) {
        return ldapTemplate.search(
            LdapQueryBuilder.query()
                .base(userDn)
                .where("objectClass").is("user"),
            (AttributesMapper<Set<String>>) attrs -> {
                Attribute memberOf = attrs.get("memberOf");
                Set<String> groups = new HashSet<>();
                if (memberOf != null) {
                    NamingEnumeration<?> values = memberOf.getAll();
                    while (values.hasMore()) {
                        groups.add(values.next().toString());
                    }
                }
                return groups;
            }
        ).stream().findFirst().orElse(Collections.emptySet());
    }
}
```

### 6.3 Testing Authentication Flow

```bash
# Test user authentication
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "jdoe",
    "password": "TestPassword@123"
  }'

# Expected response:
{
  "access_token": "eyJhbGc...",
  "token_type": "Bearer",
  "expires_in": 3600,
  "user": {
    "username": "jdoe",
    "display_name": "John Doe",
    "email": "john.doe@company.com",
    "department": "Engineering",
    "roles": ["END_ENTITY", "VPN_USER", "DEVELOPER"]
  }
}
```

---

## 7. Troubleshooting

### 7.1 Container Won't Start

**Issue**: `samba-ad-dc` container exits immediately

**Solution**:
```bash
# Check logs
docker logs samba-ad-dc

# Common issues:
# 1. Port conflicts (53, 389, 636 already in use)
#    - Stop conflicting services or change ports in docker-compose

# 2. Permission issues
docker exec -it samba-ad-dc bash
chown -R root:root /var/lib/samba
exit

# 3. Corrupted volumes
docker-compose -f docker-compose-samba-ad.yml down -v
docker-compose -f docker-compose-samba-ad.yml up -d
```

### 7.2 Cannot Connect to LDAP

**Issue**: `ldapsearch` or application cannot connect

**Solution**:
```bash
# 1. Verify container is running
docker ps | grep samba-ad

# 2. Check port binding
netstat -an | grep 389

# 3. Test from container itself
docker exec -it samba-ad-dc bash
ldapsearch -x -H ldap://127.0.0.1 -b "dc=sambaad,dc=local"

# 4. Check firewall (Windows)
# Allow port 389 in Windows Firewall

# 5. Verify DNS resolution
nslookup company.local 172.20.0.10
```

### 7.3 Authentication Fails

**Issue**: User authentication returns "Invalid credentials"

**Solution**:
```bash
# 1. Verify user exists
docker exec -it samba-ad-dc samba-tool user list | grep jdoe

# 2. Check user account status
docker exec -it samba-ad-dc samba-tool user show jdoe

# 3. Reset password
docker exec -it samba-ad-dc samba-tool user setpassword jdoe
# Enter: TestPassword@123

# 4. Test with smbclient
docker exec -it samba-ad-dc smbclient -L localhost -U jdoe%TestPassword@123

# 5. Check LDAP bind directly
ldapsearch -x -H ldap://localhost:389 \
  -D "cn=John Doe,cn=Users,dc=sambaad,dc=local" \
  -w TestPassword@123 \
  -b "dc=sambaad,dc=local" "(objectClass=user)"
```

### 7.4 Group Membership Not Working

**Issue**: User not recognized as member of group

**Solution**:
```bash
# 1. Verify group membership
docker exec -it samba-ad-dc samba-tool group listmembers "PKI-RA-Admins"

# 2. Check memberOf attribute
ldapsearch -x -H ldap://localhost:389 \
  -D "cn=Administrator,cn=Users,dc=sambaad,dc=local" \
  -w CompanyAdmin@123 \
  -b "cn=Bob Admin,cn=Users,dc=sambaad,dc=local" \
  memberOf

# 3. Re-add user to group
docker exec -it samba-ad-dc bash
samba-tool group removemembers "PKI-RA-Admins" bobadmin
samba-tool group addmembers "PKI-RA-Admins" bobadmin
exit
```

### 7.5 LDAPS (SSL) Not Working

**Issue**: Cannot connect via LDAPS on port 636

**Solution**:
```bash
# 1. Check if LDAPS is enabled
docker exec -it samba-ad-dc netstat -tlnp | grep 636

# 2. Export AD certificate
docker exec -it samba-ad-dc bash
tdbdump /var/lib/samba/private/secrets.tdb | grep -A 10 "SAMDB/DOMAINCONTROLLER_CERTIFICATE"
# Copy certificate to /tmp/ad-cert.pem
exit

# 3. Trust certificate on client
# Copy cert from container
docker cp samba-ad-dc:/tmp/ad-cert.pem ./ad-cert.pem

# 4. Test LDAPS with certificate
ldapsearch -x -H ldaps://localhost:636 \
  -D "cn=Administrator,cn=Users,dc=sambaad,dc=local" \
  -w CompanyAdmin@123 \
  -b "dc=sambaad,dc=local"
```

### 7.6 phpLDAPadmin Cannot Connect

**Issue**: phpLDAPadmin shows connection error

**Solution**:
```bash
# 1. Check ldap-admin container logs
docker logs ldap-admin

# 2. Verify network connectivity
docker exec ldap-admin ping samba-ad

# 3. Check LDAP from ldap-admin container
docker exec ldap-admin nc -zv samba-ad 389

# 4. Restart ldap-admin
docker restart ldap-admin
```

---

## 8. Stopping and Cleanup

### 8.1 Stop Containers

```bash
# Stop all containers
docker-compose -f docker-compose-samba-ad.yml stop

# Start again
docker-compose -f docker-compose-samba-ad.yml start
```

### 8.2 Remove Containers (Keep Data)

```bash
# Remove containers but keep volumes (data persists)
docker-compose -f docker-compose-samba-ad.yml down
```

### 8.3 Complete Cleanup (Remove Data)

```bash
# WARNING: This deletes all AD data!
docker-compose -f docker-compose-samba-ad.yml down -v

# Remove images
docker rmi nowsci/samba-domain:latest
docker rmi osixia/phpldapadmin:latest
```

---

## 9. Production Considerations

### 9.1 Security Hardening

**⚠️ This setup is for DEVELOPMENT ONLY. For production:**

1. **Change Default Passwords**:
   ```bash
   # Strong admin password
   samba-tool user setpassword Administrator
   ```

2. **Enable LDAPS Only** (disable plain LDAP):
   ```yaml
   environment:
     INSECURELDAP: "false"
   ```

3. **Use Complex Passwords**:
   ```yaml
   environment:
     NOCOMPLEXITY: "false"
   ```

4. **Restrict Network Access**:
   ```yaml
   networks:
     ad-network:
       internal: true  # No external access
   ```

5. **Enable Firewall Rules**:
   - Only allow RA application to connect to AD
   - Block direct user access to LDAP ports

### 9.2 Backup and Recovery

```bash
# Backup AD data
docker exec samba-ad-dc samba-tool domain backup offline \
  --targetdir=/var/lib/samba/backup

# Copy backup to host
docker cp samba-ad-dc:/var/lib/samba/backup ./ad-backup

# Restore (if needed)
docker exec samba-ad-dc samba-tool domain backup restore \
  --backup-file=/var/lib/samba/backup/samba-backup-*.tar.bz2 \
  --newservername=dc1
```

### 9.3 Monitoring

```bash
# Monitor AD health
docker exec samba-ad-dc samba-tool dbcheck

# Check replication (if multiple DCs)
docker exec samba-ad-dc samba-tool drs showrepl

# View active connections
docker exec samba-ad-dc smbstatus
```

---

## 10. Quick Reference

### 10.1 Connection Parameters

| Parameter | Value |
|-----------|-------|
| **LDAP URL** | `ldap://localhost:389` |
| **LDAPS URL** | `ldaps://localhost:636` |
| **Base DN** | `dc=sambaad,dc=local` |
| **Admin DN** | `cn=Administrator,cn=Users,dc=sambaad,dc=local` |
| **Admin Password** | `CompanyAdmin@123` |
| **Domain** | `company.local` |
| **phpLDAPadmin** | http://localhost:8080 |

### 10.2 Common Commands

```bash
# List all users
docker exec samba-ad-dc samba-tool user list

# List all groups
docker exec samba-ad-dc samba-tool group list

# Show user details
docker exec samba-ad-dc samba-tool user show jdoe

# Show group members
docker exec samba-ad-dc samba-tool group listmembers "VPN-Users"

# Reset user password
docker exec samba-ad-dc samba-tool user setpassword jdoe

# Disable user account
docker exec samba-ad-dc samba-tool user disable jdoe

# Enable user account
docker exec samba-ad-dc samba-tool user enable jdoe

# Delete user
docker exec samba-ad-dc samba-tool user delete jdoe
```

---

## 11. Next Steps

1. ✅ **Start Samba AD**: `docker-compose -f docker-compose-samba-ad.yml up -d`
2. ✅ **Create Test Users**: Follow section 4.2
3. ✅ **Create RA Role Groups**: Follow section 4.3
4. ✅ **Test LDAP Connection**: Follow section 5
5. ✅ **Integrate with RA App**: Follow section 6
6. ✅ **Test Authentication**: Follow section 6.3

---

**Document End**

**Prepared By**: DevOps Team
**Status**: Ready for Development Use
**Security Note**: For development/testing only. Harden for production.
