# Active Directory Quick Reference
**Setup Date**: 2026-01-16

---

## 📋 Connection Parameters

```
═══════════════════════════════════════════════════════════════
              ACTIVE DIRECTORY CONNECTION INFO
═══════════════════════════════════════════════════════════════
LDAP URL:       ldap://localhost:389
LDAPS URL:      ldaps://localhost:636
Base DN:        dc=corp,dc=local
Admin DN:       cn=Administrator,cn=Users,dc=corp,dc=local
Password:       P@ssw0rd123!
Domain:         corp.local
Realm:          CORP.LOCAL
NetBIOS:        CORP
Container:      samba-ad-dc
Network:        172.30.0.10
═══════════════════════════════════════════════════════════════
```

---

## 🚀 Quick Commands

### Start/Stop/Restart

```bash
# Start AD
docker compose -f ad-docker-compose.yml up -d

# Stop AD
docker compose -f ad-docker-compose.yml stop

# Restart AD
docker compose -f ad-docker-compose.yml restart

# Stop and remove (keeps data)
docker compose -f ad-docker-compose.yml down

# Stop and remove with data (CAUTION!)
docker compose -f ad-docker-compose.yml down -v

# View logs
docker logs -f samba-ad-dc
```

### Test LDAP Connection

```bash
# Basic connection test
ldapsearch -x -H ldap://localhost:389 \
  -D "cn=Administrator,cn=Users,dc=corp,dc=local" \
  -w "P@ssw0rd123!" \
  -b "dc=corp,dc=local" \
  "(objectClass=user)" cn

# List all users
ldapsearch -x -H ldap://localhost:389 \
  -D "cn=Administrator,cn=Users,dc=corp,dc=local" \
  -w "P@ssw0rd123!" \
  -b "dc=corp,dc=local" \
  -LLL "(objectClass=user)" sAMAccountName cn mail

# Search specific user
ldapsearch -x -H ldap://localhost:389 \
  -D "cn=Administrator,cn=Users,dc=corp,dc=local" \
  -w "P@ssw0rd123!" \
  -b "dc=corp,dc=local" \
  "(sAMAccountName=jdoe)" *
```

---

## 👥 User Management

### Enter Container

```bash
docker exec -it samba-ad-dc bash
```

### Create Users

```bash
# Create user
samba-tool user create jdoe Password@123 \
  --given-name=John \
  --surname=Doe \
  --mail-address=john.doe@corp.local

# Create user with more details
samba-tool user create alice Password@123 \
  --given-name=Alice \
  --surname=Smith \
  --mail-address=alice@corp.local \
  --department=Engineering \
  --company="Company Name"

# List all users
samba-tool user list

# Show user details
samba-tool user show jdoe

# Delete user
samba-tool user delete jdoe

# Change password
samba-tool user setpassword jdoe

# Enable/disable user
samba-tool user enable jdoe
samba-tool user disable jdoe
```

---

## 👥 Group Management

### Create Groups

```bash
# Create group
samba-tool group add "PKI-RA-Admins" --description="RA Administrators"
samba-tool group add "PKI-RA-Officers" --description="RA Officers"
samba-tool group add "PKI-RA-Operators" --description="RA Operators"
samba-tool group add "PKI-Auditors" --description="PKI Auditors"

# List all groups
samba-tool group list

# Add user to group
samba-tool group addmembers "PKI-RA-Admins" jdoe

# Remove user from group
samba-tool group removemembers "PKI-RA-Admins" jdoe

# List group members
samba-tool group listmembers "PKI-RA-Admins"

# Delete group
samba-tool group delete "PKI-RA-Admins"
```

---

## 🔍 Diagnostics

### Container Health

```bash
# Check container status
docker ps | grep samba-ad-dc

# Check logs
docker logs --tail 50 samba-ad-dc

# Check domain info
docker exec samba-ad-dc samba-tool domain info 127.0.0.1

# Check DNS
docker exec samba-ad-dc nslookup corp.local 127.0.0.1

# Test LDAP from inside container
docker exec samba-ad-dc ldapsearch -x -H ldap://127.0.0.1 \
  -D "cn=Administrator,cn=Users,dc=corp,dc=local" \
  -w "P@ssw0rd123!" \
  -b "dc=corp,dc=local" -LLL

# Database check
docker exec samba-ad-dc samba-tool dbcheck
```

### Port Check

```bash
# Check if ports are listening
sudo lsof -i :389   # LDAP
sudo lsof -i :636   # LDAPS
sudo lsof -i :88    # Kerberos
sudo lsof -i :445   # SMB

# Or use netstat
sudo netstat -tlnp | grep -E '389|636|88|445'
```

---

## 🔧 Spring Boot Integration

### application.yml

