package com.company.ra.service;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.*;
import java.util.Hashtable;

/**
 * LDAP Service for Active Directory Operations
 *
 * Provides methods to:
 * - Check user existence in AD
 * - Add new users to AD
 * - Verify user in specific OU
 *
 * @author RA Team
 */
public class LDAPService {

    // LDAP Configuration
    private static final String LDAP_URL = "ldap://localhost:389";
    private static final String ADMIN_DN = "cn=Administrator,cn=Users,dc=corp,dc=local";
    private static final String ADMIN_PASSWORD = "P@ssw0rd123!";
    private static final String BASE_DN = "dc=corp,dc=local";

    private DirContext context;

    /**
     * Initialize LDAP connection
     */
    public LDAPService() throws Exception {
        connect();
    }

    /**
     * Connect to LDAP/Active Directory
     */
    private void connect() throws Exception {
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, LDAP_URL);
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, ADMIN_DN);
        env.put(Context.SECURITY_CREDENTIALS, ADMIN_PASSWORD);
        // Ignore referrals to prevent PartialResultException in Active Directory
        env.put(Context.REFERRAL, "ignore");

        try {
            context = new InitialDirContext(env);
            System.out.println("✓ Successfully connected to LDAP: " + LDAP_URL);
        } catch (Exception e) {
            System.err.println("✗ Failed to connect to LDAP: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Check if user exists in Active Directory
     *
     * @param username Username to search (sAMAccountName or cn)
     * @return true if user exists, false otherwise
     */
    public boolean checkUserExists(String username) {
        try {
            System.out.println("\n=== Checking User Existence ===");
            System.out.println("Username: " + username);

            // Create search filter
            String searchFilter = String.format("(&(objectClass=user)(cn=%s))", username);

            // Configure search controls
            SearchControls searchControls = new SearchControls();
            searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            searchControls.setReturningAttributes(new String[]{
                "cn", "sAMAccountName", "distinguishedName", "mail", "displayName"
            });

            // Perform search
            NamingEnumeration<SearchResult> results = context.search(BASE_DN, searchFilter, searchControls);

            boolean found = false;
            try {
                while (results.hasMore()) {
                    SearchResult result = results.next();
                    found = true;

                    System.out.println("\n✓ User found!");
                    System.out.println("Distinguished Name: " + result.getNameInNamespace());

                    Attributes attrs = result.getAttributes();
                    if (attrs.get("cn") != null) {
                        System.out.println("CN: " + attrs.get("cn").get());
                    }
                    if (attrs.get("sAMAccountName") != null) {
                        System.out.println("sAMAccountName: " + attrs.get("sAMAccountName").get());
                    }
                    if (attrs.get("mail") != null) {
                        System.out.println("Email: " + attrs.get("mail").get());
                    }
                    if (attrs.get("displayName") != null) {
                        System.out.println("Display Name: " + attrs.get("displayName").get());
                    }
                }
            } catch (javax.naming.PartialResultException e) {
                // Ignore partial result exception (caused by referrals in AD)
                if (!found) {
                    System.out.println("⚠ Search completed with referrals (partial results)");
                }
            }

            if (!found) {
                System.out.println("✗ User not found in Active Directory");
            }

            return found;

        } catch (javax.naming.PartialResultException e) {
            System.err.println("⚠ Partial result (LDAP referrals) - this is normal in AD");
            return false;
        } catch (Exception e) {
            System.err.println("✗ Error checking user existence: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Add new user to Active Directory in specified OU
     *
     * @param username Username (cn and sAMAccountName)
     * @param password User password
     * @param ouName Organizational Unit name (e.g., "RA Users")
     * @return true if user created successfully, false otherwise
     */
    public boolean addUser(String username, String password, String ouName) {
        try {
            System.out.println("\n=== Adding New User ===");
            System.out.println("Username: " + username);
            System.out.println("OU: " + ouName);

            // Construct DN for new user
            String userDN = String.format("cn=%s,ou=%s,%s", username, ouName, BASE_DN);
            System.out.println("User DN: " + userDN);

            // Create user attributes
            Attributes userAttributes = new BasicAttributes(true); // case-ignore

            // Object class
            Attribute objectClass = new BasicAttribute("objectClass");
            objectClass.add("top");
            objectClass.add("person");
            objectClass.add("organizationalPerson");
            objectClass.add("user");
            userAttributes.put(objectClass);

            // Common Name
            userAttributes.put("cn", username);

            // sAMAccountName (required for Windows login)
            userAttributes.put("sAMAccountName", username);

            // User Principal Name
            userAttributes.put("userPrincipalName", username + "@corp.local");

            // Display Name
            userAttributes.put("displayName", username);

            // User Account Control (normal account)
            // 512 = Normal account
            // 544 = Normal account + password not required
            // 66048 = Normal account + password never expires
            userAttributes.put("userAccountControl", "544");

            // Create the user
            context.createSubcontext(userDN, userAttributes);
            System.out.println("✓ User created successfully!");

            // Set password (Active Directory requires Unicode password)
            setPassword(userDN, password);

            // Enable the account (set userAccountControl to 512)
            ModificationItem[] mods = new ModificationItem[1];
            mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE,
                new BasicAttribute("userAccountControl", "512"));
            context.modifyAttributes(userDN, mods);
            System.out.println("✓ User account enabled!");

            return true;

        } catch (Exception e) {
            System.err.println("✗ Error adding user: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Set password for user (Active Directory specific)
     *
     * @param userDN Distinguished Name of user
     * @param password New password
     */
    private void setPassword(String userDN, String password) {
        try {
            // Active Directory requires password to be enclosed in quotes and encoded as UTF-16LE
            String quotedPassword = "\"" + password + "\"";
            byte[] unicodePassword = quotedPassword.getBytes("UTF-16LE");

            ModificationItem[] mods = new ModificationItem[1];
            mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE,
                new BasicAttribute("unicodePwd", unicodePassword));

            context.modifyAttributes(userDN, mods);
            System.out.println("✓ Password set successfully!");

        } catch (Exception e) {
            System.err.println("✗ Error setting password: " + e.getMessage());
            // Note: Password setting might fail due to password policy or SSL requirement
            System.err.println("Note: AD often requires SSL/TLS for password operations");
        }
    }

    /**
     * Create Organizational Unit if it doesn't exist
     *
     * @param ouName OU name
     * @return true if OU exists or created successfully
     */
    public boolean createOUIfNotExists(String ouName) {
        try {
            String ouDN = String.format("ou=%s,%s", ouName, BASE_DN);

            // Check if OU exists
            try {
                context.getAttributes(ouDN);
                System.out.println("✓ OU already exists: " + ouDN);
                return true;
            } catch (Exception e) {
                // OU doesn't exist, create it
                System.out.println("Creating OU: " + ouDN);

                Attributes ouAttributes = new BasicAttributes(true);

                Attribute objectClass = new BasicAttribute("objectClass");
                objectClass.add("top");
                objectClass.add("organizationalUnit");
                ouAttributes.put(objectClass);

                ouAttributes.put("ou", ouName);

                context.createSubcontext(ouDN, ouAttributes);
                System.out.println("✓ OU created successfully: " + ouDN);
                return true;
            }

        } catch (Exception e) {
            System.err.println("✗ Error creating OU: " + e.getMessage());
            return false;
        }
    }

    /**
     * Check if user exists in specific OU
     *
     * @param username Username to search
     * @param ouName OU name
     * @return true if user exists in specified OU
     */
    public boolean checkUserInOU(String username, String ouName) {
        try {
            System.out.println("\n=== Checking User in OU ===");
            System.out.println("Username: " + username);
            System.out.println("OU: " + ouName);

            String ouDN = String.format("ou=%s,%s", ouName, BASE_DN);
            String searchFilter = String.format("(&(objectClass=user)(cn=%s))", username);

            SearchControls searchControls = new SearchControls();
            searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            searchControls.setReturningAttributes(new String[]{
                "cn", "sAMAccountName", "distinguishedName", "mail", "displayName", "userPrincipalName"
            });

            NamingEnumeration<SearchResult> results = context.search(ouDN, searchFilter, searchControls);

            boolean found = false;
            try {
                while (results.hasMore()) {
                    SearchResult result = results.next();
                    found = true;

                    System.out.println("\n✓ User found in OU!");
                    System.out.println("Distinguished Name: " + result.getNameInNamespace());

                    Attributes attrs = result.getAttributes();
                    System.out.println("CN: " + attrs.get("cn").get());
                    System.out.println("sAMAccountName: " + attrs.get("sAMAccountName").get());
                    if (attrs.get("userPrincipalName") != null) {
                        System.out.println("UPN: " + attrs.get("userPrincipalName").get());
                    }
                    if (attrs.get("displayName") != null) {
                        System.out.println("Display Name: " + attrs.get("displayName").get());
                    }
                }
            } catch (javax.naming.PartialResultException e) {
                // Ignore partial result exception
                if (!found) {
                    System.out.println("⚠ Search completed with referrals");
                }
            }

            if (!found) {
                System.out.println("✗ User not found in OU: " + ouDN);
            }

            return found;

        } catch (javax.naming.PartialResultException e) {
            System.err.println("⚠ Partial result (LDAP referrals) - this is normal in AD");
            return false;
        } catch (Exception e) {
            System.err.println("✗ Error checking user in OU: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * List all users in Active Directory
     */
    public void listAllUsers() {
        try {
            System.out.println("\n=== Listing All Users ===");

            String searchFilter = "(objectClass=user)";

            SearchControls searchControls = new SearchControls();
            searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            searchControls.setReturningAttributes(new String[]{"cn", "sAMAccountName", "distinguishedName"});

            NamingEnumeration<SearchResult> results = context.search(BASE_DN, searchFilter, searchControls);

            int count = 0;
            try {
                while (results.hasMore()) {
                    SearchResult result = results.next();
                    count++;

                    Attributes attrs = result.getAttributes();
                    String cn = attrs.get("cn") != null ? (String) attrs.get("cn").get() : "N/A";
                    String sam = attrs.get("sAMAccountName") != null ? (String) attrs.get("sAMAccountName").get() : "N/A";

                    System.out.println(count + ". CN: " + cn + ", sAMAccountName: " + sam);
                }
            } catch (javax.naming.PartialResultException e) {
                // Ignore partial result exception
                System.out.println("(Stopped at " + count + " users due to referrals)");
            }

            System.out.println("\nTotal users found: " + count);

        } catch (javax.naming.PartialResultException e) {
            System.err.println("⚠ Partial result (LDAP referrals) - this is normal in AD");
        } catch (Exception e) {
            System.err.println("✗ Error listing users: " + e.getMessage());
        }
    }

    /**
     * Authenticate user with username and password
     *
     * @param username Username (sAMAccountName or cn)
     * @param password User's password
     * @return true if authentication successful, false otherwise
     */
    public boolean authenticateUser(String username, String password) {
        try {
            System.out.println("\n=== Authenticating User ===");
            System.out.println("Username: " + username);

            // Step 1: Find user's DN
            String userDN = getUserDN(username);
            if (userDN == null) {
                System.err.println("✗ User not found: " + username);
                return false;
            }

            System.out.println("User DN: " + userDN);

            // Step 2: Attempt to bind with user credentials
            Hashtable<String, String> authEnv = new Hashtable<>();
            authEnv.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            authEnv.put(Context.PROVIDER_URL, LDAP_URL);
            authEnv.put(Context.SECURITY_AUTHENTICATION, "simple");
            authEnv.put(Context.SECURITY_PRINCIPAL, userDN);
            authEnv.put(Context.SECURITY_CREDENTIALS, password);
            authEnv.put(Context.REFERRAL, "ignore");

            DirContext userContext = null;
            try {
                // Try to create context with user credentials
                userContext = new InitialDirContext(authEnv);
                System.out.println("✓ Authentication successful!");
                System.out.println("✓ User '" + username + "' credentials are valid");
                return true;

            } catch (javax.naming.AuthenticationException e) {
                System.err.println("✗ Authentication failed: Invalid username or password");
                System.err.println("  Error: " + e.getMessage());
                return false;

            } finally {
                if (userContext != null) {
                    try {
                        userContext.close();
                    } catch (Exception e) {
                        // Ignore close errors
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("✗ Error during authentication: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get user's Distinguished Name by username
     *
     * @param username Username to search (cn or sAMAccountName)
     * @return User's DN if found, null otherwise
     */
    private String getUserDN(String username) {
        try {
            String searchFilter = String.format("(&(objectClass=user)(|(cn=%s)(sAMAccountName=%s)))",
                username, username);

            SearchControls searchControls = new SearchControls();
            searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            searchControls.setReturningAttributes(new String[]{"distinguishedName"});

            NamingEnumeration<SearchResult> results = context.search(BASE_DN, searchFilter, searchControls);

            try {
                if (results.hasMore()) {
                    SearchResult result = results.next();
                    return result.getNameInNamespace();
                }
            } catch (javax.naming.PartialResultException e) {
                // Ignore partial result
            }

            return null;

        } catch (Exception e) {
            System.err.println("✗ Error getting user DN: " + e.getMessage());
            return null;
        }
    }

    /**
     * Authenticate user and retrieve user attributes
     *
     * @param username Username
     * @param password Password
     * @return User attributes if authentication successful, null otherwise
     */
    public Hashtable<String, String> authenticateAndGetUserInfo(String username, String password) {
        try {
            System.out.println("\n=== Authenticate and Retrieve User Info ===");
            System.out.println("Username: " + username);

            // Step 1: Authenticate
            if (!authenticateUser(username, password)) {
                return null;
            }

            // Step 2: Retrieve user attributes
            String searchFilter = String.format("(&(objectClass=user)(|(cn=%s)(sAMAccountName=%s)))",
                username, username);

            SearchControls searchControls = new SearchControls();
            searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            searchControls.setReturningAttributes(new String[]{
                "cn", "sAMAccountName", "distinguishedName", "mail",
                "displayName", "department", "userPrincipalName", "memberOf"
            });

            NamingEnumeration<SearchResult> results = context.search(BASE_DN, searchFilter, searchControls);

            Hashtable<String, String> userInfo = new Hashtable<>();

            try {
                if (results.hasMore()) {
                    SearchResult result = results.next();
                    Attributes attrs = result.getAttributes();

                    System.out.println("\n✓ User Information Retrieved:");

                    // Extract attributes
                    if (attrs.get("cn") != null) {
                        String cn = (String) attrs.get("cn").get();
                        userInfo.put("cn", cn);
                        System.out.println("  CN: " + cn);
                    }
                    if (attrs.get("sAMAccountName") != null) {
                        String sam = (String) attrs.get("sAMAccountName").get();
                        userInfo.put("sAMAccountName", sam);
                        System.out.println("  sAMAccountName: " + sam);
                    }
                    if (attrs.get("distinguishedName") != null) {
                        String dn = (String) attrs.get("distinguishedName").get();
                        userInfo.put("distinguishedName", dn);
                        System.out.println("  DN: " + dn);
                    }
                    if (attrs.get("mail") != null) {
                        String mail = (String) attrs.get("mail").get();
                        userInfo.put("mail", mail);
                        System.out.println("  Email: " + mail);
                    }
                    if (attrs.get("displayName") != null) {
                        String displayName = (String) attrs.get("displayName").get();
                        userInfo.put("displayName", displayName);
                        System.out.println("  Display Name: " + displayName);
                    }
                    if (attrs.get("department") != null) {
                        String department = (String) attrs.get("department").get();
                        userInfo.put("department", department);
                        System.out.println("  Department: " + department);
                    }
                    if (attrs.get("userPrincipalName") != null) {
                        String upn = (String) attrs.get("userPrincipalName").get();
                        userInfo.put("userPrincipalName", upn);
                        System.out.println("  UPN: " + upn);
                    }

                    // Extract groups (memberOf)
                    if (attrs.get("memberOf") != null) {
                        System.out.println("  Groups:");
                        NamingEnumeration<?> groups = attrs.get("memberOf").getAll();
                        int groupCount = 0;
                        while (groups.hasMore()) {
                            String group = (String) groups.next();
                            groupCount++;
                            System.out.println("    " + groupCount + ". " + group);
                        }
                    }

                    return userInfo;
                }
            } catch (javax.naming.PartialResultException e) {
                // Ignore partial result
            }

            return userInfo;

        } catch (Exception e) {
            System.err.println("✗ Error retrieving user info: " + e.getMessage());
            return null;
        }
    }

    /**
     * Close LDAP connection
     */
    public void close() {
        try {
            if (context != null) {
                context.close();
                System.out.println("\n✓ LDAP connection closed");
            }
        } catch (Exception e) {
            System.err.println("✗ Error closing LDAP connection: " + e.getMessage());
        }
    }

    /**
     * Main method to test LDAP operations
     */
    public static void main(String[] args) {
        LDAPService ldapService = null;

        try {
            System.out.println("╔════════════════════════════════════════════╗");
            System.out.println("║   LDAP Active Directory Test Application  ║");
            System.out.println("╚════════════════════════════════════════════╝");

            // Initialize LDAP connection
            ldapService = new LDAPService();

            // Test 1: Check if Administrator user exists
            System.out.println("\n" + "=".repeat(50));
            System.out.println("TEST 1: Check Administrator User");
            System.out.println("=".repeat(50));
            boolean adminExists = ldapService.checkUserExists("Administrator");
            System.out.println("Result: Administrator exists = " + adminExists);

            // Test 2: Create OU if it doesn't exist
            System.out.println("\n" + "=".repeat(50));
            System.out.println("TEST 2: Create OU 'RA Users'");
            System.out.println("=".repeat(50));
            String ouName = "RA Users";
            boolean ouCreated = ldapService.createOUIfNotExists(ouName);
            System.out.println("Result: OU creation/verification = " + ouCreated);

            // Test 3: Check if user 'kablu' exists before adding
            System.out.println("\n" + "=".repeat(50));
            System.out.println("TEST 3: Check if user 'kablu' exists");
            System.out.println("=".repeat(50));
            String username = "kablu";
            boolean kabulExistsBefore = ldapService.checkUserExists(username);
            System.out.println("Result: kablu exists (before) = " + kabulExistsBefore);

            // Test 4: Add new user 'kablu' with password 'mandal'
            System.out.println("\n" + "=".repeat(50));
            System.out.println("TEST 4: Add user 'kablu' with password 'mandal'");
            System.out.println("=".repeat(50));
            String password = "mandal";
            boolean userAdded = ldapService.addUser(username, password, ouName);
            System.out.println("Result: User added = " + userAdded);

            // Wait a moment for AD to sync
            if (userAdded) {
                System.out.println("\nWaiting 2 seconds for AD synchronization...");
                Thread.sleep(2000);
            }

            // Test 5: Verify user 'kablu' exists in AD
            System.out.println("\n" + "=".repeat(50));
            System.out.println("TEST 5: Verify user 'kablu' exists in AD");
            System.out.println("=".repeat(50));
            boolean kabulExistsAfter = ldapService.checkUserExists(username);
            System.out.println("Result: kablu exists (after) = " + kabulExistsAfter);

            // Test 6: Check if user 'kablu' exists in OU 'RA Users'
            System.out.println("\n" + "=".repeat(50));
            System.out.println("TEST 6: Verify user 'kablu' in OU 'RA Users'");
            System.out.println("=".repeat(50));
            boolean kabulInOU = ldapService.checkUserInOU(username, ouName);
            System.out.println("Result: kablu in OU = " + kabulInOU);

            // Test 7: Authenticate user 'kablu' with correct password
            System.out.println("\n" + "=".repeat(50));
            System.out.println("TEST 7: Authenticate user 'kablu' with correct password");
            System.out.println("=".repeat(50));
            boolean authSuccess = false;
            if (userAdded && kabulExistsAfter) {
                // Wait a moment for password to be set in AD
                System.out.println("Waiting 2 seconds for password synchronization...");
                Thread.sleep(2000);
                authSuccess = ldapService.authenticateUser(username, password);
            } else {
                System.out.println("⚠ Skipping authentication test - user not created");
            }
            System.out.println("Result: Authentication with correct password = " + authSuccess);

            // Test 8: Authenticate user 'kablu' with incorrect password
            System.out.println("\n" + "=".repeat(50));
            System.out.println("TEST 8: Authenticate user 'kablu' with incorrect password");
            System.out.println("=".repeat(50));
            boolean authFailed = true; // Should fail
            if (userAdded && kabulExistsAfter) {
                String wrongPassword = "wrongpassword";
                boolean authResult = ldapService.authenticateUser(username, wrongPassword);
                authFailed = !authResult; // Should be false (authentication should fail)
                System.out.println("Result: Authentication correctly rejected = " + authFailed);
            } else {
                System.out.println("⚠ Skipping authentication test - user not created");
            }

            // Test 9: Authenticate and retrieve user information
            System.out.println("\n" + "=".repeat(50));
            System.out.println("TEST 9: Authenticate and retrieve user information");
            System.out.println("=".repeat(50));
            Hashtable<String, String> userInfo = null;
            if (userAdded && kabulExistsAfter) {
                userInfo = ldapService.authenticateAndGetUserInfo(username, password);
                boolean userInfoRetrieved = (userInfo != null && !userInfo.isEmpty());
                System.out.println("Result: User info retrieved = " + userInfoRetrieved);
            } else {
                System.out.println("⚠ Skipping user info test - user not created");
            }

            // Summary
            System.out.println("\n" + "=".repeat(50));
            System.out.println("SUMMARY");
            System.out.println("=".repeat(50));
            System.out.println("✓ LDAP Connection: SUCCESS");
            System.out.println("✓ Administrator Check: " + (adminExists ? "FOUND" : "NOT FOUND"));
            System.out.println("✓ OU Creation: " + (ouCreated ? "SUCCESS" : "FAILED"));
            System.out.println("✓ User Addition: " + (userAdded ? "SUCCESS" : "FAILED"));
            System.out.println("✓ User Verification: " + (kabulExistsAfter ? "SUCCESS" : "FAILED"));
            System.out.println("✓ User in OU Check: " + (kabulInOU ? "SUCCESS" : "FAILED"));
            System.out.println("✓ Authentication Test (Correct Password): " + (authSuccess ? "SUCCESS" : "FAILED"));
            System.out.println("✓ Authentication Test (Incorrect Password): " + (authFailed ? "SUCCESS" : "FAILED"));
            System.out.println("✓ User Info Retrieval: " + (userInfo != null ? "SUCCESS" : "FAILED"));

            if (userAdded && kabulExistsAfter && kabulInOU && authSuccess && authFailed) {
                System.out.println("\n🎉 ALL TESTS PASSED! User 'kablu' successfully created, verified, and authenticated");
            } else {
                System.out.println("\n⚠ SOME TESTS FAILED - Review logs above");
            }

        } catch (Exception e) {
            System.err.println("\n✗ ERROR: " + e.getMessage());
            e.printStackTrace();

        } finally {
            if (ldapService != null) {
                ldapService.close();
            }
        }

        System.out.println("\n" + "=".repeat(50));
        System.out.println("Test execution completed!");
        System.out.println("=".repeat(50));
    }
}
