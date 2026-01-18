# Active Directory Docker Setup - Quick Start Guide

**Date**: 2026-01-16
**Platform**: Ubuntu 20.04+ / Debian 11+
**Purpose**: One-command Active Directory installation for development/testing

---

## Quick Start (One Command)

```bash
# Start Active Directory
docker compose -f ad-docker-compose.yml up -d

# Monitor initialization (wait 2-3 minutes)
docker logs -f ad-dc
```

**Look for**: `samba: ready to serve connections` ✅

---

## Configuration Details

### Domain Information

| Parameter | Value |
|-----------|-------|
| **Domain Name** | `sambaad.local` |
| **NetBIOS Name** | `CORP` |
| **Realm** | `SAMBAAD.LOCAL` |
| **Base DN** | `dc=sambaad,dc=local` |
| **Admin User** | `Administrator` |
| **Admin Password** | `Admin@123456` |
| **DC IP** | `172.30.0.10` |

### Access Points

| Service | URL/Endpoint |
|---------|--------------|
| **LDAP** | `ldap://localhost:389` |
| **LDAPS** | `ldaps://localhost:636` |
| **phpLDAPadmin** | http://localhost:8080 |

---

## Initial Setup After Installation

### 1. Verify Installation

```bash
# Check container status
docker compose -f ad-docker-compose.yml ps

# View logs
docker logs ad-dc --tail 50

# Test LDAP connection
docker exec ad-dc samba-tool domain info 127.0.0.1
```

### 2. Access phpLDAPadmin

1. Open browser: http://localhost:8080
2. Click "login"
3. Enter credentials:
   - **Login DN**: `cn=Administrator,cn=Users,dc=sambaad,dc=local`
   - **Password**: `Admin@123456`

### 3. Create Test Users

```bash
# Enter AD container
docker exec -it ad-dc bash

# Create users
samba-tool user create jdoe Password@123 --given-name=John --surname=Doe --mail-address=john.doe@sambaad.local
samba-tool user create alice Password@123 --given-name=Alice --surname=Smith --mail-address=alice@sambaad.local
samba-tool user create bobadmin Password@123 --given-name=Bob --surname=Admin --mail-address=bob.admin@sambaad.local

# Create groups
samba-tool group add "PKI-RA-Admins" --description="RA Administrators"
samba-tool group add "PKI-RA-Officers" --description="RA Officers"
samba-tool group add "PKI-RA-Operators" --description="RA Operators"
samba-tool group add "PKI-Auditors" --description="PKI Auditors"

# Add users to groups
samba-tool group addmembers "PKI-RA-Admins" bobadmin
samba-tool group addmembers "PKI-RA-Officers" alice
samba-tool group addmembers "PKI-RA-Operators" jdoe

# List all users
samba-tool user list

# List group members
samba-tool group listmembers "PKI-RA-Admins"

# Exit container
exit
```

---

## LDAP Connection Testing

### From Host Machine (Linux/WSL)

```bash
# Install ldap-utils if not present
sudo apt-get install ldap-utils -y

# Test LDAP connection
ldapsearch -x -H ldap://localhost:389 \
  -D "cn=Administrator,cn=Users,dc=sambaad,dc=local" \
  -w Admin@123456 \
  -b "dc=sambaad,dc=local" \
  "(objectClass=user)" cn mail

# Search for specific user
ldapsearch -x -H ldap://localhost:389 \
  -D "cn=Administrator,cn=Users,dc=sambaad,dc=local" \
  -w Admin@123456 \
  -b "dc=sambaad,dc=local" \
  "(sAMAccountName=jdoe)" *

# List all groups
ldapsearch -x -H ldap://localhost:389 \
  -D "cn=Administrator,cn=Users,dc=sambaad,dc=local" \
  -w Admin@123456 \
  -b "dc=sambaad,dc=local" \
  "(objectClass=group)" cn member
```

### Test Authentication

```bash
# Test user authentication (bind)
ldapwhoami -x -H ldap://localhost:389 \
  -D "cn=John Doe,cn=Users,dc=sambaad,dc=local" \
  -w Password@123

# Expected output: u:CORP\jdoe
```

---

## Integration with Spring Boot Application

### application.yml

```yaml
spring:
  ldap:
    urls: ldap://localhost:389
    base: dc=sambaad,dc=local
    username: cn=Administrator,cn=Users,dc=sambaad,dc=local
    password: Admin@123456

ra:
  ad:
    domain: sambaad.local
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

### Java LDAP Service

```java
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.stereotype.Service;

@Service
public class ActiveDirectoryService {

    private final LdapTemplate ldapTemplate;

    public ActiveDirectoryService() {
        LdapContextSource contextSource = new LdapContextSource();
        contextSource.setUrl("ldap://localhost:389");
        contextSource.setBase("dc=sambaad,dc=local");
        contextSource.setUserDn("cn=Administrator,cn=Users,dc=sambaad,dc=local");
        contextSource.setPassword("Admin@123456");
        contextSource.afterPropertiesSet();

        this.ldapTemplate = new LdapTemplate(contextSource);
    }

