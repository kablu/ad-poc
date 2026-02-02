package ad.poc.repository;

import ad.poc.model.AdUser;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.filter.AndFilter;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.ldap.filter.LikeFilter;
import org.springframework.ldap.filter.OrFilter;
import org.springframework.ldap.query.LdapQuery;
import org.springframework.ldap.query.LdapQueryBuilder;
import org.springframework.ldap.support.LdapNameBuilder;
import org.springframework.stereotype.Repository;

import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import java.util.List;

@Repository
public class AdUserLdapRepository {

    private static final String USER_SEARCH_BASE = "OU=Users";
    private static final String[] USER_ATTRIBUTES = {
            "sAMAccountName", "cn", "displayName", "givenName", "sn",
            "mail", "department", "title", "telephoneNumber", "company",
            "distinguishedName", "userPrincipalName", "memberOf",
            "userAccountControl"
    };

    private final LdapTemplate ldapTemplate;

    public AdUserLdapRepository(LdapTemplate ldapTemplate) {
        this.ldapTemplate = ldapTemplate;
    }

    /**
     * LIST - Get all AD users in the Users OU.
     */
    public List<AdUser> findAll() {
        LdapQuery query = LdapQueryBuilder.query()
                .base(USER_SEARCH_BASE)
                .attributes(USER_ATTRIBUTES)
                .where("objectClass").is("user")
                .and("objectCategory").is("person");

        return ldapTemplate.search(query, new AdUserAttributesMapper());
    }

