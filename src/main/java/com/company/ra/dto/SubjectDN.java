package com.company.ra.dto;

/**
 * DTO representing Subject Distinguished Name from CSR
 */
public class SubjectDN {

    private String rawDN;
    private String commonName;
    private String email;
    private String organizationalUnit;
    private String organization;
    private String country;

    public SubjectDN() {
    }

    public String getRawDN() {
        return rawDN;
    }

    public void setRawDN(String rawDN) {
        this.rawDN = rawDN;
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

    @Override
    public String toString() {
        return rawDN != null ? rawDN : "";
    }
}