```yaml
spring:
  ldap:
    urls: ldap://localhost:389
    base: dc=corp,dc=local
    username: cn=Administrator,cn=Users,dc=corp,dc=local
    password: P@ssw0rd123!

ra:
  ad:
    domain: corp.local
    user-search-base: cn=Users,dc=corp,dc=local
    user-search-filter: (sAMAccountName={0})
    group-search-base: cn=Users,dc=corp,dc=local
    group-search-filter: (member={0})

  role-mapping:
    - ad-group: cn=PKI-RA-Admins,cn=Users,dc=corp,dc=local
      app-role: RA_ADMIN
    - ad-group: cn=PKI-RA-Officers,cn=Users,dc=corp,dc=local
      app-role: RA_OFFICER
    - ad-group: cn=PKI-RA-Operators,cn=Users,dc=corp,dc=local
      app-role: RA_OPERATOR
    - ad-group: cn=PKI-Auditors,cn=Users,dc=corp,dc=local
      app-role: AUDITOR
```

---

## 📝 Setup Script for Test Users

Save as `create-test-users.sh`:

```bash
#!/bin/bash

docker exec samba-ad-dc bash -c "
# Create users
samba-tool user create jdoe Password@123 --given-name=John --surname=Doe --mail-address=john.doe@corp.local
samba-tool user create alice Password@123 --given-name=Alice --surname=Smith --mail-address=alice@corp.local
samba-tool user create bobadmin Password@123 --given-name=Bob --surname=Admin --mail-address=bob.admin@corp.local

# Create groups
samba-tool group add 'PKI-RA-Admins' --description='RA Administrators'
samba-tool group add 'PKI-RA-Officers' --description='RA Officers'
samba-tool group add 'PKI-RA-Operators' --description='RA Operators'
samba-tool group add 'PKI-Auditors' --description='PKI Auditors'

# Add users to groups
samba-tool group addmembers 'PKI-RA-Admins' bobadmin
samba-tool group addmembers 'PKI-RA-Officers' alice
samba-tool group addmembers 'PKI-RA-Operators' jdoe

echo 'Test users and groups created successfully!'
samba-tool user list
"
```

Run with:
```bash
chmod +x create-test-users.sh
./create-test-users.sh
```

---

## 🧪 Test Users (after setup script)

| Username | Password | DN | Groups |
|----------|----------|-----|--------|
| Administrator | P@ssw0rd123! | cn=Administrator,cn=Users,dc=corp,dc=local | Domain Admins |
| jdoe | Password@123 | cn=John Doe,cn=Users,dc=corp,dc=local | PKI-RA-Operators |
| alice | Password@123 | cn=Alice Smith,cn=Users,dc=corp,dc=local | PKI-RA-Officers |
| bobadmin | Password@123 | cn=Bob Admin,cn=Users,dc=corp,dc=local | PKI-RA-Admins |

---

## ⚠️ Common Errors

### Error: "Transport encryption required"
**Solution**: Already fixed with `INSECURELDAP=true` in ad-docker-compose.yml

### Error: "Can't contact LDAP server"
**Solutions**:
```bash
# 1. Check container is running
docker ps | grep samba-ad-dc

# 2. Wait for initialization (2-3 minutes)
docker logs -f samba-ad-dc

# 3. Test from container
docker exec samba-ad-dc ldapsearch -x -H ldap://127.0.0.1 \
  -b "dc=corp,dc=local"

# 4. Check firewall
sudo ufw allow 389/tcp
sudo ufw allow 636/tcp
```

### Error: "Invalid credentials"
**Solutions**:
```bash
# Reset Administrator password
docker exec -it samba-ad-dc samba-tool user setpassword Administrator

# Check DN format (must include cn=Users)
# ✅ Correct: cn=Administrator,cn=Users,dc=corp,dc=local
# ❌ Wrong: cn=Administrator,dc=corp,dc=local
```

---

## 🔐 Security Notes

### Development Settings (Current)
- `INSECURELDAP=true` - Allows unencrypted LDAP (port 389)
- `NOCOMPLEXITY=true` - Allows simple passwords

### Production Settings (Change before deployment)
```yaml
environment:
  - INSECURELDAP=false     # Require LDAPS (port 636)
  - NOCOMPLEXITY=false     # Enforce password complexity
  - DOMAINPASS=ComplexP@ssw0rd123!  # Strong password
```

Then use only LDAPS:
```yaml
spring:
  ldap:
    urls: ldaps://localhost:636  # Use secure port
```

---

## 📚 Additional Resources

- **Samba Wiki**: https://wiki.samba.org/
- **Docker Image**: https://hub.docker.com/r/nowsci/samba-domain
- **LDAP Tools**: `apt-get install ldap-utils`
- **GUI Tool**: Apache Directory Studio

---

**Document Status**: Ready for Development
**Last Updated**: 2026-01-16
