package ad.poc.service;

import ad.poc.dto.AdUserDto;
import ad.poc.model.AdUser;
import ad.poc.repository.AdUserLdapRepository;
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

    private static final int UF_ACCOUNT_DISABLE = 2;

    private final AdUserLdapRepository ldapRepository;

    public AdUserService(AdUserLdapRepository ldapRepository) {
        this.ldapRepository = ldapRepository;
    }

    /**
     * LIST - Get all users from Active Directory.
     */
    public List<AdUserDto> listAll() {
        log.debug("Listing all AD users");
        List<AdUser> users = ldapRepository.findAll();
        return users.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * SEARCH - Get a single user by sAMAccountName.
     */
    public Optional<AdUserDto> getBySamAccountName(String samAccountName) {
        log.debug("Looking up AD user: {}", samAccountName);
        AdUser user = ldapRepository.findBySamAccountName(samAccountName);
        return Optional.ofNullable(user).map(this::toDto);
    }

    /**
     * SEARCH - Search users by keyword across displayName, mail, sAMAccountName, department, title.
     */
    public List<AdUserDto> search(String keyword) {
        log.debug("Searching AD users with keyword: {}", keyword);
        List<AdUser> users = ldapRepository.search(keyword);
        return users.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * SEARCH - Filter users by department.
     */
    public List<AdUserDto> getByDepartment(String department) {
        log.debug("Searching AD users by department: {}", department);
        List<AdUser> users = ldapRepository.findByDepartment(department);
        return users.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * INSERT - Create a new user in Active Directory.
     */
    public AdUserDto create(AdUserDto dto) {
        log.info("Creating AD user: {}", dto.getSamAccountName());

        AdUser existing = ldapRepository.findBySamAccountName(dto.getSamAccountName());
        if (existing != null) {
            throw new IllegalArgumentException(
                    "User with sAMAccountName '" + dto.getSamAccountName() + "' already exists in AD");
        }

        AdUser user = toModel(dto);
        ldapRepository.create(user);

        log.info("AD user created successfully: {}", dto.getSamAccountName());
        AdUser created = ldapRepository.findBySamAccountName(dto.getSamAccountName());
        return toDto(created);
    }

    /**
     * UPDATE - Update an existing user's attributes in Active Directory.
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
     * DELETE - Remove a user from Active Directory.
     */
    public void delete(String samAccountName) {
        log.info("Deleting AD user: {}", samAccountName);
        ldapRepository.delete(samAccountName);
        log.info("AD user deleted successfully: {}", samAccountName);
    }

    private AdUser toModel(AdUserDto dto) {
        AdUser user = new AdUser();
        user.setSamAccountName(dto.getSamAccountName());
        user.setCommonName(dto.getFirstName() + " " + dto.getLastName());
        user.setDisplayName(dto.getDisplayName() != null
                ? dto.getDisplayName()
                : dto.getFirstName() + " " + dto.getLastName());
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setEmail(dto.getEmail());
        user.setDepartment(dto.getDepartment());
        user.setTitle(dto.getTitle());
        user.setPhoneNumber(dto.getPhoneNumber());
        user.setCompany(dto.getCompany());
        user.setUserPrincipalName(dto.getUserPrincipalName());
        return user;
    }

    private AdUserDto toDto(AdUser user) {
        AdUserDto dto = new AdUserDto();
        dto.setSamAccountName(user.getSamAccountName());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setDisplayName(user.getDisplayName());
        dto.setEmail(user.getEmail());
        dto.setDepartment(user.getDepartment());
        dto.setTitle(user.getTitle());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setCompany(user.getCompany());
        dto.setDistinguishedName(user.getDistinguishedName());
        dto.setUserPrincipalName(user.getUserPrincipalName());

        if (user.getMemberOf() != null) {
            dto.setMemberOf(Arrays.asList(user.getMemberOf()));
        } else {
            dto.setMemberOf(Collections.emptyList());
        }

        if (user.getUserAccountControl() != null) {
            try {
                int uac = Integer.parseInt(user.getUserAccountControl());
                dto.setEnabled((uac & UF_ACCOUNT_DISABLE) == 0);
            } catch (NumberFormatException e) {
                dto.setEnabled(true);
            }
        } else {
            dto.setEnabled(true);
        }

        return dto;
    }
}
