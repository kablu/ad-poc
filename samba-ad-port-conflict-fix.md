# Samba AD Port Conflict Resolution Guide
## Fixing "Port 53 Already in Use" Error

**Date**: 2026-01-15
**Issue**: Docker cannot bind to port 53 (DNS) because it's already in use by system services

---

## Understanding the Issue

### What's Happening?

When you try to start the Samba AD container, you see:
```
Error response from daemon: failed to set up container networking:
driver failed programming external connectivity on endpoint samba-ad-dc:
failed to bind host port 0.0.0.0:53/tcp: address already in use
```

**Cause**: Port 53 is typically used by:
- **systemd-resolved** (Ubuntu 18.04+) - System DNS resolver
- **dnsmasq** - Lightweight DNS/DHCP server
- **bind9** - Full-featured DNS server
- **NetworkManager** - Network management daemon

---

## Solution Options

### ✅ Option 1: Use LDAP Only (Recommended for RA Application)

**Best for**: Development and RA application testing

The RA application only needs **LDAP ports (389, 636)**, not DNS. The updated `docker-compose-samba-ad.yml` already implements this.

**What's Changed**:
- ✅ DNS ports (53) are commented out
- ✅ LDAP ports (389, 636) remain active
- ✅ Optional ports (Kerberos, SMB) are commented out

**No additional action needed!** Just run:
```bash
docker compose -f docker-compose-samba-ad.yml up -d
```

**Note**: DNS still works **inside** the container and between containers on the same network. Only host-to-container DNS is disabled.

---

### Option 2: Disable systemd-resolved (If You Need DNS Port 53)

**Best for**: Testing full Active Directory features including DNS

#### On Ubuntu/Debian:

```bash
# 1. Stop systemd-resolved
sudo systemctl stop systemd-resolved

# 2. Disable systemd-resolved from starting on boot
sudo systemctl disable systemd-resolved

# 3. Remove symlink to systemd-resolved's stub resolver
sudo rm /etc/resolv.conf

# 4. Create new resolv.conf with public DNS servers
cat <<EOF | sudo tee /etc/resolv.conf
nameserver 8.8.8.8
nameserver 8.8.4.4
nameserver 1.1.1.1
EOF

# 5. Make resolv.conf immutable (prevent systemd from overwriting)
sudo chattr +i /etc/resolv.conf

# 6. Verify DNS still works
nslookup google.com

# 7. Uncomment DNS ports in docker-compose-samba-ad.yml
# Edit lines 29-30 to:
# - "53:53/tcp"
# - "53:53/udp"

# 8. Start Samba AD
docker compose -f docker-compose-samba-ad.yml up -d
```

**Revert if needed**:
```bash
# Make resolv.conf mutable again
sudo chattr -i /etc/resolv.conf

# Re-enable systemd-resolved
sudo systemctl enable systemd-resolved
sudo systemctl start systemd-resolved

# Restore symlink
sudo ln -sf /run/systemd/resolve/stub-resolv.conf /etc/resolv.conf
```

---

### Option 3: Use Alternative DNS Ports

**Best for**: Keeping both system DNS and Samba AD DNS

Map Samba AD DNS to different host ports:

**Edit docker-compose-samba-ad.yml**:
```yaml
ports:
  # DNS on alternative ports
  - "5353:53/tcp"    # DNS on port 5353 instead of 53
  - "5353:53/udp"

  # LDAP (Required for RA application)
  - "389:389/tcp"
  - "636:636/tcp"
```

**Access Samba DNS**:
```bash
# Query Samba DNS on port 5353
nslookup -port=5353 sambaad.local 127.0.0.1
dig @127.0.0.1 -p 5353 sambaad.local

# LDAP still works on standard ports
ldapsearch -x -H ldap://localhost:389 -b "dc=sambaad,dc=local"
```

---

### Option 4: Stop Conflicting Services

**Find what's using port 53**:

```bash
# Check what's using port 53
sudo lsof -i :53

# Or use netstat
sudo netstat -tlnp | grep :53

# Or use ss
sudo ss -tlnp | grep :53
```

**Common outputs and solutions**:

#### If `systemd-resolved`:
```bash
# Output: systemd-resolve 1234 systemd-resolve 13u IPv4 ...

# Solution: Use Option 2 above
sudo systemctl stop systemd-resolved
```

#### If `dnsmasq`:
```bash
# Output: dnsmasq 5678 nobody ...

# Stop dnsmasq
sudo systemctl stop dnsmasq
sudo systemctl disable dnsmasq
```

#### If `bind9`:
```bash
# Output: named 9012 bind ...

# Stop bind9
sudo systemctl stop bind9
sudo systemctl disable bind9
```

#### If `NetworkManager`:
```bash
# Edit NetworkManager config
sudo nano /etc/NetworkManager/NetworkManager.conf

# Add under [main] section:
dns=none

# Restart NetworkManager
sudo systemctl restart NetworkManager
```

---

## Verification

### Check Port Availability

**Before starting Samba AD**:
```bash
# Check if port 53 is free
sudo lsof -i :53

# If output is empty, port 53 is available

# Check LDAP ports (389, 636)
sudo lsof -i :389
sudo lsof -i :636

# Should be empty if available
```

### Test Samba AD After Starting

