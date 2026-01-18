package com.company.ra.dto;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * DTO representing user attributes from Active Directory
 */
public class ADUserAttributes {

    private String username;
    private String commonName;
    private String email;
    private String organizationalUnit;
    private String organization;
    private String country;
    private String distinguishedName;
    private List<String> roles = new ArrayList<>();
    private Set<String> adGroups = new HashSet<>();

    public ADUserAttributes() {
    }

    public ADUserAttributes(String username, String commonName, String email) {
        this.username = username;
        this.commonName = commonName;
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getCommonName() {
        return commonName;
    }

    public void setCommonName(String commonName) {
        this.commonName = commonName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getOrganizationalUnit() {
        return organizationalUnit;
    }

    public void setOrganizationalUnit(String organizationalUnit) {
        this.organizationalUnit = organizationalUnit;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getDistinguishedName() {
        return distinguishedName;
    }

    public void setDistinguishedName(String distinguishedName) {
        this.distinguishedName = distinguishedName;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public Set<String> getAdGroups() {
        return adGroups;
    }

    public void setAdGroups(Set<String> adGroups) {
        this.adGroups = adGroups;
    }

    @Override
    public String toString() {
        return "ADUserAttributes{" +
                "username='" + username + '\'' +
                ", commonName='" + commonName + '\'' +
                ", email='" + email + '\'' +
                ", organizationalUnit='" + organizationalUnit + '\'' +
                ", organization='" + organization + '\'' +
                ", roles=" + roles +
                '}';
    }
}
