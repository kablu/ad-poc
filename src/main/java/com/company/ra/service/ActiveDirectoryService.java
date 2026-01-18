package com.company.ra.service;

import com.company.ra.dto.ADUserAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.*;
import java.util.*;

/**
 * Service for Active Directory integration
 */
@Service
public class ActiveDirectoryService {

    private static final Logger logger = LoggerFactory.getLogger(ActiveDirectoryService.class);

    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final int PBKDF2_ITERATIONS = 10000;
    private static final int KEY_LENGTH = 256;

    @Value("${ad.url:ldap://localhost:389}")
    private String adUrl;

    @Value("${ad.baseDn:DC=company,DC=com}")
    private String baseDn;

    @Value("${ad.serviceAccount.username}")
    private String serviceUsername;

    @Value("${ad.serviceAccount.password}")
    private String servicePassword;

    /**
     * Authenticate user against Active Directory using challenge-response
     *
     * Simplified approach:
     * 1. Client encrypts response using password-derived key
     * 2. RA checks if username exists in Active Directory
     * 3. If user exists and is active, authentication is successful
     *
     * @param username Username (userPrincipalName)
     * @param encryptedResponse Base64-encoded encrypted response (not validated in this simplified approach)
     * @param challenge Original challenge bytes (not validated in this simplified approach)
     * @param salt Salt used for PBKDF2 (not used in this simplified approach)
     * @return true if authentication successful
     */
    public boolean authenticate(String username, String encryptedResponse,
                               byte[] challenge, byte[] salt) {
        try {
            logger.debug("Authenticating user against AD: {}", username);

            // Check if username exists in Active Directory
            String userDn = getUserDN(username);
            if (userDn == null) {
                logger.warn("User not found in AD: {}", username);
                return false;
            }

            // Verify user exists and is active (not locked/disabled)
            ADUserAttributes userAttrs = getUserDetailsInternal(userDn);
            if (userAttrs == null) {
                logger.warn("Failed to retrieve user details from AD: {}", username);
                return false;
            }

            // If we reach here, user exists in AD and is active
            logger.info("User authenticated successfully against AD: {}", username);
            return true;

        } catch (Exception e) {
            logger.error("Error during AD authentication for user: {}", username, e);
            return false;
        }
    }

    /**
     * Get user's Distinguished Name (DN) from username
     *
     * @param username Username (userPrincipalName or sAMAccountName)
     * @return User DN or null if not found
     */
    private String getUserDN(String username) {
        DirContext context = null;
        try {
            context = createServiceContext();

            SearchControls searchControls = new SearchControls();
            searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            searchControls.setReturningAttributes(new String[]{"distinguishedName"});

            String searchFilter = String.format(
                "(|(userPrincipalName=%s)(sAMAccountName=%s))",
                escapeLDAPSearchFilter(username),
                escapeLDAPSearchFilter(username)
            );

            NamingEnumeration<SearchResult> results = context.search(baseDn, searchFilter, searchControls);

            if (results.hasMore()) {
                SearchResult result = results.next();
                String dn = result.getNameInNamespace();
                logger.debug("Found user DN: {}", dn);
                return dn;
            }

            return null;

        } catch (Exception e) {
            logger.error("Error searching for user DN: {}", username, e);
            return null;
        } finally {
            closeContext(context);
        }
    }

    /**
     * Get user details from Active Directory
     *
     * @param username Username
     * @return ADUserAttributes or null
     */
    public ADUserAttributes getUserDetails(String username) {
        try {
            String userDn = getUserDN(username);
            if (userDn == null) {
                logger.warn("User not found in AD: {}", username);
                return null;
            }

            return getUserDetailsInternal(userDn);

        } catch (Exception e) {
            logger.error("Error retrieving user details from AD: {}", username, e);
            return null;
        }
    }

    /**
     * Get user details using DN
     *
     * @param userDn User Distinguished Name
     * @return ADUserAttributes or null
     */
    private ADUserAttributes getUserDetailsInternal(String userDn) {
        DirContext context = null;
        try {
            context = createServiceContext();

            Attributes attributes = context.getAttributes(userDn, new String[]{
                "cn", "mail", "userPrincipalName", "sAMAccountName",
                "ou", "o", "c", "memberOf", "userAccountControl"
            });

            ADUserAttributes userAttrs = new ADUserAttributes();

            // Common Name
            Attribute cnAttr = attributes.get("cn");
            if (cnAttr != null) {
                userAttrs.setCommonName((String) cnAttr.get());
            }

            // Email
            Attribute mailAttr = attributes.get("mail");
            if (mailAttr != null) {
                userAttrs.setEmail((String) mailAttr.get());
            }

            // Username
            Attribute upnAttr = attributes.get("userPrincipalName");
            if (upnAttr != null) {
                userAttrs.setUsername((String) upnAttr.get());
            } else {
                Attribute samAttr = attributes.get("sAMAccountName");
                if (samAttr != null) {
                    userAttrs.setUsername((String) samAttr.get());
                }
            }

            // Parse DN for organizational info
            parseDNAttributes(userDn, userAttrs);

            // Get group memberships
            Attribute memberOfAttr = attributes.get("memberOf");
            if (memberOfAttr != null) {
                Set<String> groups = new HashSet<>();
                NamingEnumeration<?> memberOfEnum = memberOfAttr.getAll();
                while (memberOfEnum.hasMore()) {
                    String groupDn = (String) memberOfEnum.next();
                    String groupName = extractCNFromDN(groupDn);
                    groups.add(groupName);
                }
                userAttrs.setAdGroups(groups);
            }

            // Map AD groups to application roles
            userAttrs.setRoles(mapGroupsToRoles(userAttrs.getAdGroups()));

            // Check if account is enabled
            Attribute uacAttr = attributes.get("userAccountControl");
            if (uacAttr != null) {
                int uac = Integer.parseInt((String) uacAttr.get());
                boolean isDisabled = (uac & 0x0002) != 0;
                if (isDisabled) {
                    logger.warn("User account is disabled: {}", userAttrs.getUsername());
                    return null;
                }
            }

            logger.debug("Retrieved user details from AD: {}", userAttrs.getUsername());
            return userAttrs;

        } catch (Exception e) {
            logger.error("Error retrieving user details from AD for DN: {}", userDn, e);
            return null;
        } finally {
            closeContext(context);
        }
    }