    public boolean authenticate(String username, String password) {
        try {
            String userDn = "cn=" + username + ",cn=Users,dc=sambaad,dc=local";
            ldapTemplate.authenticate("", "(sAMAccountName=" + username + ")", password);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
```

---

## Management Commands

### Container Management

```bash
# Start AD
docker compose -f ad-docker-compose.yml up -d

# Stop AD
docker compose -f ad-docker-compose.yml stop

# Restart AD
docker compose -f ad-docker-compose.yml restart

# View logs
docker compose -f ad-docker-compose.yml logs -f

# Stop and remove (keeps data volumes)
docker compose -f ad-docker-compose.yml down

# Stop and remove including data (CAUTION: destroys all data)
docker compose -f ad-docker-compose.yml down -v
```

### User Management

```bash
# Enter container
docker exec -it ad-dc bash

# Create user
samba-tool user create USERNAME PASSWORD --given-name=First --surname=Last --mail-address=email@domain.com

# Delete user
samba-tool user delete USERNAME

# Change password
samba-tool user setpassword USERNAME

# Enable/disable user
samba-tool user enable USERNAME
samba-tool user disable USERNAME

# List all users
samba-tool user list

# Show user details
samba-tool user show USERNAME

# Exit
exit
```

### Group Management

```bash
# Enter container
docker exec -it ad-dc bash

# Create group
samba-tool group add GROUPNAME --description="Group Description"

# Delete group
samba-tool group delete GROUPNAME

# Add user to group
samba-tool group addmembers GROUPNAME USERNAME

# Remove user from group
samba-tool group removemembers GROUPNAME USERNAME

# List all groups
samba-tool group list

# List group members
samba-tool group listmembers GROUPNAME

# Exit
exit
```

---

## Troubleshooting

### Issue: Container won't start

```bash
# Check logs
docker logs ad-dc

# Common cause: Port already in use
sudo lsof -i :389
sudo lsof -i :636

# Solution: Stop conflicting service or change ports in docker-compose
```

### Issue: Cannot connect to LDAP

```bash
# 1. Verify container is running
docker ps | grep ad-dc

# 2. Check from inside container
docker exec ad-dc smbclient -L localhost -U Administrator%Admin@123456

# 3. Test LDAP from container
docker exec ad-dc ldapsearch -x -H ldap://127.0.0.1 -b "dc=sambaad,dc=local"

# 4. Check firewall (Ubuntu)
sudo ufw status
sudo ufw allow 389/tcp
sudo ufw allow 636/tcp
```

### Issue: Authentication fails

```bash
# Check user exists
docker exec ad-dc samba-tool user list | grep USERNAME

# Reset password
docker exec -it ad-dc samba-tool user setpassword USERNAME

# Verify correct DN format
# ✅ Correct: cn=Administrator,cn=Users,dc=sambaad,dc=local
# ❌ Wrong: Administrator@sambaad.local
```

### Issue: phpLDAPadmin shows error

```bash
# Restart ldap-admin container
docker restart ldap-admin

# Check logs
docker logs ldap-admin

# Verify ad-dc is reachable from ldap-admin
docker exec ldap-admin ping -c 3 ad-dc
```

---

## Production Recommendations

### ⚠️ Important Security Changes

Before deploying to production:

1. **Change Admin Password**:
   ```bash
   docker exec -it ad-dc samba-tool user setpassword Administrator
   ```

2. **Enable Password Complexity**:
   In `ad-docker-compose.yml`, change:
   ```yaml
   NOCOMPLEXITY: "false"
   ```

3. **Require Secure LDAP**:
   In `ad-docker-compose.yml`, change:
   ```yaml
   INSECURELDAP: "false"
   ```
   Then use only LDAPS (port 636) in applications.

4. **Restrict Network Access**:
   Use firewall rules to allow LDAP only from application servers:
   ```bash
   sudo ufw allow from 192.168.1.0/24 to any port 389
   ```

5. **Enable TLS/SSL**:
   Configure proper SSL certificates for LDAPS.

6. **Regular Backups**:
   ```bash
   # Backup AD data
   docker run --rm -v ad-data:/data -v $(pwd):/backup ubuntu tar czf /backup/ad-backup-$(date +%Y%m%d).tar.gz /data
   ```

---

## Quick Reference

### Connection Parameters

```
LDAP URL:     ldap://localhost:389
LDAPS URL:    ldaps://localhost:636
Base DN:      dc=sambaad,dc=local
Admin DN:     cn=Administrator,cn=Users,dc=sambaad,dc=local
Admin Pass:   Admin@123456
Domain:       sambaad.local
Realm:        SAMBAAD.LOCAL
NetBIOS:      CORP
```

### Test Users (after setup script)

| Username | Password | Groups | DN |
|----------|----------|--------|-----|
| Administrator | Admin@123456 | Domain Admins | cn=Administrator,cn=Users,dc=sambaad,dc=local |
| jdoe | Password@123 | PKI-RA-Operators | cn=John Doe,cn=Users,dc=sambaad,dc=local |
| alice | Password@123 | PKI-RA-Officers | cn=Alice Smith,cn=Users,dc=sambaad,dc=local |
| bobadmin | Password@123 | PKI-RA-Admins | cn=Bob Admin,cn=Users,dc=sambaad,dc=local |

---

## Support & Documentation

- **Samba Wiki**: https://wiki.samba.org/index.php/Main_Page
- **Docker Image**: https://hub.docker.com/r/nowsci/samba-domain
- **phpLDAPadmin**: http://phpldapadmin.sourceforge.net/

---

**Document Status**: Production Ready
**Last Updated**: 2026-01-16
**Tested On**: Ubuntu 22.04 LTS
