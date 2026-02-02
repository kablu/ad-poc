package ad.poc.service;

import ad.poc.dto.AdUserDto;
import ad.poc.model.AdUser;
import ad.poc.repository.AdUserLdapRepository;
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

    private AdUser sampleUser;
    private AdUserDto sampleDto;

    @BeforeEach
    void setUp() {
        sampleUser = new AdUser();
        sampleUser.setSamAccountName("jdoe");
        sampleUser.setCommonName("John Doe");
        sampleUser.setDisplayName("John Doe");
        sampleUser.setFirstName("John");
        sampleUser.setLastName("Doe");
        sampleUser.setEmail("jdoe@company.com");
        sampleUser.setDepartment("Engineering");
        sampleUser.setTitle("Developer");
        sampleUser.setUserAccountControl("512"); // Normal enabled account

        sampleDto = new AdUserDto("jdoe", "John", "Doe", "jdoe@company.com");
        sampleDto.setDepartment("Engineering");
        sampleDto.setTitle("Developer");
    }

    // ---- LIST ----

    @Test
    void listAll_shouldReturnAllUsersAsDtos() {
        when(ldapRepository.findAll()).thenReturn(List.of(sampleUser));

        List<AdUserDto> result = adUserService.listAll();

        assertEquals(1, result.size());
        assertEquals("jdoe", result.get(0).getSamAccountName());
        assertEquals("John Doe", result.get(0).getDisplayName());
        verify(ldapRepository).findAll();
    }

    @Test
    void listAll_shouldReturnEmptyList() {
        when(ldapRepository.findAll()).thenReturn(Collections.emptyList());

        List<AdUserDto> result = adUserService.listAll();

        assertTrue(result.isEmpty());
    }

    // ---- SEARCH ----

    @Test
    void getBySamAccountName_shouldReturnUserWhenFound() {
        when(ldapRepository.findBySamAccountName("jdoe")).thenReturn(sampleUser);

        Optional<AdUserDto> result = adUserService.getBySamAccountName("jdoe");

        assertTrue(result.isPresent());
        assertEquals("jdoe", result.get().getSamAccountName());
    }

    @Test
    void getBySamAccountName_shouldReturnEmptyWhenNotFound() {
        when(ldapRepository.findBySamAccountName("nonexistent")).thenReturn(null);

        Optional<AdUserDto> result = adUserService.getBySamAccountName("nonexistent");

        assertTrue(result.isEmpty());
    }

    @Test
    void search_shouldReturnMatchingUsers() {
        when(ldapRepository.search("john")).thenReturn(List.of(sampleUser));

        List<AdUserDto> result = adUserService.search("john");

        assertEquals(1, result.size());
        assertEquals("jdoe", result.get(0).getSamAccountName());
    }

    @Test
    void search_shouldReturnEmptyWhenNoMatch() {
        when(ldapRepository.search("xyz")).thenReturn(Collections.emptyList());

        List<AdUserDto> result = adUserService.search("xyz");

        assertTrue(result.isEmpty());
    }

    @Test
    void getByDepartment_shouldReturnFilteredUsers() {
        when(ldapRepository.findByDepartment("Engineering")).thenReturn(List.of(sampleUser));

        List<AdUserDto> result = adUserService.getByDepartment("Engineering");

        assertEquals(1, result.size());
        assertEquals("Engineering", result.get(0).getDepartment());
    }

    // ---- INSERT ----

    @Test
    void create_shouldCreateNewUser() {
        when(ldapRepository.findBySamAccountName("jdoe"))
                .thenReturn(null)        // first call: duplicate check
                .thenReturn(sampleUser); // second call: return created user

        doNothing().when(ldapRepository).create(any(AdUser.class));

        AdUserDto result = adUserService.create(sampleDto);

        assertNotNull(result);
        assertEquals("jdoe", result.getSamAccountName());
        verify(ldapRepository).create(any(AdUser.class));
    }

    @Test
    void create_shouldThrowWhenDuplicateSamAccountName() {
        when(ldapRepository.findBySamAccountName("jdoe")).thenReturn(sampleUser);

        assertThrows(IllegalArgumentException.class, () -> adUserService.create(sampleDto));
        verify(ldapRepository, never()).create(any());
    }

    // ---- UPDATE ----

    @Test
    void update_shouldUpdateExistingUser() {
        doNothing().when(ldapRepository).update(eq("jdoe"), any(AdUser.class));
        when(ldapRepository.findBySamAccountName("jdoe")).thenReturn(sampleUser);

        AdUserDto updateDto = new AdUserDto("jdoe", "John", "Doe", "jdoe-new@company.com");
        AdUserDto result = adUserService.update("jdoe", updateDto);

        assertNotNull(result);
        verify(ldapRepository).update(eq("jdoe"), any(AdUser.class));
    }

    @Test
    void update_shouldThrowWhenUserNotFound() {
        doThrow(new IllegalArgumentException("User not found: nonexistent"))
                .when(ldapRepository).update(eq("nonexistent"), any(AdUser.class));

        AdUserDto updateDto = new AdUserDto("nonexistent", "X", "Y", "x@y.com");

        assertThrows(IllegalArgumentException.class,
                () -> adUserService.update("nonexistent", updateDto));
    }

    // ---- DELETE ----

    @Test
    void delete_shouldDeleteUser() {
        doNothing().when(ldapRepository).delete("jdoe");

        adUserService.delete("jdoe");

        verify(ldapRepository).delete("jdoe");
    }

    @Test
    void delete_shouldThrowWhenUserNotFound() {
        doThrow(new IllegalArgumentException("User not found: nonexistent"))
                .when(ldapRepository).delete("nonexistent");

        assertThrows(IllegalArgumentException.class,
                () -> adUserService.delete("nonexistent"));
    }

    // ---- DTO mapping ----

    @Test
    void toDto_shouldParseEnabledAccount() {
        sampleUser.setUserAccountControl("512"); // enabled
        when(ldapRepository.findBySamAccountName("jdoe")).thenReturn(sampleUser);

        Optional<AdUserDto> result = adUserService.getBySamAccountName("jdoe");

        assertTrue(result.isPresent());
        assertTrue(result.get().isEnabled());
    }

    @Test
    void toDto_shouldParseDisabledAccount() {
        sampleUser.setUserAccountControl("514"); // disabled (512 + 2)
        when(ldapRepository.findBySamAccountName("jdoe")).thenReturn(sampleUser);

        Optional<AdUserDto> result = adUserService.getBySamAccountName("jdoe");

        assertTrue(result.isPresent());
        assertFalse(result.get().isEnabled());
    }

    @Test
    void toDto_shouldMapMemberOfGroups() {
        sampleUser.setMemberOf(new String[]{
                "CN=Developers,OU=Groups,DC=company,DC=com",
                "CN=Admins,OU=Groups,DC=company,DC=com"
        });
        when(ldapRepository.findBySamAccountName("jdoe")).thenReturn(sampleUser);

        Optional<AdUserDto> result = adUserService.getBySamAccountName("jdoe");

        assertTrue(result.isPresent());
        assertEquals(2, result.get().getMemberOf().size());
    }

    @Test
    void toDto_shouldHandleNullMemberOf() {
        sampleUser.setMemberOf(null);
        when(ldapRepository.findBySamAccountName("jdoe")).thenReturn(sampleUser);

        Optional<AdUserDto> result = adUserService.getBySamAccountName("jdoe");

        assertTrue(result.isPresent());
        assertTrue(result.get().getMemberOf().isEmpty());
    }

    @Test
    void toDto_shouldDefaultToEnabledWhenUacNull() {
        sampleUser.setUserAccountControl(null);
        when(ldapRepository.findBySamAccountName("jdoe")).thenReturn(sampleUser);

        Optional<AdUserDto> result = adUserService.getBySamAccountName("jdoe");

        assertTrue(result.isPresent());
        assertTrue(result.get().isEnabled());
    }
}