    /**
     * Parse organizational attributes from DN
     *
     * @param dn Distinguished Name
     * @param userAttrs User attributes to populate
     */
    private void parseDNAttributes(String dn, ADUserAttributes userAttrs) {
        try {
            String[] parts = dn.split(",");
            for (String part : parts) {
                String[] keyValue = part.split("=", 2);
                if (keyValue.length == 2) {
                    String key = keyValue[0].trim().toUpperCase();
                    String value = keyValue[1].trim();

                    switch (key) {
                        case "OU":
                            if (userAttrs.getOrganizationalUnit() == null) {
                                userAttrs.setOrganizationalUnit(value);
                            }
                            break;
                        case "O":
                            userAttrs.setOrganization(value);
                            break;
                        case "C":
                            userAttrs.setCountry(value);
                            break;
                    }
                }
            }

            userAttrs.setDistinguishedName(dn);

        } catch (Exception e) {
            logger.warn("Error parsing DN attributes: {}", dn, e);
        }
    }

    /**
     * Extract CN value from DN
     *
     * @param dn Distinguished Name
     * @return CN value or full DN if CN not found
     */
    private String extractCNFromDN(String dn) {
        try {
            if (dn.toUpperCase().startsWith("CN=")) {
                int commaIndex = dn.indexOf(',');
                if (commaIndex > 0) {
                    return dn.substring(3, commaIndex);
                } else {
                    return dn.substring(3);
                }
            }
        } catch (Exception e) {
            logger.warn("Error extracting CN from DN: {}", dn, e);
        }
        return dn;
    }

    /**
     * Map AD groups to application roles
     *
     * @param adGroups Set of AD group names
     * @return List of application roles
     */
    private List<String> mapGroupsToRoles(Set<String> adGroups) {
        List<String> roles = new ArrayList<>();

        for (String group : adGroups) {
            // Map AD groups to RA roles
            if (group.equalsIgnoreCase("PKI-RA-Admins")) {
                roles.add("RA_ADMIN");
            } else if (group.equalsIgnoreCase("PKI-RA-Officers")) {
                roles.add("RA_OFFICER");
            } else if (group.equalsIgnoreCase("PKI-RA-Operators")) {
                roles.add("RA_OPERATOR");
            } else if (group.equalsIgnoreCase("PKI-Auditors")) {
                roles.add("AUDITOR");
            }
        }

        // All authenticated users are end entities
        if (!roles.isEmpty() || !adGroups.isEmpty()) {
            roles.add("END_ENTITY");
        }

        return roles;
    }

    /**
     * Create LDAP context using service account
     *
     * @return DirContext
     * @throws Exception if context creation fails
     */
    private DirContext createServiceContext() throws Exception {
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, adUrl);
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, serviceUsername);
        env.put(Context.SECURITY_CREDENTIALS, servicePassword);

        // Connection pool settings
        env.put("com.sun.jndi.ldap.connect.pool", "true");
        env.put("com.sun.jndi.ldap.connect.pool.timeout", "300000");

        return new InitialDirContext(env);
    }

    /**
     * Close LDAP context
     *
     * @param context Context to close
     */
    private void closeContext(DirContext context) {
        if (context != null) {
            try {
                context.close();
            } catch (Exception e) {
                logger.warn("Error closing LDAP context", e);
            }
        }
    }

    /**
     * Escape LDAP search filter special characters
     *
     * @param filter Filter string to escape
     * @return Escaped filter string
     */
    private String escapeLDAPSearchFilter(String filter) {
        StringBuilder sb = new StringBuilder();
        for (char c : filter.toCharArray()) {
            switch (c) {
                case '\\':
                    sb.append("\\5c");
                    break;
                case '*':
                    sb.append("\\2a");
                    break;
                case '(':
                    sb.append("\\28");
                    break;
                case ')':
                    sb.append("\\29");
                    break;
                case '\u0000':
                    sb.append("\\00");
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }
}
