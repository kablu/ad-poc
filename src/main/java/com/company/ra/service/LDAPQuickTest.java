package com.company.ra.service;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.*;
import java.util.Hashtable;

/**
 * Quick LDAP Test Utility
 *
 * Simple standalone test to verify LDAP connectivity and operations
 *
 * @author RA Team
 */
public class LDAPQuickTest {

    public static void main(String[] args) {
        // LDAP Configuration
        String ldapUrl = "ldap://localhost:389";
        String adminDn = "cn=Administrator,cn=Users,dc=corp,dc=local";
        String adminPassword = "P@ssw0rd123!";
        String baseDn = "dc=corp,dc=local";

        System.out.println("╔════════════════════════════════════════╗");
        System.out.println("║      LDAP Quick Connection Test        ║");
        System.out.println("╚════════════════════════════════════════╝\n");

        System.out.println("LDAP URL: " + ldapUrl);
        System.out.println("Admin DN: " + adminDn);
        System.out.println("Base DN: " + baseDn);
        System.out.println();

        DirContext ctx = null;

        try {
            // Step 1: Connect to LDAP
            System.out.println("=".repeat(50));
            System.out.println("STEP 1: Connecting to LDAP...");
            System.out.println("=".repeat(50));

            Hashtable<String, String> env = new Hashtable<>();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            env.put(Context.PROVIDER_URL, ldapUrl);
            env.put(Context.SECURITY_AUTHENTICATION, "simple");
            env.put(Context.SECURITY_PRINCIPAL, adminDn);
            env.put(Context.SECURITY_CREDENTIALS, adminPassword);
            // Ignore referrals to prevent PartialResultException
            env.put(Context.REFERRAL, "ignore");

            ctx = new InitialDirContext(env);
            System.out.println("✓ Successfully connected to LDAP!\n");

            // Step 2: Search for Administrator user
            System.out.println("=".repeat(50));
            System.out.println("STEP 2: Searching for Administrator user...");
            System.out.println("=".repeat(50));

            String searchFilter = "(&(objectClass=user)(cn=Administrator))";
            SearchControls searchControls = new SearchControls();
            searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            searchControls.setReturningAttributes(new String[]{"cn", "distinguishedName", "sAMAccountName"});

            NamingEnumeration<SearchResult> results = ctx.search(baseDn, searchFilter, searchControls);

            if (results.hasMore()) {
                SearchResult result = results.next();
                System.out.println("✓ Administrator user found!");
                System.out.println("  DN: " + result.getNameInNamespace());
                System.out.println("  CN: " + result.getAttributes().get("cn").get());
                if (result.getAttributes().get("sAMAccountName") != null) {
                    System.out.println("  sAMAccountName: " + result.getAttributes().get("sAMAccountName").get());
                }
            } else {
                System.out.println("✗ Administrator user not found");
            }
            System.out.println();

            // Step 3: Search for any existing users
            System.out.println("=".repeat(50));
            System.out.println("STEP 3: Listing first 5 users in AD...");
            System.out.println("=".repeat(50));

            searchFilter = "(objectClass=user)";
            searchControls.setCountLimit(5); // Limit to 5 users
            results = ctx.search(baseDn, searchFilter, searchControls);

            int count = 0;
            try {
                while (results.hasMore()) {
                    SearchResult result = results.next();
                    count++;
                    Attributes attrs = result.getAttributes();
                    String cn = attrs.get("cn") != null ? (String) attrs.get("cn").get() : "N/A";
                    System.out.println("  " + count + ". " + cn);
                }
            } catch (javax.naming.PartialResultException e) {
                // Ignore partial result exception (caused by referrals)
                System.out.println("  (Stopped at " + count + " users due to referral)");
            }
            System.out.println();

            // Step 4: Check if 'kablu' user exists
            System.out.println("=".repeat(50));
            System.out.println("STEP 4: Checking if user 'kablu' exists...");
            System.out.println("=".repeat(50));

            searchFilter = "(&(objectClass=user)(cn=kablu))";
            searchControls = new SearchControls();
            searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            searchControls.setReturningAttributes(new String[]{"cn", "distinguishedName"});

            results = ctx.search(baseDn, searchFilter, searchControls);

            if (results.hasMore()) {
                SearchResult result = results.next();
                System.out.println("✓ User 'kablu' already exists!");
                System.out.println("  DN: " + result.getNameInNamespace());
            } else {
                System.out.println("✗ User 'kablu' not found (will be created)");
            }
            System.out.println();

            System.out.println("=".repeat(50));
            System.out.println("✓ LDAP Connection Test: SUCCESS");
            System.out.println("=".repeat(50));
            System.out.println("\n✓ LDAP is working correctly!");
            System.out.println("✓ You can now run LDAPService.main() to create user 'kablu'");

        } catch (javax.naming.AuthenticationException e) {
            System.err.println("\n✗ AUTHENTICATION FAILED!");
            System.err.println("  - Check username and password");
            System.err.println("  - Verify admin DN: " + adminDn);
            System.err.println("  Error: " + e.getMessage());

        } catch (javax.naming.CommunicationException e) {
            System.err.println("\n✗ CONNECTION FAILED!");
            System.err.println("  - Check if LDAP server is running on localhost:389");
            System.err.println("  - Verify firewall settings");
            System.err.println("  Error: " + e.getMessage());

        } catch (javax.naming.PartialResultException e) {
            System.err.println("\n⚠ PARTIAL RESULT (LDAP Referrals)");
            System.err.println("  - Some results may be incomplete due to referrals");
            System.err.println("  - This is common in AD environments");
            System.err.println("  - Connection is still working!");
            System.err.println("  Error: " + e.getMessage());

        } catch (Exception e) {
            System.err.println("\n✗ ERROR: " + e.getMessage());
            e.printStackTrace();

        } finally {
            if (ctx != null) {
                try {
                    ctx.close();
                    System.out.println("\n✓ LDAP connection closed");
                } catch (Exception e) {
                    System.err.println("✗ Error closing connection: " + e.getMessage());
                }
            }
        }
    }
}
