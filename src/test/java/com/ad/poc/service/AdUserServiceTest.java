package com.ad.poc.service;

import com.ad.poc.dto.AdUserDto;
import com.ad.poc.model.AdUser;
import com.ad.poc.repository.AdUserLdapRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdUserServiceTest {

    @Mock
    private AdUserLdapRepository ldapRepository;

    @InjectMocks
    private AdUserService adUserService;

    private AdUser sampleAdUser;
    private AdUserDto sampleDto;

    @BeforeEach
    void setUp() {
        sampleAdUser = new AdUser();
        sampleAdUser.setSamAccountName("jdoe");
        sampleAdUser.setCommonName("John Doe");
        sampleAdUser.setDisplayName("John Doe");
        sampleAdUser.setFirstName("John");
        sampleAdUser.setLastName("Doe");
        sampleAdUser.setEmail("jdoe@company.com");
        sampleAdUser.setDepartment("Engineering");
        sampleAdUser.setTitle("Developer");
        sampleAdUser.setPhoneNumber("+1-555-0100");
        sampleAdUser.setCompany("Company Inc");
        sampleAdUser.setDistinguishedName("CN=John Doe,OU=Users,DC=company,DC=com");
        sampleAdUser.setUserPrincipalName("jdoe@company.com");
        sampleAdUser.setUserAccountControl("512"); // enabled
        sampleAdUser.setMemberOf(new String[]{"CN=Developers,OU=Groups,DC=company,DC=com"});

        sampleDto = new AdUserDto("jdoe", "John", "Doe", "jdoe@company.com");
        sampleDto.setDisplayName("John Doe");
        sampleDto.setDepartment("Engineering");
        sampleDto.setTitle("Developer");
        sampleDto.setPhoneNumber("+1-555-0100");
        sampleDto.setCompany("Company Inc");
        sampleDto.setUserPrincipalName("jdoe@company.com");
    }

    // ---- LIST ----

    @Test
    void listAll_shouldReturnAllUsers() {
        AdUser secondUser = new AdUser();
        secondUser.setSamAccountName("asmith");
        secondUser.setFirstName("Alice");
        secondUser.setLastName("Smith");
        secondUser.setDisplayName("Alice Smith");

        when(ldapRepository.findAll()).thenReturn(Arrays.asList(sampleAdUser, secondUser));

        List<AdUserDto> result = adUserService.listAll();

        assertEquals(2, result.size());
        assertEquals("jdoe", result.get(0).getSamAccountName());
        assertEquals("asmith", result.get(1).getSamAccountName());
        verify(ldapRepository).findAll();
    }

    @Test
    void listAll_shouldReturnEmptyListWhenNoUsers() {
        when(ldapRepository.findAll()).thenReturn(Collections.emptyList());

        List<AdUserDto> result = adUserService.listAll();

        assertTrue(result.isEmpty());
    }

    @Test
    void getByDepartment_shouldReturnFilteredUsers() {
        when(ldapRepository.findByDepartment("Engineering"))
                .thenReturn(List.of(sampleAdUser));

        List<AdUserDto> result = adUserService.getByDepartment("Engineering");

        assertEquals(1, result.size());
        assertEquals("Engineering", result.get(0).getDepartment());
    }

    // ---- SEARCH ----

    @Test
    void search_shouldReturnMatchingUsers() {
        when(ldapRepository.search("john")).thenReturn(List.of(sampleAdUser));

        List<AdUserDto> result = adUserService.search("john");

        assertEquals(1, result.size());
        assertEquals("jdoe", result.get(0).getSamAccountName());
        verify(ldapRepository).search("john");
    }

    @Test
    void search_shouldReturnEmptyWhenNoMatch() {
        when(ldapRepository.search("nonexistent")).thenReturn(Collections.emptyList());

        List<AdUserDto> result = adUserService.search("nonexistent");

        assertTrue(result.isEmpty());
    }

    @Test
    void getBySamAccountName_shouldReturnUserWhenFound() {
        when(ldapRepository.findBySamAccountName("jdoe")).thenReturn(sampleAdUser);

        Optional<AdUserDto> result = adUserService.getBySamAccountName("jdoe");

        assertTrue(result.isPresent());
        assertEquals("jdoe", result.get().getSamAccountName());
        assertEquals("John Doe", result.get().getDisplayName());
        assertEquals("jdoe@company.com", result.get().getEmail());
    }

    @Test
    void getBySamAccountName_shouldReturnEmptyWhenNotFound() {
        when(ldapRepository.findBySamAccountName("nonexistent")).thenReturn(null);

        Optional<AdUserDto> result = adUserService.getBySamAccountName("nonexistent");

        assertTrue(result.isEmpty());
    }

    // ---- INSERT (CREATE) ----

    @Test
    void create_shouldInsertNewUserInAd() {
        when(ldapRepository.findBySamAccountName("jdoe"))
                .thenReturn(null)       // first call: check existence
                .thenReturn(sampleAdUser); // second call: fetch after create

        doNothing().when(ldapRepository).create(any(AdUser.class));

        AdUserDto result = adUserService.create(sampleDto);

        assertNotNull(result);
        assertEquals("jdoe", result.getSamAccountName());
        assertEquals("John Doe", result.getDisplayName());
        verify(ldapRepository).create(any(AdUser.class));
    }

    @Test
    void create_shouldThrowWhenUserAlreadyExists() {
        when(ldapRepository.findBySamAccountName("jdoe")).thenReturn(sampleAdUser);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> adUserService.create(sampleDto)
        );

        assertTrue(ex.getMessage().contains("already exists"));
        verify(ldapRepository, never()).create(any());
    }

    @Test
    void create_shouldSetDisplayNameFromFirstAndLastWhenNull() {
        sampleDto.setDisplayName(null);

        when(ldapRepository.findBySamAccountName("jdoe"))
                .thenReturn(null)
                .thenReturn(sampleAdUser);
        doNothing().when(ldapRepository).create(any(AdUser.class));

        adUserService.create(sampleDto);

        verify(ldapRepository).create(argThat(user ->
                "John Doe".equals(user.getDisplayName())
        ));
    }

    // ---- UPDATE ----

    @Test
    void update_shouldModifyUserAttributes() {
        AdUser updatedAdUser = new AdUser();
        updatedAdUser.setSamAccountName("jdoe");
        updatedAdUser.setDisplayName("John Updated");
        updatedAdUser.setFirstName("John");
        updatedAdUser.setLastName("Doe");
        updatedAdUser.setEmail("jdoe-new@company.com");
        updatedAdUser.setDepartment("Management");

        doNothing().when(ldapRepository).update(eq("jdoe"), any(AdUser.class));
        when(ldapRepository.findBySamAccountName("jdoe")).thenReturn(updatedAdUser);

        AdUserDto updateDto = new AdUserDto("jdoe", "John", "Doe", "jdoe-new@company.com");
        updateDto.setDisplayName("John Updated");
        updateDto.setDepartment("Management");

        AdUserDto result = adUserService.update("jdoe", updateDto);

        assertEquals("John Updated", result.getDisplayName());
        assertEquals("Management", result.getDepartment());
        verify(ldapRepository).update(eq("jdoe"), any(AdUser.class));
    }

    @Test
    void update_shouldPropagateExceptionWhenUserNotFound() {
        doThrow(new IllegalArgumentException("User not found: unknown"))
                .when(ldapRepository).update(eq("unknown"), any(AdUser.class));

        AdUserDto updateDto = new AdUserDto("unknown", "X", "Y", "x@y.com");

        assertThrows(IllegalArgumentException.class,
                () -> adUserService.update("unknown", updateDto));
    }

    // ---- DELETE ----

    @Test
    void delete_shouldRemoveUserFromAd() {
        doNothing().when(ldapRepository).delete("jdoe");

        adUserService.delete("jdoe");

        verify(ldapRepository).delete("jdoe");
    }

    @Test
    void delete_shouldPropagateExceptionWhenUserNotFound() {
        doThrow(new IllegalArgumentException("User not found: unknown"))
                .when(ldapRepository).delete("unknown");

        assertThrows(IllegalArgumentException.class,
                () -> adUserService.delete("unknown"));
    }

    // ---- DTO MAPPING ----

    @Test
    void toDto_shouldMapUserAccountControlEnabled() {
        sampleAdUser.setUserAccountControl("512"); // normal enabled
        when(ldapRepository.findBySamAccountName("jdoe")).thenReturn(sampleAdUser);

        Optional<AdUserDto> result = adUserService.getBySamAccountName("jdoe");

        assertTrue(result.isPresent());
        assertTrue(result.get().isEnabled());
    }

    @Test
    void toDto_shouldMapUserAccountControlDisabled() {
        sampleAdUser.setUserAccountControl("514"); // disabled (512 + 2)
        when(ldapRepository.findBySamAccountName("jdoe")).thenReturn(sampleAdUser);

        Optional<AdUserDto> result = adUserService.getBySamAccountName("jdoe");

        assertTrue(result.isPresent());
        assertFalse(result.get().isEnabled());
    }

    @Test
    void toDto_shouldMapMemberOfGroups() {
        when(ldapRepository.findBySamAccountName("jdoe")).thenReturn(sampleAdUser);

        Optional<AdUserDto> result = adUserService.getBySamAccountName("jdoe");

        assertTrue(result.isPresent());
        assertNotNull(result.get().getMemberOf());
        assertEquals(1, result.get().getMemberOf().size());
        assertTrue(result.get().getMemberOf().get(0).contains("Developers"));
    }

    @Test
    void toDto_shouldHandleNullMemberOf() {
        sampleAdUser.setMemberOf(null);
        when(ldapRepository.findBySamAccountName("jdoe")).thenReturn(sampleAdUser);

        Optional<AdUserDto> result = adUserService.getBySamAccountName("jdoe");

        assertTrue(result.isPresent());
        assertNotNull(result.get().getMemberOf());
        assertTrue(result.get().getMemberOf().isEmpty());
    }

    @Test
    void toDto_shouldHandleNullUserAccountControl() {
        sampleAdUser.setUserAccountControl(null);
        when(ldapRepository.findBySamAccountName("jdoe")).thenReturn(sampleAdUser);

        Optional<AdUserDto> result = adUserService.getBySamAccountName("jdoe");

        assertTrue(result.isPresent());
        assertTrue(result.get().isEnabled()); // default to enabled
    }
}