    /**
     * SEARCH - Find a single user by sAMAccountName.
     */
    public AdUser findBySamAccountName(String samAccountName) {
        LdapQuery query = LdapQueryBuilder.query()
                .base(USER_SEARCH_BASE)
                .attributes(USER_ATTRIBUTES)
                .where("objectClass").is("user")
                .and("sAMAccountName").is(samAccountName);

        List<AdUser> results = ldapTemplate.search(query, new AdUserAttributesMapper());
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * SEARCH - Find users by department.
     */
    public List<AdUser> findByDepartment(String department) {
        LdapQuery query = LdapQueryBuilder.query()
                .base(USER_SEARCH_BASE)
                .attributes(USER_ATTRIBUTES)
                .where("objectClass").is("user")
                .and("department").is(department);

        return ldapTemplate.search(query, new AdUserAttributesMapper());
    }

    /**
     * SEARCH - Search users by keyword across displayName, mail, sAMAccountName, department, title.
     */
    public List<AdUser> search(String keyword) {
        AndFilter andFilter = new AndFilter();
        andFilter.and(new EqualsFilter("objectClass", "user"));
        andFilter.and(new EqualsFilter("objectCategory", "person"));

        OrFilter orFilter = new OrFilter();
        orFilter.or(new LikeFilter("displayName", "*" + keyword + "*"));
        orFilter.or(new LikeFilter("mail", "*" + keyword + "*"));
        orFilter.or(new LikeFilter("sAMAccountName", "*" + keyword + "*"));
        orFilter.or(new LikeFilter("department", "*" + keyword + "*"));
        orFilter.or(new LikeFilter("title", "*" + keyword + "*"));
        andFilter.and(orFilter);

        return ldapTemplate.search(
                USER_SEARCH_BASE,
                andFilter.encode(),
                new AdUserAttributesMapper()
        );
    }

    /**
     * SEARCH - Find users by email address.
     */
    public AdUser findByEmail(String email) {
        LdapQuery query = LdapQueryBuilder.query()
                .base(USER_SEARCH_BASE)
                .attributes(USER_ATTRIBUTES)
                .where("objectClass").is("user")
                .and("mail").is(email);

        List<AdUser> results = ldapTemplate.search(query, new AdUserAttributesMapper());
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * INSERT - Create a new user in Active Directory.
     */
    public void create(AdUser user) {
        Name dn = buildDn(user.getCommonName());

        DirContextAdapter context = new DirContextAdapter(dn);
        context.setAttributeValues("objectClass",
                new String[]{"top", "person", "organizationalPerson", "user"});
        context.setAttributeValue("cn", user.getCommonName());
        context.setAttributeValue("sAMAccountName", user.getSamAccountName());

        setAttributeIfNotNull(context, "displayName", user.getDisplayName());
        setAttributeIfNotNull(context, "givenName", user.getFirstName());
        setAttributeIfNotNull(context, "sn", user.getLastName());
        setAttributeIfNotNull(context, "mail", user.getEmail());
        setAttributeIfNotNull(context, "department", user.getDepartment());
        setAttributeIfNotNull(context, "title", user.getTitle());
        setAttributeIfNotNull(context, "telephoneNumber", user.getPhoneNumber());
        setAttributeIfNotNull(context, "company", user.getCompany());
        setAttributeIfNotNull(context, "userPrincipalName", user.getUserPrincipalName());

        ldapTemplate.bind(context);
    }

    /**
     * UPDATE - Modify an existing user's attributes in Active Directory.
     */
    public void update(String samAccountName, AdUser updatedUser) {
        AdUser existing = findBySamAccountName(samAccountName);
        if (existing == null) {
            throw new IllegalArgumentException("User not found: " + samAccountName);
        }

        Name dn = existing.getDn();
        DirContextOperations context = ldapTemplate.lookupContext(dn);

        setAttributeIfNotNull(context, "displayName", updatedUser.getDisplayName());
        setAttributeIfNotNull(context, "givenName", updatedUser.getFirstName());
        setAttributeIfNotNull(context, "sn", updatedUser.getLastName());
        setAttributeIfNotNull(context, "mail", updatedUser.getEmail());
        setAttributeIfNotNull(context, "department", updatedUser.getDepartment());
        setAttributeIfNotNull(context, "title", updatedUser.getTitle());
        setAttributeIfNotNull(context, "telephoneNumber", updatedUser.getPhoneNumber());
        setAttributeIfNotNull(context, "company", updatedUser.getCompany());
        setAttributeIfNotNull(context, "userPrincipalName", updatedUser.getUserPrincipalName());

        ldapTemplate.modifyAttributes(context);
    }

    /**
     * DELETE - Remove a user from Active Directory by sAMAccountName.
     */
    public void delete(String samAccountName) {
        AdUser existing = findBySamAccountName(samAccountName);
        if (existing == null) {
            throw new IllegalArgumentException("User not found: " + samAccountName);
        }
        ldapTemplate.unbind(existing.getDn());
    }

    private Name buildDn(String commonName) {
        return LdapNameBuilder.newInstance(USER_SEARCH_BASE)
                .add("cn", commonName)
                .build();
    }

    private void setAttributeIfNotNull(DirContextOperations context, String attrName, String value) {
        if (value != null) {
            context.setAttributeValue(attrName, value);
        }
    }

    /**
     * Maps LDAP attributes to AdUser model.
     */
    private static class AdUserAttributesMapper implements AttributesMapper<AdUser> {

        @Override
        public AdUser mapFromAttributes(Attributes attrs) throws NamingException {
            AdUser user = new AdUser();
            user.setSamAccountName(getStringAttribute(attrs, "sAMAccountName"));
            user.setCommonName(getStringAttribute(attrs, "cn"));
            user.setDisplayName(getStringAttribute(attrs, "displayName"));
            user.setFirstName(getStringAttribute(attrs, "givenName"));
            user.setLastName(getStringAttribute(attrs, "sn"));
            user.setEmail(getStringAttribute(attrs, "mail"));
            user.setDepartment(getStringAttribute(attrs, "department"));
            user.setTitle(getStringAttribute(attrs, "title"));
            user.setPhoneNumber(getStringAttribute(attrs, "telephoneNumber"));
            user.setCompany(getStringAttribute(attrs, "company"));
            user.setDistinguishedName(getStringAttribute(attrs, "distinguishedName"));
            user.setUserPrincipalName(getStringAttribute(attrs, "userPrincipalName"));
            user.setUserAccountControl(getStringAttribute(attrs, "userAccountControl"));

            if (attrs.get("memberOf") != null) {
                int size = attrs.get("memberOf").size();
                String[] groups = new String[size];
                for (int i = 0; i < size; i++) {
                    groups[i] = (String) attrs.get("memberOf").get(i);
                }
                user.setMemberOf(groups);
            }

            return user;
        }

        private String getStringAttribute(Attributes attrs, String name) throws NamingException {
            if (attrs.get(name) != null) {
                return (String) attrs.get(name).get();
            }
            return null;
        }
    }
}
