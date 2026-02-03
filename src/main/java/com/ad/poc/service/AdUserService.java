package com.ad.poc.service;

import com.ad.poc.dto.AdUserDto;
import com.ad.poc.model.AdUser;
import com.ad.poc.repository.AdUserLdapRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AdUserService {

    private static final Logger log = LoggerFactory.getLogger(AdUserService.class);

    // AD userAccountControl flag: 512 = normal account enabled
    private static final int UF_NORMAL_ACCOUNT = 512;
    // AD userAccountControl flag: 514 = normal account disabled
    private static final int UF_ACCOUNT_DISABLE = 2;

    private final AdUserLdapRepository ldapRepository;

    public AdUserService(AdUserLdapRepository ldapRepository) {
        this.ldapRepository = ldapRepository;
    }

    /**
     * List all users from Active Directory.
     */
    public List<AdUserDto> listAll() {
        log.debug("Listing all AD users");
        List<AdUser> users = ldapRepository.findAll();
        return users.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Get a single user by sAMAccountName.
     */
    public Optional<AdUserDto> getBySamAccountName(String samAccountName) {
        log.debug("Looking up AD user: {}", samAccountName);
        AdUser user = ldapRepository.findBySamAccountName(samAccountName);
        return Optional.ofNullable(user).map(this::toDto);
    }

    /**
     * Search users by keyword across displayName, mail, sAMAccountName, department, title.
     */
    public List<AdUserDto> search(String keyword) {
        log.debug("Searching AD users with keyword: {}", keyword);
        List<AdUser> users = ldapRepository.search(keyword);
        return users.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Search users by department.
     */
    public List<AdUserDto> getByDepartment(String department) {
        log.debug("Searching AD users by department: {}", department);
        List<AdUser> users = ldapRepository.findByDepartment(department);
        return users.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Insert (create) a new user in Active Directory.
     */
    public AdUserDto create(AdUserDto dto) {
        log.info("Creating AD user: {}", dto.samAccountName());

        AdUser existing = ldapRepository.findBySamAccountName(dto.samAccountName());
        if (existing != null) {
            throw new IllegalArgumentException(
                    "User with sAMAccountName '" + dto.samAccountName() + "' already exists in AD");
        }

        AdUser user = toModel(dto);
        ldapRepository.create(user);

        log.info("AD user created successfully: {}", dto.samAccountName());
        AdUser created = ldapRepository.findBySamAccountName(dto.samAccountName());
        return toDto(created);
    }

    /**
     * Update an existing user's attributes in Active Directory.
     */
    public AdUserDto update(String samAccountName, AdUserDto dto) {
        log.info("Updating AD user: {}", samAccountName);

        AdUser updatedModel = toModel(dto);
        ldapRepository.update(samAccountName, updatedModel);

        log.info("AD user updated successfully: {}", samAccountName);
        AdUser updated = ldapRepository.findBySamAccountName(samAccountName);
        return toDto(updated);
    }

    /**
     * Delete a user from Active Directory by sAMAccountName.
     */
    public void delete(String samAccountName) {
        log.info("Deleting AD user: {}", samAccountName);
        ldapRepository.delete(samAccountName);
        log.info("AD user deleted successfully: {}", samAccountName);
    }

    private AdUser toModel(AdUserDto dto) {
        AdUser user = new AdUser();
        user.setSamAccountName(dto.samAccountName());
        user.setCommonName(dto.firstName() + " " + dto.lastName());
        user.setDisplayName(dto.displayName() != null
                ? dto.displayName()
                : dto.firstName() + " " + dto.lastName());
        user.setFirstName(dto.firstName());
        user.setLastName(dto.lastName());
        user.setEmail(dto.email());
        user.setDepartment(dto.department());
        user.setTitle(dto.title());
        user.setPhoneNumber(dto.phoneNumber());
        user.setCompany(dto.company());
        user.setUserPrincipalName(dto.userPrincipalName());
        return user;
    }

    private AdUserDto toDto(AdUser user) {
        List<String> memberOfList = user.getMemberOf() != null
                ? Arrays.asList(user.getMemberOf())
                : Collections.emptyList();

        boolean enabled = true;
        if (user.getUserAccountControl() != null) {
            try {
                int uac = Integer.parseInt(user.getUserAccountControl());
                enabled = (uac & UF_ACCOUNT_DISABLE) == 0;
            } catch (NumberFormatException e) {
                // default to enabled
            }
        }

        return new AdUserDto(
                user.getSamAccountName(),
                user.getFirstName(),
                user.getLastName(),
                user.getDisplayName(),
                user.getEmail(),
                user.getDepartment(),
                user.getTitle(),
                user.getPhoneNumber(),
                user.getCompany(),
                user.getDistinguishedName(),
                user.getUserPrincipalName(),
                memberOfList,
                enabled
        );
    }
}
