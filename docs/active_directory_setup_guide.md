# Active Directory Setup Guide for Windows Server

## Document Information
- **Version:** 1.0
- **Last Updated:** 2026-01-21
- **Purpose:** Complete step-by-step guide to install and configure Active Directory Domain Services
- **Audience:** System administrators, IT professionals

---

## Table of Contents
1. [Prerequisites](#prerequisites)
2. [Installing Active Directory Domain Services](#installing-active-directory-domain-services)
3. [Promoting Server to Domain Controller](#promoting-server-to-domain-controller)
4. [Configuring DNS](#configuring-dns)
5. [Creating Organizational Units (OUs)](#creating-organizational-units-ous)
6. [Creating User Accounts](#creating-user-accounts)
7. [Creating Security Groups](#creating-security-groups)
8. [Configuring Group Policies](#configuring-group-policies)
9. [Joining Client Computers to Domain](#joining-client-computers-to-domain)
10. [Verification and Testing](#verification-and-testing)
11. [Best Practices](#best-practices)
12. [Troubleshooting](#troubleshooting)

---

## Prerequisites

### Hardware Requirements:

| Component | Minimum | Recommended |
|-----------|---------|-------------|
| **Processor** | 1.4 GHz 64-bit | 2.0 GHz or faster |
| **RAM** | 2 GB | 4 GB or more |
| **Disk Space** | 32 GB | 60 GB or more |
| **Network** | 1 Gbps | 1 Gbps or faster |

### Software Requirements:

- ✅ Windows Server 2016, 2019, or 2022
- ✅ Static IP address configured
- ✅ Proper DNS configuration
- ✅ Administrator privileges
- ✅ Valid Windows Server license

### Network Configuration:

Before starting, ensure:
1. **Static IP Address**: Server must have a static IP (not DHCP)
2. **DNS Configuration**: Server should point to itself or another DNS server
3. **Firewall**: Required ports open (if firewall enabled)
4. **Time Synchronization**: Correct time and time zone

---

## Installing Active Directory Domain Services

### Step 1: Open Server Manager

1. **Login to Windows Server**
   - Use Administrator credentials
   - Password: Your admin password

2. **Launch Server Manager**
   - Server Manager opens automatically on login
   - If not open: Click **Start** → **Server Manager**
   - Wait for Server Manager to fully load

   ```
   ┌─────────────────────────────────────────────────┐
   │ Server Manager                          [_][□][X]│
   ├─────────────────────────────────────────────────┤
   │ Dashboard | Local Server | All Servers | ...    │
   ├─────────────────────────────────────────────────┤
   │                                                  │
   │  Welcome to Server Manager                       │
   │                                                  │
   │  Configure this local server                     │
   │  Add roles and features                          │
   │  Add other servers to manage                     │
   │  Create a server group                           │
   │                                                  │
   └──────────────────────────────────────────────────┘
   ```

---

### Step 2: Add Roles and Features

1. **Start the Wizard**
   - Click **Manage** (top right corner)
   - Select **Add Roles and Features**

   ```
   Server Manager → Manage → Add Roles and Features
   ```

2. **Before You Begin Screen**
   ```
   ┌────────────────────────────────────────────────┐
   │ Add Roles and Features Wizard                  │
   ├────────────────────────────────────────────────┤
   │ Before You Begin                               │
   │                                                │
   │ This wizard helps you install roles, role      │
   │ services, or features.                         │
   │                                                │
   │ ☑ Skip this page by default                    │
   │                                                │
   │            [< Previous] [Next >] [Cancel]      │
   └────────────────────────────────────────────────┘
   ```
   - Click **Next**

3. **Installation Type**
   ```
   ┌────────────────────────────────────────────────┐
   │ Select Installation Type                       │
   ├────────────────────────────────────────────────┤
   │                                                │
   │ (•) Role-based or feature-based installation   │
   │     Configure a single server by adding roles, │
   │     role services, and features.               │
   │                                                │
   │ ( ) Remote Desktop Services installation       │
   │     Install required role services for Virtual │
   │     Desktop Infrastructure (VDI).              │
   │                                                │
   │            [< Previous] [Next >] [Cancel]      │
   └────────────────────────────────────────────────┘
   ```
   - Select **Role-based or feature-based installation**
   - Click **Next**

4. **Server Selection**
   ```
   ┌────────────────────────────────────────────────┐
   │ Select Destination Server                      │
   ├────────────────────────────────────────────────┤
   │                                                │
   │ (•) Select a server from the server pool       │
   │                                                │
   │ Server Pool:                                   │
   │ ┌────────────────────────────────────────────┐ │
   │ │☑ WIN-SERVER01    192.168.1.10  Online     │ │
   │ └────────────────────────────────────────────┘ │
   │                                                │
   │ ( ) Select a virtual hard disk                 │
   │                                                │
   │            [< Previous] [Next >] [Cancel]      │
   └────────────────────────────────────────────────┘
   ```
   - Your server should be selected by default
   - Click **Next**

---

### Step 3: Select Active Directory Domain Services Role

1. **Server Roles Screen**
   ```
   ┌────────────────────────────────────────────────┐
   │ Select Server Roles                            │
   ├────────────────────────────────────────────────┤
   │                                                │
   │ Roles:                                         │
   │ ☐ Active Directory Certificate Services        │
   │ ☑ Active Directory Domain Services      ← CHECK│
   │ ☐ Active Directory Federation Services         │
   │ ☐ Active Directory Lightweight Directory...    │
   │ ☐ Active Directory Rights Management...        │
   │ ☐ DHCP Server                                  │
   │ ☐ DNS Server                                   │
   │ ☐ File and Storage Services                    │
   │ ☐ Hyper-V                                      │
   │ ☐ Print and Document Services                  │
   │ ☐ Remote Access                                │
   │ ☐ Web Server (IIS)                             │
   │                                                │
   │            [< Previous] [Next >] [Cancel]      │
   └────────────────────────────────────────────────┘
   ```
   - **Check the box**: ☑ **Active Directory Domain Services**
   - A popup will appear

2. **Add Features Dialog**
   ```
   ┌────────────────────────────────────────────────┐
   │ Add features that are required for Active      │
   │ Directory Domain Services?                     │
   ├────────────────────────────────────────────────┤
   │                                                │
   │ The following tools are required to manage     │
   │ this feature, but do not have to be installed  │
   │ on the same server.                            │
   │                                                │
   │ ☑ Include management tools (if applicable)     │
   │                                                │
   │ Features to be added:                          │
   │   • Group Policy Management                    │
   │   • Remote Server Administration Tools         │
   │     - Role Administration Tools                │
   │     - AD DS and AD LDS Tools                   │
   │       - Active Directory module for PowerShell │
   │       - AD DS Tools                            │
   │         • AD DS Snap-Ins and Command Tools     │
   │                                                │
   │            [Add Features]        [Cancel]      │
   └────────────────────────────────────────────────┘
   ```
   - Keep **Include management tools** checked ✓
   - Click **Add Features**

3. **Back to Server Roles**
   - You'll see ☑ Active Directory Domain Services is now checked
   - Click **Next**

---

### Step 4: Select Features

```
┌────────────────────────────────────────────────┐
│ Select Features                                │
├────────────────────────────────────────────────┤
│                                                │
│ Features:                                      │
│ ☐ .NET Framework 3.5 Features                  │
│ ☑ .NET Framework 4.8 Features                  │
│ ☐ Background Intelligent Transfer Service      │
│ ☐ BitLocker Drive Encryption                   │
│ ☑ Group Policy Management           (added)    │
│ ☐ IP Address Management (IPAM) Server         │
│ ☐ Remote Server Administration Tools  (added)  │
│ ☐ Windows PowerShell                           │
│ ☐ Windows Server Backup                        │
│                                                │
│            [< Previous] [Next >] [Cancel]      │
└────────────────────────────────────────────────┘
```
- Features are automatically selected based on AD DS requirements
- Click **Next**

---

### Step 5: Active Directory Domain Services Information

```
┌────────────────────────────────────────────────┐
│ Active Directory Domain Services               │
├────────────────────────────────────────────────┤
│                                                │
│ Things to Note:                                │
│                                                │
│ • The server will need to be restarted after   │
│   installation                                 │
│                                                │
│ • You cannot uninstall AD DS if the server is  │
│   a domain controller                          │
│                                                │
│ • After installing AD DS, you must promote     │
│   this server to a domain controller           │
│                                                │
│                          [More information...] │
│                                                │
│            [< Previous] [Next >] [Cancel]      │
└────────────────────────────────────────────────┘
```
- Read the information
- Click **Next**

---

### Step 6: Confirmation

```
┌────────────────────────────────────────────────┐
│ Confirm installation selections                │
├────────────────────────────────────────────────┤
│                                                │
│ To install the following roles, role services, │
│ or features, click Install.                    │
│                                                │
│ Roles and Features:                            │
│   • Active Directory Domain Services           │
│     - AD DS Tools                              │
│     - Group Policy Management                  │
│                                                │
│ ☐ Restart the destination server automatically │
│   if required                                  │
│                                                │
│ ☐ Export configuration settings                │
│   [Specify an alternate source path]           │
│                                                │
│            [< Previous] [Install] [Cancel]     │
└────────────────────────────────────────────────┘
```
- **Optional**: Check **Restart the destination server automatically**
- Click **Install**

---

### Step 7: Installation Progress

```
┌────────────────────────────────────────────────┐
│ Installation Progress                          │
├────────────────────────────────────────────────┤
│                                                │
│ Installing:                                    │
│ [████████████████████░░░░░░░░░░] 75%          │
│                                                │
│ Active Directory Domain Services               │
│ Feature installation                           │
│   • Copying files...                           │
│   • Installing components...                   │
│   • Configuring features...                    │
│                                                │
│ This may take several minutes...               │
│                                                │
│            [< Previous] [Close] [Cancel]       │
└────────────────────────────────────────────────┘
```
- Wait for installation to complete (2-5 minutes)
- Do NOT close the window

---

### Step 8: Installation Complete

```
┌────────────────────────────────────────────────┐
│ Installation Progress                          │
├────────────────────────────────────────────────┤
│                                                │
│ ✓ Installation succeeded                       │
│                                                │
│ Active Directory Domain Services               │
│   ✓ Role installation complete                 │
│                                                │
│ Configuration required. Installation succeeded │
│ on WIN-SERVER01.                               │
│                                                │
│ Promote this server to a domain controller     │
│                                                │
│            [Close] [Promote this server to...] │
└────────────────────────────────────────────────┘
```
- ✓ Installation succeeded!
- **DO NOT CLOSE** yet
- Click **Promote this server to a domain controller**

**Note**: If you closed the window by mistake, you can access promotion later:
- Server Manager → Flag icon (top right, yellow triangle)
- Click "Promote this server to a domain controller"

---

## Promoting Server to Domain Controller

### Step 9: Deployment Configuration

```
┌────────────────────────────────────────────────┐
│ Active Directory Domain Services Configuration │
│ Wizard                                         │
├────────────────────────────────────────────────┤
│ Deployment Configuration                       │
│                                                │
│ Select the deployment operation:               │
│                                                │
│ (•) Add a new forest                           │
│     This server will become the first domain   │
│     controller in a new forest.                │
│                                                │
│ ( ) Add a new domain to an existing forest     │
│     Create a new child domain or new tree      │
│     domain in an existing forest.              │
│                                                │
│ ( ) Add a domain controller to an existing     │
│     domain                                     │
│     Join this server to an existing domain.    │
│                                                │
│ Root domain name:                              │
│ [company.local                        ]        │
│                                                │
│ Examples: contoso.com, corp.fabrikam.com       │
│                                                │
│            [< Previous] [Next >] [Cancel]      │
└────────────────────────────────────────────────┘
```

**Configuration Steps:**

1. **Select Deployment Type**
   - Select **(•) Add a new forest**
   - This creates a brand new Active Directory forest

2. **Enter Root Domain Name**
   - **Format**: `company.local` or `company.com`
   - **Examples**:
     - `company.local` (internal only)
     - `corp.company.com` (subdomain)
     - `company.com` (if you own the domain)

   **Naming Considerations**:
   - **Use `.local` for internal domains** (recommended)
   - Avoid using a public domain you own (causes issues)
   - Use lowercase letters
   - No spaces or special characters
   - Cannot be changed later!

   **Example**: `company.local`

3. Click **Next**

---

### Step 10: Domain Controller Options

```
┌────────────────────────────────────────────────┐
│ Domain Controller Options                      │
├────────────────────────────────────────────────┤
│                                                │
│ Select domain and forest functional levels:    │
│                                                │
│ Forest functional level:                       │
│ [Windows Server 2016               ▼]         │
│                                                │
│ Domain functional level:                       │
│ [Windows Server 2016               ▼]         │
│                                                │
│ Specify domain controller capabilities:        │
│ ☑ Domain Name System (DNS) server             │
│ ☑ Global Catalog (GC)                          │
│ ☐ Read only domain controller (RODC)          │
│                                                │
│ Type the Directory Services Restore Mode       │
│ (DSRM) password:                               │
│                                                │
│ Password:     [********************]           │
│ Confirm:      [********************]           │
│                                                │
│            [< Previous] [Next >] [Cancel]      │
└────────────────────────────────────────────────┘
```

**Configuration Steps:**

1. **Forest Functional Level**
   - Select: **Windows Server 2016** (or latest available)
   - Higher = more features, but requires all DCs to be that version
   - **Recommended**: Match your Windows Server version

2. **Domain Functional Level**
   - Select: **Windows Server 2016** (same as forest level)
   - Must be equal or lower than forest level

3. **Domain Controller Capabilities**
   - ☑ **Domain Name System (DNS) server** - KEEP CHECKED
     - This server will also be a DNS server (required for AD)
   - ☑ **Global Catalog (GC)** - KEEP CHECKED
     - First DC must be a Global Catalog
   - ☐ **Read only domain controller (RODC)** - LEAVE UNCHECKED
     - Only used for branch offices

4. **DSRM Password**
   - **Directory Services Restore Mode password**
   - Used for recovery/troubleshooting
   - **Enter a strong password**: Example: `P@ssw0rd123!`
   - **Confirm the password**
   - **WRITE IT DOWN SECURELY** - You'll need this for disaster recovery

   **Password Requirements**:
   - At least 8 characters
   - Mix of uppercase, lowercase, numbers, symbols
   - Different from Administrator password (recommended)

5. Click **Next**

---

### Step 11: DNS Options

```
┌────────────────────────────────────────────────┐
│ DNS Options                                    │
├────────────────────────────────────────────────┤
│                                                │
│ ⚠ A delegation for this DNS server cannot be  │
│   created because the authoritative parent    │
│   zone cannot be found or it does not run     │
│   Windows DNS server. If you are integrating  │
│   with an existing DNS infrastructure, you    │
│   should manually create a delegation to this │
│   DNS server in the parent zone to ensure     │
│   reliable name resolution from outside the   │
│   domain "company.local". Otherwise, no action │
│   is required.                                 │
│                                                │
│ This warning is normal for the first DC in a  │
│ new forest. You can safely ignore it.         │
│                                                │
│            [< Previous] [Next >] [Cancel]      │
└────────────────────────────────────────────────┘
```

**What This Means**:
- ⚠ Warning message is **NORMAL** and **EXPECTED**
- You're creating the first DNS server for this domain
- There's no parent DNS zone yet (because it's a new forest)
- **No action needed**

**Action**:
- Read the warning (it's informational)
- Click **Next**

---

### Step 12: Additional Options

```
┌────────────────────────────────────────────────┐
│ Additional Options                             │
├────────────────────────────────────────────────┤
│                                                │
│ The NetBIOS domain name:                       │
│                                                │
│ [COMPANY                         ]             │
│                                                │
│ The NetBIOS name is used by older operating   │
│ systems and applications to identify the       │
│ domain.                                        │
│                                                │
│ The NetBIOS name is automatically generated    │
│ from the DNS domain name unless you specify    │
│ a different name.                              │
│                                                │
│            [< Previous] [Next >] [Cancel]      │
└────────────────────────────────────────────────┘
```

**Configuration**:

1. **NetBIOS Domain Name**
   - Automatically generated from your domain name
   - Example: `company.local` → `COMPANY`
   - Used by older systems (Windows 95/98/XP)

2. **Should You Change It?**
   - **Usually NO** - keep the default
   - Only change if you have specific naming requirements
   - Must be uppercase
   - Maximum 15 characters

3. Click **Next**

---

### Step 13: Paths

```
┌────────────────────────────────────────────────┐
│ Paths                                          │
├────────────────────────────────────────────────┤
│                                                │
│ Specify the location of the AD DS database,    │
│ log files, and SYSVOL folder.                  │
│                                                │
│ Database folder:                               │
│ [C:\Windows\NTDS               ]  [Browse...]  │
│                                                │
│ Log files folder:                              │
│ [C:\Windows\NTDS               ]  [Browse...]  │
│                                                │
│ SYSVOL folder:                                 │
│ [C:\Windows\SYSVOL             ]  [Browse...]  │
│                                                │
│ For better performance, store the database and │
│ logs on separate volumes.                      │
│                                                │
│            [< Previous] [Next >] [Cancel]      │
└────────────────────────────────────────────────┘
```

**Default Paths**:
- **Database folder**: `C:\Windows\NTDS`
  - Stores the Active Directory database (ntds.dit)
- **Log files folder**: `C:\Windows\NTDS`
  - Transaction logs for AD operations
- **SYSVOL folder**: `C:\Windows\SYSVOL`
  - Stores Group Policy and logon scripts

**Should You Change Paths?**

**For Production (Recommended)**:
- ✓ Place database on a separate drive (e.g., `D:\NTDS`)
- ✓ Place logs on another drive (e.g., `E:\NTDS\Logs`)
- ✓ Keep SYSVOL on C: (default is fine)
- **Benefits**: Better performance, easier backup

**For Testing/Lab (Default is OK)**:
- ✓ Keep default paths (all on C:)
- Simpler configuration
- Acceptable for test environments

**For This Guide (Testing)**:
- Keep default paths
- Click **Next**

---

### Step 14: Review Options

```
┌────────────────────────────────────────────────┐
│ Review Options                                 │
├────────────────────────────────────────────────┤
│                                                │
│ Review your selections:                        │
│                                                │
│ ┌────────────────────────────────────────────┐ │
│ │ Deployment Operation:                      │ │
│ │   Install a new forest                     │ │
│ │                                            │ │
│ │ Root domain name:                          │ │
│ │   company.local                            │ │
│ │                                            │ │
│ │ Forest functional level:                   │ │
│ │   Windows Server 2016                      │ │
│ │                                            │ │
│ │ Domain functional level:                   │ │
│ │   Windows Server 2016                      │ │
│ │                                            │ │
│ │ Additional Options:                        │ │
│ │   • Domain Name System (DNS) server: Yes   │ │
│ │   • Global Catalog: Yes                    │ │
│ │   • Read only domain controller: No        │ │
│ │                                            │ │
│ │ NetBIOS domain name: COMPANY               │ │
│ │                                            │ │
│ │ Paths:                                     │ │
│ │   Database: C:\Windows\NTDS                │ │
│ │   Log files: C:\Windows\NTDS               │ │
│ │   SYSVOL: C:\Windows\SYSVOL                │ │
│ └────────────────────────────────────────────┘ │
│                                                │
│ [View script]                                  │
│                                                │
│            [< Previous] [Next >] [Cancel]      │
└────────────────────────────────────────────────┘
```

**Review Your Settings**:
- ✓ Root domain: company.local
- ✓ DNS server: Yes
- ✓ Global Catalog: Yes
- ✓ NetBIOS name: COMPANY

**Optional: View PowerShell Script**
- Click **View script** to see PowerShell equivalent
- Useful for scripting multiple DC installations
- Can save for documentation

Click **Next**

---

### Step 15: Prerequisites Check

```
┌────────────────────────────────────────────────┐
│ Prerequisites Check                            │
├────────────────────────────────────────────────┤
│                                                │
│ All prerequisite checks passed successfully.   │
│ Click Install to begin installation.           │
│                                                │
│ ┌────────────────────────────────────────────┐ │
│ │ ✓ Checking Forest prerequisites            │ │
│ │ ✓ Checking Domain prerequisites            │ │
│ │ ✓ Checking DNS prerequisites               │ │
│ │ ✓ Verifying paths                          │ │
│ │ ✓ Verifying DNS delegation                 │ │
│ │ ✓ Verifying permissions                    │ │
│ │                                            │ │
│ │ ⚠ Some warnings detected (informational)   │ │
│ │   • Cryptography settings                  │ │
│ │   • Security settings                      │ │
│ └────────────────────────────────────────────┘ │
│                                                │
│ ⚠ This server will be restarted automatically │
│   after installation completes.                │
│                                                │
│            [< Previous] [Install] [Cancel]     │
└────────────────────────────────────────────────┘
```

**Prerequisites Check Results**:

✓ **Green Checks**: All required checks passed
⚠ **Yellow Warnings**: Informational, safe to proceed
❌ **Red Errors**: Must fix before continuing

**Common Warnings (Safe to Ignore)**:
- Cryptography settings: Legacy algorithm warnings
- Security settings: Older DC compatibility warnings
- DNS delegation: Expected for new forest

**If You See Red Errors**:
- Fix the issue before proceeding
- Common fixes:
  - Ensure static IP is configured
  - Check DNS settings
  - Verify Administrator privileges

**Action**:
- Review the checks
- Click **Install**

---

### Step 16: Installation Progress

```
┌────────────────────────────────────────────────┐
│ Installation                                   │
├────────────────────────────────────────────────┤
│                                                │
│ Configuring Active Directory Domain Services  │
│                                                │
│ [████████████████░░░░░░░░░░░░] 60%            │
│                                                │
│ Current operation:                             │
│ • Creating Active Directory database...        │
│                                                │
│ This operation may take several minutes...     │
│                                                │
│ Steps completed:                               │
│ ✓ Validating parameters                        │
│ ✓ Creating directory service objects           │
│ ✓ Creating AD database                         │
│ ⏳ Configuring DNS                              │
│ ⏳ Configuring security                         │
│ ⏳ Finalizing installation                      │
│                                                │
└────────────────────────────────────────────────┘
```

**Installation Process** (10-20 minutes):

**Phase 1: Preparation** (0-20%)
- Validating parameters
- Checking system requirements
- Preparing files

**Phase 2: Database Creation** (20-50%)
- Creating NTDS.dit database
- Initializing schema
- Creating directory partitions

**Phase 3: DNS Configuration** (50-70%)
- Installing DNS role
- Creating DNS zones
- Configuring DNS records

**Phase 4: Security Configuration** (70-85%)
- Configuring Kerberos
- Setting up security principals
- Creating default policies

**Phase 5: Finalization** (85-100%)
- Creating SYSVOL
- Replicating system files
- Completing configuration

**Do NOT**:
- ❌ Close the window
- ❌ Restart the server manually
- ❌ Cancel the process

**Wait patiently** - This is normal and takes time.

---

### Step 17: Installation Complete - Automatic Restart

```
┌────────────────────────────────────────────────┐
│ Installation                                   │
├────────────────────────────────────────────────┤
│                                                │
│ ✓ Configuration completed successfully         │
│                                                │
│ [████████████████████████████████] 100%        │
│                                                │
│ This server has been successfully configured   │
│ as a domain controller for domain:             │
│                                                │
│         company.local                          │
│                                                │
│ The server will restart automatically to       │
│ complete the installation.                     │
│                                                │
│ Restarting in 10 seconds...                    │
│                                                │
│            [Close] [Restart Now]               │
└────────────────────────────────────────────────┘
```

**Success!** 🎉

- ✓ Active Directory installed
- ✓ Domain controller configured
- ✓ DNS server installed
- ✓ Server will restart automatically

**Action**:
- Click **Close** or wait for automatic restart
- Server reboots in 10 seconds

---

### Step 18: Server Restart and First Login

After restart:

1. **Login Screen Changes**
   ```
   Before AD:
   Login as: Administrator

   After AD:
   Login as: COMPANY\Administrator
   ```

2. **Login Credentials**
   - **Username**: `COMPANY\Administrator`
     - Or: `Administrator@company.local`
   - **Password**: Your administrator password (same as before)

3. **Press Ctrl+Alt+Delete**
   - Enter credentials
   - Press Enter

4. **Wait for Login**
   - First login after DC promotion takes longer (normal)
   - Wait 1-2 minutes

---

## Configuring DNS

After AD installation, DNS is automatically configured, but let's verify:

### Step 19: Verify DNS Installation

1. **Open DNS Manager**
   ```
   Start → Windows Administrative Tools → DNS
   ```

   Or:
   ```
   Server Manager → Tools → DNS
   ```

2. **DNS Manager Window**
   ```
   ┌────────────────────────────────────────────────┐
   │ DNS Manager                         [_][□][X]  │
   ├────────────────────────────────────────────────┤
   │ ▼ WIN-SERVER01                                 │
   │   ├─ Forward Lookup Zones                      │
   │   │  ├─ company.local                          │
   │   │  │  ├─ _msdcs                              │
   │   │  │  ├─ _sites                              │
   │   │  │  ├─ _tcp                                │
   │   │  │  └─ _udp                                │
   │   │  └─ _msdcs.company.local                   │
   │   ├─ Reverse Lookup Zones                      │
   │   │  └─ (empty)                                │
   │   └─ Trust Points                              │
   │      └─ (empty)                                │
   └────────────────────────────────────────────────┘
   ```

3. **Verify Forward Lookup Zone**
   - Expand **Forward Lookup Zones**
   - You should see: **company.local**
   - This zone contains all domain records

4. **Check DNS Records**
   - Click on **company.local**
   - You should see records like:
     - `_ldap._tcp.company.local` (SRV record)
     - `_kerberos._tcp.company.local` (SRV record)
     - `gc._msdcs.company.local` (CNAME)
     - `WIN-SERVER01` (Host A record)

---

### Step 20: Configure DNS Forwarders (Optional but Recommended)

**What are Forwarders?**
- DNS servers your server asks when it can't resolve a name
- Example: To resolve www.google.com

**Configuration**:

1. **Right-click Your Server Name**
   ```
   DNS Manager
   ▼ WIN-SERVER01 (right-click)
      └─ Properties
   ```

2. **Forwarders Tab**
   ```
   ┌────────────────────────────────────────────┐
   │ WIN-SERVER01 Properties                    │
   ├────────────────────────────────────────────┤
   │ [Interfaces] [Forwarders] [Advanced] ...   │
   │                                            │
   │ Forwarders allow this DNS server to        │
   │ forward queries to other DNS servers.      │
   │                                            │
   │ IP addresses of forwarding servers:        │
   │ ┌────────────────────────────────────────┐ │
   │ │ 8.8.8.8                                │ │
   │ │ 8.8.4.4                                │ │
   │ └────────────────────────────────────────┘ │
   │                                            │
   │ [Edit...] [Remove]                         │
   │                                            │
   │        [OK] [Cancel] [Apply]               │
   └────────────────────────────────────────────┘
   ```

3. **Add Forwarders**
   - Click **Edit...**
   - Add DNS servers:
     - **8.8.8.8** (Google Public DNS)
     - **8.8.4.4** (Google Public DNS Secondary)
     - Or your ISP's DNS servers
   - Click **OK**

4. **Apply Changes**
   - Click **OK**
   - Forwarders are now configured

---

## Creating Organizational Units (OUs)

**What are OUs?**
- Containers for organizing users, groups, computers
- Used to apply Group Policies
- Like folders in a file system

### Step 21: Open Active Directory Users and Computers

1. **Launch AD Users and Computers**
   ```
   Start → Windows Administrative Tools → Active Directory Users and Computers
   ```

   Or:
   ```
   Server Manager → Tools → Active Directory Users and Computers
   ```

2. **AD Users and Computers Window**
   ```
   ┌────────────────────────────────────────────────┐
   │ Active Directory Users and Computers [_][□][X] │
   ├────────────────────────────────────────────────┤
   │ ▼ company.local                                │
   │   ├─ Builtin                                   │
   │   ├─ Computers                                 │
   │   ├─ Domain Controllers                        │
   │   ├─ ForeignSecurityPrincipals                 │
   │   ├─ Managed Service Accounts                  │
   │   ├─ Users                                     │
   │   └─ ...                                       │
   └────────────────────────────────────────────────┘
   ```

---

### Step 22: Create Organizational Units

**Recommended OU Structure**:
```
company.local
├─ Company
   ├─ Users
   │  ├─ Employees
   │  ├─ Contractors
   │  └─ ServiceAccounts
   ├─ Groups
   │  ├─ Security Groups
   │  └─ Distribution Groups
   ├─ Computers
   │  ├─ Desktops
   │  ├─ Laptops
   │  └─ Servers
   └─ Servers
```

**Creating the Main OU**:

1. **Right-click Domain**
   ```
   Right-click "company.local"
   └─ New → Organizational Unit
   ```

2. **New Object - Organizational Unit**
   ```
   ┌────────────────────────────────────────────┐
   │ New Object - Organizational Unit           │
   ├────────────────────────────────────────────┤
   │                                            │
   │ Name:                                      │
   │ [Company                         ]         │
   │                                            │
   │ ☑ Protect container from accidental       │
   │   deletion                                 │
   │                                            │
   │        [OK] [Cancel]                       │
   └────────────────────────────────────────────┘
   ```
   - **Name**: `Company`
   - ☑ **Protect from deletion** - Keep checked
   - Click **OK**

3. **Create Sub-OUs**

   **Create Users OU**:
   - Right-click **Company** → **New** → **Organizational Unit**
   - Name: `Users`
   - Click **OK**

   **Create Groups OU**:
   - Right-click **Company** → **New** → **Organizational Unit**
   - Name: `Groups`
   - Click **OK**

   **Create Computers OU**:
   - Right-click **Company** → **New** → **Organizational Unit**
   - Name: `Computers`
   - Click **OK**

4. **Final Structure**:
   ```
   ▼ company.local
     ├─ Builtin
     ├─ ▼ Company                    ← New OU
     │  ├─ Users                      ← New OU
     │  ├─ Groups                     ← New OU
     │  └─ Computers                  ← New OU
     ├─ Computers (built-in)
     ├─ Domain Controllers
     └─ Users (built-in)
   ```

---

## Creating User Accounts

### Step 23: Create User Accounts

1. **Navigate to Users OU**
   ```
   company.local → Company → Users (right-click)
   ```

2. **Create New User**
   ```
   Right-click "Users" OU
   └─ New → User
   ```

3. **New Object - User (Page 1)**
   ```
   ┌────────────────────────────────────────────────┐
   │ New Object - User                              │
   ├────────────────────────────────────────────────┤
   │                                                │
   │ Create in: company.local/Company/Users         │
   │                                                │
   │ First name:      [Kablu                  ]     │
   │ Initials:        [                       ]     │
   │ Last name:       [Khan                   ]     │
   │                                                │
   │ Full name:       [Kablu Khan             ]     │
   │                                                │
   │ User logon name:                               │
   │ [kablu           ] [@company.local      ▼]     │
   │                                                │
   │ User logon name (pre-Windows 2000):            │
   │ [COMPANY\kablu                          ]      │
   │                                                │
   │                    [Next >] [Cancel]           │
   └────────────────────────────────────────────────┘
   ```

   **Fill in the fields**:
   - **First name**: `Kablu`
   - **Last name**: `Khan`
   - **Full name**: Auto-fills to `Kablu Khan`
   - **User logon name**: `kablu`
     - Domain is automatically selected: `@company.local`
   - **Pre-Windows 2000**: Auto-fills to `COMPANY\kablu`

   Click **Next**

4. **New Object - User (Page 2 - Password)**
   ```
   ┌────────────────────────────────────────────────┐
   │ New Object - User                              │
   ├────────────────────────────────────────────────┤
   │                                                │
   │ Password:                                      │
   │ [********************              ]           │
   │                                                │
   │ Confirm password:                              │
   │ [********************              ]           │
   │                                                │
   │ Password options:                              │
   │ ☑ User must change password at next logon     │
   │ ☐ User cannot change password                 │
   │ ☐ Password never expires                      │
   │ ☐ Account is disabled                         │
   │                                                │
   │            [< Back] [Next >] [Cancel]          │
   └────────────────────────────────────────────────┘
   ```

   **Password Configuration**:
   - **Password**: Enter a strong password (e.g., `P@ssw0rd123!`)
   - **Confirm password**: Re-enter the same password

   **Password Options**:
   - ☑ **User must change password at next logon** - RECOMMENDED
     - Forces user to change password on first login
   - ☐ **User cannot change password** - Leave unchecked
     - Only check for service accounts
   - ☐ **Password never expires** - Leave unchecked
     - Only check for service accounts
   - ☐ **Account is disabled** - Leave unchecked

   Click **Next**

5. **New Object - User (Page 3 - Confirmation)**
   ```
   ┌────────────────────────────────────────────────┐
   │ New Object - User                              │
   ├────────────────────────────────────────────────┤
   │                                                │
   │ The following object will be created in        │
   │ company.local/Company/Users:                   │
   │                                                │
   │ Full name:       Kablu Khan                    │
   │ User logon name: kablu@company.local           │
   │                                                │
   │            [< Back] [Finish] [Cancel]          │
   └────────────────────────────────────────────────┘
   ```

   **Review and Confirm**:
   - Verify the information
   - Click **Finish**

6. **User Created Successfully**
   ```
   ✓ User "Kablu Khan" created successfully
   ```

   You'll see the new user in the Users OU:
   ```
   ▼ Company
     ▼ Users
       └─ Kablu Khan
   ```

---

### Step 24: Configure User Properties (Optional)

To add more details to the user account:

1. **Open User Properties**
   - Right-click **Kablu Khan** → **Properties**

2. **Properties Tabs**:

   **General Tab**:
   ```
   Description:    [Engineering Department     ]
   Office:         [Building A, Room 101       ]
   Telephone:      [+1-555-0100                ]
   Email:          [kablu@company.com          ]
   Web page:       [                           ]
   ```

   **Account Tab**:
   ```
   User logon name: kablu@company.local
   Logon hours:     [Logon Hours...] (restrict login times)
   Log On To:       [Log On To...] (restrict computers)
   Account expires: (•) Never
                    ( ) End of: [Date picker]
   ```

   **Profile Tab**:
   ```
   Profile path:    [\\server\profiles\%username%]
   Logon script:    [logon.bat                    ]
   Home folder:     [\\server\home\%username%     ]
   ```

   **Member Of Tab**:
   - Shows which groups the user belongs to
   - Add user to groups here

3. **Click OK** to save changes

---

### Step 25: Create Additional Users

**Quick Method** - Create multiple users:

1. **Copy Existing User**
   - Right-click existing user (e.g., Kablu Khan)
   - Select **Copy**
   - This copies group memberships and settings

2. **Enter New User Details**
   - First name, Last name, User logon name
   - Set password
   - Click Finish

**Example Users to Create**:
```
Users OU:
├─ Kablu Khan (kablu@company.local) - IT Administrator
├─ John Doe (john@company.local) - Engineering
├─ Jane Smith (jane@company.local) - Finance
└─ Admin User (admin@company.local) - Service Account
```

---

## Creating Security Groups

### Step 26: Create Security Groups

**What are Security Groups?**
- Collections of users with similar permissions
- Used for access control (files, folders, applications)
- Simplify permission management

**Common Groups**:
- IT Administrators
- Developers
- Finance Team
- All Employees
- VPN Users
- Certificate Users (for RA auto-enrollment!)

---

### Step 27: Create a Security Group

1. **Navigate to Groups OU**
   ```
   company.local → Company → Groups (right-click)
   ```

2. **Create New Group**
   ```
   Right-click "Groups" OU
   └─ New → Group
   ```

3. **New Object - Group**
   ```
   ┌────────────────────────────────────────────────┐
   │ New Object - Group                             │
   ├────────────────────────────────────────────────┤
   │                                                │
   │ Group name:                                    │
   │ [All Employees                        ]        │
   │                                                │
   │ Group name (pre-Windows 2000):                 │
   │ [All Employees                        ]        │
   │                                                │
   │ Group scope:                                   │
   │ ( ) Domain local                               │
   │ (•) Global                                     │
   │ ( ) Universal                                  │
   │                                                │
   │ Group type:                                    │
   │ (•) Security                                   │
   │ ( ) Distribution                               │
   │                                                │
   │                    [OK] [Cancel]               │
   └────────────────────────────────────────────────┘
   ```

   **Configuration**:
   - **Group name**: `All Employees`
   - **Group scope**: Select **(•) Global**
     - Global: Most common, can be used anywhere in domain
     - Domain Local: Used within single domain
     - Universal: Used across multiple domains
   - **Group type**: Select **(•) Security**
     - Security: For permissions and access control
     - Distribution: For email distribution lists only

   Click **OK**

4. **Create Additional Groups**

   Repeat for these groups:
   ```
   Groups OU:
   ├─ All Employees (Global, Security)
   ├─ IT Administrators (Global, Security)
   ├─ Developers (Global, Security)
   ├─ Finance Team (Global, Security)
   ├─ VPN Users (Global, Security)
   ├─ PKI-RA-Admins (Global, Security)          ← For RA system
   ├─ PKI-RA-Officers (Global, Security)        ← For RA system
   └─ PKI-RA-Operators (Global, Security)       ← For RA system
   ```

---

### Step 28: Add Users to Groups

1. **Open Group Properties**
   - Navigate to: `Company → Groups`
   - Right-click **All Employees** → **Properties**

2. **Members Tab**
   ```
   ┌────────────────────────────────────────────────┐
   │ All Employees Properties                       │
   ├────────────────────────────────────────────────┤
   │ [General] [Members] [Member Of] [Managed By]   │
   │                                                │
   │ Members:                                       │
   │ ┌────────────────────────────────────────────┐ │
   │ │ Name                    Active Dir...       │ │
   │ │ (empty)                                    │ │
   │ └────────────────────────────────────────────┘ │
   │                                                │
   │ [Add...] [Remove]                              │
   │                                                │
   │                    [OK] [Cancel] [Apply]       │
   └────────────────────────────────────────────────┘
   ```

3. **Add Members**
   - Click **Add...**

4. **Select Users**
   ```
   ┌────────────────────────────────────────────────┐
   │ Select Users, Contacts, Computers, Service...  │
   ├────────────────────────────────────────────────┤
   │                                                │
   │ Select this object type:                       │
   │ [Users, Groups, or Other objects]  [Object...] │
   │                                                │
   │ From this location:                            │
   │ [company.local                  ]  [Locations] │
   │                                                │
   │ Enter the object names to select:              │
   │ ┌────────────────────────────────────────────┐ │
   │ │ kablu; john; jane                          │ │
   │ └────────────────────────────────────────────┘ │
   │                                                │
   │ Examples: John Smith; jsmith@company.local     │
   │                                                │
   │            [Check Names] [Advanced...]         │
   │                                                │
   │                    [OK] [Cancel]               │
   └────────────────────────────────────────────────┘
   ```

   **Add Users**:
   - Type usernames separated by semicolons
   - Example: `kablu; john; jane`
   - Click **Check Names** (underlines valid names)
   - Click **OK**

5. **Verify Members Added**
   ```
   Members:
   ├─ Kablu Khan (kablu@company.local)
   ├─ John Doe (john@company.local)
   └─ Jane Smith (jane@company.local)
   ```

6. **Click OK** to save

---

### Step 29: Add User to Multiple Groups

**Alternative Method** - Add user to groups:

1. **Open User Properties**
   - Right-click **Kablu Khan** → **Properties**

2. **Member Of Tab**
   ```
   ┌────────────────────────────────────────────────┐
   │ Kablu Khan Properties                          │
   ├────────────────────────────────────────────────┤
   │ [General] [Account] [Profile] [Member Of] ...  │
   │                                                │
   │ Member of:                                     │
   │ ┌────────────────────────────────────────────┐ │
   │ │ Name                    Active Dir...       │ │
   │ │ Domain Users            company.local/Users │ │
   │ └────────────────────────────────────────────┘ │
   │                                                │
   │ [Add...] [Remove]                              │
   │                                                │
   │ Primary group: Domain Users                    │
   │                                                │
   │                    [OK] [Cancel] [Apply]       │
   └────────────────────────────────────────────────┘
   ```

3. **Add to Groups**
   - Click **Add...**
   - Enter group names: `All Employees; IT Administrators; PKI-RA-Admins`
   - Click **Check Names**
   - Click **OK**

4. **Result**:
   ```
   Member of:
   ├─ Domain Users (built-in)
   ├─ All Employees
   ├─ IT Administrators
   └─ PKI-RA-Admins
   ```

5. Click **OK** to save

---

## Configuring Group Policies

**What is Group Policy?**
- Centralized configuration management
- Apply settings to users and computers
- Examples: Password policy, desktop wallpaper, software installation

### Step 30: Open Group Policy Management

1. **Launch Group Policy Management**
   ```
   Start → Windows Administrative Tools → Group Policy Management
   ```

   Or:
   ```
   Server Manager → Tools → Group Policy Management
   ```

2. **Group Policy Management Console**
   ```
   ┌────────────────────────────────────────────────┐
   │ Group Policy Management             [_][□][X]  │
   ├────────────────────────────────────────────────┤
   │ ▼ Forest: company.local                        │
   │   ▼ Domains                                    │
   │     ▼ company.local                            │
   │       ├─ Domain Controllers                    │
   │       ├─ Company                               │
   │       ├─ Group Policy Objects                  │
   │       │  └─ Default Domain Policy              │
   │       ├─ WMI Filters                           │
   │       └─ Starter GPOs                          │
   └────────────────────────────────────────────────┘
   ```

---

### Step 31: Configure Default Domain Password Policy

1. **Edit Default Domain Policy**
   - Expand: `Domains → company.local`
   - Right-click **Default Domain Policy**
   - Select **Edit**

2. **Group Policy Management Editor**
   ```
   ┌────────────────────────────────────────────────┐
   │ Group Policy Management Editor      [_][□][X]  │
   ├────────────────────────────────────────────────┤
   │ ▼ Default Domain Policy [DC01.company.local]   │
   │   ├─ Computer Configuration                    │
   │   │  ├─ Policies                               │
   │   │  │  ├─ Software Settings                   │
   │   │  │  ├─ Windows Settings                    │
   │   │  │  │  ├─ Security Settings                │
   │   │  │  │  │  ├─ Account Policies              │
   │   │  │  │  │  │  ├─ Password Policy      ← HERE│
   │   │  │  │  │  │  ├─ Account Lockout Policy    │
   │   │  │  │  │  │  └─ Kerberos Policy           │
   └────────────────────────────────────────────────┘
   ```

3. **Navigate to Password Policy**
   ```
   Computer Configuration
   └─ Policies
      └─ Windows Settings
         └─ Security Settings
            └─ Account Policies
               └─ Password Policy (double-click)
   ```

4. **Configure Password Policy**

   **Enforce password history**:
   ```
   Double-click: Enforce password history
   [24] passwords remembered
   [Apply] [OK]
   ```
   - Prevents reusing recent passwords
   - Recommended: 24

   **Maximum password age**:
   ```
   Double-click: Maximum password age
   [90] days
   [Apply] [OK]
   ```
   - How often users must change password
   - Recommended: 90 days

   **Minimum password age**:
   ```
   Double-click: Minimum password age
   [1] days
   [Apply] [OK]
   ```
   - Prevents immediate password changes
   - Recommended: 1 day

   **Minimum password length**:
   ```
   Double-click: Minimum password length
   [8] characters
   [Apply] [OK]
   ```
   - Recommended: 8-14 characters

   **Password must meet complexity requirements**:
   ```
   Double-click: Password must meet complexity requirements
   (•) Enabled
   [Apply] [OK]
   ```
   - Requires: Uppercase + Lowercase + Number + Symbol
   - Recommended: Enabled

   **Store passwords using reversible encryption**:
   ```
   Double-click: Store passwords using reversible encryption
   ( ) Disabled
   [Apply] [OK]
   ```
   - Recommended: Disabled (security risk if enabled)

5. **Close Group Policy Editor**
   - File → Exit

---

### Step 32: Configure Account Lockout Policy

1. **Edit Default Domain Policy** (if not already open)

2. **Navigate to Account Lockout Policy**
   ```
   Computer Configuration
   └─ Policies
      └─ Windows Settings
         └─ Security Settings
            └─ Account Policies
               └─ Account Lockout Policy
   ```

3. **Configure Settings**:

   **Account lockout threshold**:
   ```
   Double-click: Account lockout threshold
   [5] invalid logon attempts
   [Apply] [OK]
   ```
   - Locks account after X failed attempts
   - Recommended: 5 attempts

   **Account lockout duration**:
   ```
   Double-click: Account lockout duration
   [30] minutes
   [Apply] [OK]
   ```
   - How long account stays locked
   - Recommended: 30 minutes

   **Reset account lockout counter after**:
   ```
   Double-click: Reset account lockout counter after
   [30] minutes
   [Apply] [OK]
   ```
   - When failed attempt counter resets
   - Recommended: Same as lockout duration

4. **Close Group Policy Editor**

---

### Step 33: Create Custom Group Policy

**Example**: Desktop wallpaper for all users

1. **Create New GPO**
   - In Group Policy Management Console
   - Right-click **Group Policy Objects**
   - Select **New**

2. **New GPO Dialog**
   ```
   ┌────────────────────────────────────────────┐
   │ New GPO                                    │
   ├────────────────────────────────────────────┤
   │                                            │
   │ Name:                                      │
   │ [Company Desktop Settings         ]        │
   │                                            │
   │ Source Starter GPO:                        │
   │ [(none)                          ▼]        │
   │                                            │
   │                [OK] [Cancel]               │
   └────────────────────────────────────────────┘
   ```
   - Name: `Company Desktop Settings`
   - Click **OK**

3. **Link GPO to OU**
   - Right-click **Company** OU
   - Select **Link an Existing GPO...**
   - Select **Company Desktop Settings**
   - Click **OK**

4. **Edit the GPO**
   - Right-click **Company Desktop Settings**
   - Select **Edit**

5. **Configure Desktop Wallpaper**
   ```
   User Configuration
   └─ Policies
      └─ Administrative Templates
         └─ Desktop
            └─ Desktop
               └─ Desktop Wallpaper (double-click)
   ```

   ```
   ┌────────────────────────────────────────────┐
   │ Desktop Wallpaper                          │
   ├────────────────────────────────────────────┤
   │                                            │
   │ ( ) Not Configured                         │
   │ (•) Enabled                                │
   │ ( ) Disabled                               │
   │                                            │
   │ Wallpaper Name:                            │
   │ [\\server\share\wallpaper.jpg    ]         │
   │                                            │
   │ Wallpaper Style:                           │
   │ [Fill                            ▼]        │
   │                                            │
   │                [OK] [Cancel] [Apply]       │
   └────────────────────────────────────────────┘
   ```
   - Select **(•) Enabled**
   - Enter wallpaper path: `\\server\share\wallpaper.jpg`
   - Choose style: **Fill**
   - Click **OK**

6. **Close Group Policy Editor**

---

### Step 34: Force Group Policy Update

**On Domain Controller**:
```powershell
gpupdate /force
```

**On Client Computer** (after joining domain):
```powershell
gpupdate /force
```

Or wait for automatic update (90 minutes + random offset)

---

## Joining Client Computers to Domain

### Step 35: Configure Client Computer Network

**On Client Computer (Windows 10/11)**:

1. **Set DNS Server**
   - Open **Network Connections**
   - Right-click network adapter → **Properties**
   - Select **Internet Protocol Version 4 (TCP/IPv4)**
   - Click **Properties**

   ```
   ┌────────────────────────────────────────────┐
   │ Internet Protocol Version 4 (TCP/IPv4)     │
   │ Properties                                 │
   ├────────────────────────────────────────────┤
   │                                            │
   │ (•) Obtain an IP address automatically     │
   │ ( ) Use the following IP address:          │
   │                                            │
   │ ( ) Obtain DNS server address automatically│
   │ (•) Use the following DNS server addresses:│
   │                                            │
   │     Preferred DNS server:                  │
   │     [192.168.1.10         ]                │
   │                                            │
   │     Alternate DNS server:                  │
   │     [                     ]                │
   │                                            │
   │                [OK] [Cancel]               │
   └────────────────────────────────────────────┘
   ```

   **Configuration**:
   - Select: **(•) Use the following DNS server addresses**
   - **Preferred DNS server**: `192.168.1.10` (your DC's IP)
   - Click **OK**

2. **Test DNS**
   - Open Command Prompt
   - Test DNS resolution:
   ```cmd
   nslookup company.local
   ```

   Expected output:
   ```
   Server:  WIN-SERVER01.company.local
   Address: 192.168.1.10

   Name:    company.local
   Address: 192.168.1.10
   ```

---

### Step 36: Join Computer to Domain

**On Client Computer**:

1. **Open System Properties**
   - Right-click **This PC** → **Properties**
   - Or: `Win + Pause/Break`
   - Click **Advanced system settings**

2. **Computer Name Tab**
   ```
   ┌────────────────────────────────────────────┐
   │ System Properties                          │
   ├────────────────────────────────────────────┤
   │ [Computer Name] [Hardware] [Advanced] ...  │
   │                                            │
   │ Computer name:    DESKTOP-ABC123           │
   │ Full name:        DESKTOP-ABC123           │
   │ Workgroup:        WORKGROUP                │
   │                                            │
   │ To rename this computer or join a domain,  │
   │ click Change.                              │
   │                                            │
   │                          [Change...]       │
   │                                            │
   │                [OK] [Cancel] [Apply]       │
   └────────────────────────────────────────────┘
   ```
   - Click **Change...**

3. **Computer Name/Domain Changes**
   ```
   ┌────────────────────────────────────────────┐
   │ Computer Name/Domain Changes               │
   ├────────────────────────────────────────────┤
   │                                            │
   │ Computer name:                             │
   │ [DESKTOP-ABC123              ]             │
   │                                            │
   │ Full computer name:                        │
   │ DESKTOP-ABC123                             │
   │                                            │
   │ Member of:                                 │
   │ ( ) Domain:                                │
   │     [company.local           ]             │
   │ (•) Workgroup:                             │
   │     [WORKGROUP                ]            │
   │                                            │
   │                [OK] [Cancel]               │
   └────────────────────────────────────────────┘
   ```

   **Configuration**:
   - **Computer name**: Change to something meaningful (e.g., `LAPTOP-KABLU`)
   - Select: **( ) Domain**
   - Enter: `company.local`
   - Click **OK**

4. **Enter Domain Credentials**
   ```
   ┌────────────────────────────────────────────┐
   │ Computer Name/Domain Changes               │
   ├────────────────────────────────────────────┤
   │                                            │
   │ Enter the name and password of an account  │
   │ with permission to join the domain.        │
   │                                            │
   │ User name:                                 │
   │ [Administrator                    ]        │
   │                                            │
   │ Password:                                  │
   │ [********************             ]        │
   │                                            │
   │                [OK] [Cancel]               │
   └────────────────────────────────────────────┘
   ```

   **Credentials**:
   - **User name**: `Administrator` (or `COMPANY\Administrator`)
   - **Password**: Your domain admin password
   - Click **OK**

5. **Welcome to Domain**
   ```
   ┌────────────────────────────────────────────┐
   │ Computer Name/Domain Changes               │
   ├────────────────────────────────────────────┤
   │                                            │
   │  ✓  Welcome to the company.local domain.   │
   │                                            │
   │                   [OK]                     │
   └────────────────────────────────────────────┘
   ```
   - Success! ✓
   - Click **OK**

6. **Restart Required**
   ```
   ┌────────────────────────────────────────────┐
   │ Computer Name/Domain Changes               │
   ├────────────────────────────────────────────┤
   │                                            │
   │ You must restart your computer to apply    │
   │ these changes.                             │
   │                                            │
   │ Before restarting, save any open files and │
   │ close all programs.                        │
   │                                            │
   │                   [OK]                     │
   └────────────────────────────────────────────┘
   ```
   - Click **OK**
   - Click **Restart Now**

---

### Step 37: Login with Domain Account

After restart:

1. **Login Screen**
   ```
   How do you want to sign in?

   Sign in to:  [company.local           ▼]

   User name:   [kablu                      ]
   Password:    [********************       ]

   [Sign in]
   ```

   **Credentials**:
   - **Sign in to**: Select **company.local**
   - **User name**: `kablu`
   - **Password**: User's password
   - Click **Sign in**

2. **First Login**
   - Takes longer (profile creation)
   - Desktop and settings load
   - Group Policies apply automatically

3. **Verify Domain Join**
   - Open Command Prompt
   - Run:
   ```cmd
   whoami
   ```
   Output: `company\kablu`

   - Run:
   ```cmd
   echo %USERDOMAIN%
   ```
   Output: `COMPANY`

---

## Verification and Testing

### Step 38: Verify Active Directory Installation

**On Domain Controller**:

1. **Check AD Services**
   ```powershell
   Get-Service ADWS,Kdc,NTDS,DNS
   ```

   Expected output:
   ```
   Status   Name               DisplayName
   ------   ----               -----------
   Running  ADWS               Active Directory Web Services
   Running  Kdc                Kerberos Key Distribution Center
   Running  NTDS               Active Directory Domain Services
   Running  DNS                DNS Server
   ```

2. **Check Domain Controllers**
   ```powershell
   Get-ADDomainController
   ```

   Expected output:
   ```
   Name                : WIN-SERVER01
   Domain              : company.local
   Enabled             : True
   IsGlobalCatalog     : True
   IsReadOnly          : False
   OperationMasterRoles: {SchemaMaster, DomainNamingMaster, PDCEmulator...}
   ```

3. **Check Users**
   ```powershell
   Get-ADUser -Filter * | Select-Object Name, SamAccountName
   ```

   Expected output:
   ```
   Name            SamAccountName
   ----            --------------
   Administrator   Administrator
   Guest           Guest
   Kablu Khan      kablu
   John Doe        john
   ```

4. **Check Groups**
   ```powershell
   Get-ADGroup -Filter * | Select-Object Name, GroupScope
   ```

---

### Step 39: Test User Authentication

**Test Login**:

1. **From Client Computer**
   - Try logging in with domain account
   - Username: `kablu@company.local`
   - Password: User password

2. **From Server (PowerShell)**
   ```powershell
   Test-ComputerSecureChannel -Verbose
   ```
   - Returns: `True` if domain trust is healthy

3. **Check User Groups**
   ```powershell
   whoami /groups
   ```
   - Shows all groups user belongs to

---

### Step 40: Test DNS

**On Domain Controller**:

```powershell
# Test DNS resolution
nslookup company.local

# Test SRV records
nslookup -type=srv _ldap._tcp.company.local
```

Expected output:
```
_ldap._tcp.company.local SRV service location:
    priority       = 0
    weight         = 100
    port           = 389
    svr hostname   = WIN-SERVER01.company.local
```

---

### Step 41: Test Group Policy

**On Client Computer**:

1. **Check Applied Policies**
   ```cmd
   gpresult /r
   ```

   Shows:
   - Applied Group Policies
   - Computer settings
   - User settings

2. **Generate Detailed Report**
   ```cmd
   gpresult /h C:\gpreport.html
   ```
   - Open `C:\gpreport.html` in browser
   - Shows all applied policies in detail

---

## Best Practices

### Security Best Practices:

1. ✅ **Use Strong Passwords**
   - Minimum 8 characters
   - Complexity enabled
   - Regular changes (90 days)

2. ✅ **Least Privilege Principle**
   - Don't use Administrator for daily tasks
   - Create separate accounts for different roles
   - Use dedicated service accounts

3. ✅ **Regular Backups**
   - Backup Active Directory database
   - Backup System State
   - Test restore procedures
   - Schedule: Daily

4. ✅ **Monitor Security**
   - Enable audit logging
   - Review event logs regularly
   - Monitor failed login attempts
   - Track group membership changes

5. ✅ **Keep Systems Updated**
   - Install Windows Updates
   - Keep domain functional level current
   - Update firmware regularly

---

### Organizational Best Practices:

1. ✅ **Use Descriptive Names**
   - Clear OU names (Users, Computers, Groups)
   - Meaningful group names (IT-Admins, not Group1)
   - Standardized naming conventions

2. ✅ **Proper OU Structure**
   - Organize by department or location
   - Separate users, computers, groups
   - Use for Group Policy targeting

3. ✅ **Document Everything**
   - AD structure diagram
   - Group Policy purposes
   - Account naming conventions
   - Recovery procedures

4. ✅ **Test Before Production**
   - Test Group Policies on test OU first
   - Validate changes before applying
   - Have rollback plan

---

## Troubleshooting

### Common Issues:

#### Issue 1: Cannot Join Domain

**Symptoms**:
- Error: "The specified domain either does not exist or could not be contacted"

**Solutions**:
1. Check DNS configuration on client
   ```cmd
   ipconfig /all
   ```
   - DNS Server should point to DC's IP

2. Test DNS resolution
   ```cmd
   nslookup company.local
   ```

3. Ping domain controller
   ```cmd
   ping company.local
   ping 192.168.1.10
   ```

4. Check firewall on DC
   - Allow DNS (port 53)
   - Allow Kerberos (port 88)
   - Allow LDAP (port 389)

---

#### Issue 2: Login Slow or Fails

**Symptoms**:
- Login takes several minutes
- "The trust relationship between this workstation and the primary domain failed"

**Solutions**:
1. Check secure channel
   ```powershell
   Test-ComputerSecureChannel -Repair
   ```

2. Rejoin domain if needed
   - Leave domain (join workgroup)
   - Restart
   - Join domain again

---

#### Issue 3: Group Policy Not Applying

**Symptoms**:
- Settings not applying to users/computers
- Custom GPOs not working

**Solutions**:
1. Force update
   ```cmd
   gpupdate /force
   ```

2. Check GPO link and enforcement
   - Open Group Policy Management
   - Verify GPO is linked to correct OU
   - Check "Link Enabled" is checked

3. Check scope
   - Verify user/computer is in correct OU
   - Check security filtering

4. Wait for replication
   - GPOs replicate between DCs
   - Can take 5-15 minutes

---

#### Issue 4: DNS Issues

**Symptoms**:
- Cannot resolve domain name
- SRV records missing

**Solutions**:
1. Check DNS service
   ```powershell
   Get-Service DNS
   Restart-Service DNS
   ```

2. Verify DNS zones
   - Open DNS Manager
   - Check Forward Lookup Zones
   - Verify _msdcs zone exists

3. Re-register DNS records
   ```cmd
   ipconfig /registerdns
   ```

4. Restart Netlogon service
   ```powershell
   Restart-Service Netlogon
   ```

---

### Useful Commands:

**Active Directory**:
```powershell
# List all users
Get-ADUser -Filter *

# List all computers
Get-ADComputer -Filter *

# List all groups
Get-ADGroup -Filter *

# Check replication
repadmin /showrepl

# Check FSMO roles
netdom query fsmo
```

**DNS**:
```powershell
# Clear DNS cache
Clear-DnsClientCache

# Test DNS
Resolve-DnsName company.local

# List DNS records
Get-DnsServerResourceRecord -ZoneName company.local
```

**Group Policy**:
```cmd
# Force GP update
gpupdate /force

# Check applied policies
gpresult /r

# Generate HTML report
gpresult /h report.html
```

---

## Summary

You have successfully:

✅ Installed Active Directory Domain Services
✅ Promoted server to Domain Controller
✅ Configured DNS
✅ Created Organizational Units (OUs)
✅ Created user accounts
✅ Created security groups
✅ Configured Group Policies
✅ Joined client computers to domain
✅ Verified and tested the installation

Your Active Directory is now ready for:
- User management
- Computer management
- Group Policy deployment
- **Integration with RA system** for auto-enrollment!

---

## Next Steps

### For RA System Integration:

1. ✅ Note down these details (needed for RA):
   - **Domain**: `company.local`
   - **LDAP Server**: `192.168.1.10` (DC IP)
   - **LDAP Port**: `389` (or `636` for LDAPS)
   - **Base DN**: `DC=company,DC=local`
   - **Service Account**: Create dedicated account for RA
   - **Groups Created**:
     - `PKI-RA-Admins` (RA Administrators)
     - `PKI-RA-Officers` (RA Officers)
     - `PKI-RA-Operators` (RA Operators)
     - `All Employees` (Auto-enrollment eligible)

2. ✅ Create RA Service Account:
   ```powershell
   New-ADUser -Name "RA Service Account" `
              -SamAccountName "ra-service" `
              -UserPrincipalName "ra-service@company.local" `
              -Path "OU=Users,OU=Company,DC=company,DC=local" `
              -AccountPassword (ConvertTo-SecureString "P@ssw0rd123!" -AsPlainText -Force) `
              -Enabled $true `
              -PasswordNeverExpires $true
   ```

3. ✅ Grant read permissions to RA service account (for LDAP queries)

4. ✅ Configure RA system with these AD details

---

**Document End**

**Version:** 1.0
**Last Updated:** 2026-01-21
**Author:** System Administrator Guide
**Status:** Production Ready