```bash
# Start Samba AD
docker compose -f docker-compose-samba-ad.yml up -d

# Wait 2-3 minutes for initialization
docker logs -f samba-ad-dc

# Test LDAP connection (most important for RA app)
ldapsearch -x -H ldap://localhost:389 \
  -D "cn=Administrator,cn=Users,dc=sambaad,dc=local" \
  -w CompanyAdmin@123 \
  -b "dc=sambaad,dc=local" "(objectClass=user)"

# Test DNS (if port 53 is exposed)
nslookup sambaad.local localhost

# Test from within container (always works)
docker exec samba-ad-dc nslookup sambaad.local 127.0.0.1
```

---

## Recommended Configuration for RA Application

### What You Need:

| Port | Service | Required for RA? | Recommendation |
|------|---------|------------------|----------------|
| **389** | LDAP | ✅ **YES** | **Must expose** |
| **636** | LDAPS | ✅ **YES** | **Must expose** |
| **53** | DNS | ❌ No | Comment out (avoid conflict) |
| **88** | Kerberos | ❌ No | Comment out (optional) |
| **445** | SMB | ❌ No | Comment out (optional) |

### Minimal Configuration:

Your updated `docker-compose-samba-ad.yml` already has this:

```yaml
ports:
  # LDAP (Required)
  - "389:389/tcp"

  # LDAPS (Required)
  - "636:636/tcp"

  # DNS, Kerberos, SMB (Commented out)
```

### Test Your RA Application:

```bash
# 1. Start Samba AD
docker compose -f docker-compose-samba-ad.yml up -d

# 2. Verify LDAP connectivity
ldapsearch -x -H ldap://localhost:389 -b "dc=sambaad,dc=local"

# 3. Configure your RA application (application.yml)
spring:
  ldap:
    urls: ldap://localhost:389
    base: dc=sambaad,dc=local

# 4. Your RA app connects successfully!
```

---

## Troubleshooting After Changes

### Issue: DNS Resolution Broken on Host

**Symptom**: Can't resolve domain names after disabling systemd-resolved

**Solution**:
```bash
# Check /etc/resolv.conf
cat /etc/resolv.conf

# Should contain:
nameserver 8.8.8.8
nameserver 8.8.4.4

# If empty, recreate:
cat <<EOF | sudo tee /etc/resolv.conf
nameserver 8.8.8.8
nameserver 1.1.1.1
EOF

# Test
ping google.com
```

### Issue: LDAP Still Won't Connect

**Symptom**: Connection refused on port 389

**Solution**:
```bash
# 1. Check container is running
docker ps | grep samba-ad

# 2. Check port is listening
docker exec samba-ad-dc netstat -tlnp | grep 389

# 3. Check firewall
sudo ufw status
sudo ufw allow 389/tcp
sudo ufw allow 636/tcp

# 4. Check from container
docker exec samba-ad-dc ldapsearch -x -H ldap://127.0.0.1 -b "dc=sambaad,dc=local"

# 5. Check Docker network
docker network inspect ra-web_ad-network
```

### Issue: Container Exits Immediately

**Symptom**: `samba-ad-dc` container stops right after starting

**Solution**:
```bash
# Check logs
docker logs samba-ad-dc

# Common causes:
# 1. Port still in use (check with lsof)
# 2. Corrupted volumes (remove and recreate)
docker compose -f docker-compose-samba-ad.yml down -v
docker compose -f docker-compose-samba-ad.yml up -d

# 3. Permission issues
docker exec -it samba-ad-dc bash
chown -R root:root /var/lib/samba
exit
docker restart samba-ad-dc
```

---

## Quick Reference

### Start Samba AD (with current config)
```bash
docker compose -f docker-compose-samba-ad.yml up -d
```

### Test LDAP Connection
```bash
ldapsearch -x -H ldap://localhost:389 \
  -D "cn=Administrator,cn=Users,dc=sambaad,dc=local" \
  -w CompanyAdmin@123 \
  -b "dc=sambaad,dc=local"
```

### Check Port Usage
```bash
sudo lsof -i :53    # DNS
sudo lsof -i :389   # LDAP
sudo lsof -i :636   # LDAPS
```

### View Samba AD Logs
```bash
docker logs -f samba-ad-dc
```

### Stop Samba AD
```bash
docker compose -f docker-compose-samba-ad.yml down
```

### Complete Cleanup
```bash
docker compose -f docker-compose-samba-ad.yml down -v
```

---

## Summary

✅ **Recommended Solution**: Use the updated `docker-compose-samba-ad.yml` with DNS ports commented out

**Why?**
- ✅ No conflicts with system DNS (port 53)
- ✅ LDAP works perfectly for RA application
- ✅ Simpler setup, fewer issues
- ✅ DNS still works between containers
- ✅ No need to modify system services

**What You Lose**:
- ❌ Cannot query Samba DNS from host (rarely needed)
- ❌ Cannot use Samba as primary DNS (not needed for RA app)

**What You Keep**:
- ✅ Full LDAP/LDAPS functionality (main requirement)
- ✅ User authentication against AD
- ✅ Group membership queries
- ✅ All RA application features work

**Next Steps**:
1. Run `docker compose -f docker-compose-samba-ad.yml up -d`
2. Test LDAP connection
3. Integrate with RA application
4. Create users and groups as needed

---

**Document End**

**Status**: Issue Resolved ✅
**Configuration**: Production-ready for RA development
