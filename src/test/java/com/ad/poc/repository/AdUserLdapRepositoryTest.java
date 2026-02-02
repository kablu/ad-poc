package com.ad.poc.repository;

import com.ad.poc.model.AdUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.query.LdapQuery;

import javax.naming.Name;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdUserLdapRepositoryTest {

    @Mock
    private LdapTemplate ldapTemplate;

    private AdUserLdapRepository repository;

    private AdUser sampleUser;

    @BeforeEach
    void setUp() {
        repository = new AdUserLdapRepository(ldapTemplate);

        sampleUser = new AdUser();
        sampleUser.setSamAccountName("jdoe");
        sampleUser.setCommonName("John Doe");
        sampleUser.setDisplayName("John Doe");
        sampleUser.setFirstName("John");
        sampleUser.setLastName("Doe");
        sampleUser.setEmail("jdoe@company.com");
        sampleUser.setDepartment("Engineering");
    }

    @Test
    void findAll_shouldSearchWithUserObjectClass() {
        when(ldapTemplate.search(any(LdapQuery.class), any(AttributesMapper.class)))
                .thenReturn(List.of(sampleUser));

        List<AdUser> result = repository.findAll();

        assertEquals(1, result.size());
        assertEquals("jdoe", result.get(0).getSamAccountName());
        verify(ldapTemplate).search(any(LdapQuery.class), any(AttributesMapper.class));
    }

    @Test
    void findAll_shouldReturnEmptyWhenNoUsers() {
        when(ldapTemplate.search(any(LdapQuery.class), any(AttributesMapper.class)))
                .thenReturn(Collections.emptyList());

        List<AdUser> result = repository.findAll();

        assertTrue(result.isEmpty());
    }

    @Test
    void findBySamAccountName_shouldReturnUserWhenFound() {
        when(ldapTemplate.search(any(LdapQuery.class), any(AttributesMapper.class)))
                .thenReturn(List.of(sampleUser));

        AdUser result = repository.findBySamAccountName("jdoe");

        assertNotNull(result);
        assertEquals("jdoe", result.getSamAccountName());
    }

    @Test
    void findBySamAccountName_shouldReturnNullWhenNotFound() {
        when(ldapTemplate.search(any(LdapQuery.class), any(AttributesMapper.class)))
                .thenReturn(Collections.emptyList());

        AdUser result = repository.findBySamAccountName("nonexistent");

        assertNull(result);
    }

    @Test
    void findByDepartment_shouldReturnFilteredUsers() {
        when(ldapTemplate.search(any(LdapQuery.class), any(AttributesMapper.class)))
                .thenReturn(List.of(sampleUser));

        List<AdUser> result = repository.findByDepartment("Engineering");

        assertEquals(1, result.size());
        assertEquals("Engineering", result.get(0).getDepartment());
    }

    @Test
    void search_shouldSearchAcrossMultipleFields() {
        when(ldapTemplate.search(anyString(), anyString(), any(AttributesMapper.class)))
                .thenReturn(List.of(sampleUser));

        List<AdUser> result = repository.search("john");

        assertEquals(1, result.size());
        verify(ldapTemplate).search(eq("OU=Users"), anyString(), any(AttributesMapper.class));
    }

    @Test
    void findByEmail_shouldReturnUserWhenFound() {
        when(ldapTemplate.search(any(LdapQuery.class), any(AttributesMapper.class)))
                .thenReturn(List.of(sampleUser));

        AdUser result = repository.findByEmail("jdoe@company.com");

        assertNotNull(result);
        assertEquals("jdoe@company.com", result.getEmail());
    }

    @Test
    void findByEmail_shouldReturnNullWhenNotFound() {
        when(ldapTemplate.search(any(LdapQuery.class), any(AttributesMapper.class)))
                .thenReturn(Collections.emptyList());

        AdUser result = repository.findByEmail("unknown@company.com");

        assertNull(result);
    }

    @Test
    void create_shouldBindContextWithUserAttributes() {
        doNothing().when(ldapTemplate).bind(any(DirContextAdapter.class));

        repository.create(sampleUser);

        verify(ldapTemplate).bind(any(DirContextAdapter.class));
    }

    @Test
    void update_shouldLookupAndModifyAttributes() {
        // findBySamAccountName returns the existing user with a DN
        when(ldapTemplate.search(any(LdapQuery.class), any(AttributesMapper.class)))
                .thenReturn(List.of(sampleUser));

        DirContextOperations mockContext = mock(DirContextOperations.class);
        when(ldapTemplate.lookupContext(any(Name.class))).thenReturn(mockContext);
        doNothing().when(ldapTemplate).modifyAttributes(any(DirContextOperations.class));

        AdUser updatedUser = new AdUser();
        updatedUser.setDisplayName("John Updated");
        updatedUser.setFirstName("John");
        updatedUser.setLastName("Doe");
        updatedUser.setEmail("jdoe-new@company.com");

        repository.update("jdoe", updatedUser);

        verify(ldapTemplate).lookupContext(any(Name.class));
        verify(mockContext).setAttributeValue("displayName", "John Updated");
        verify(mockContext).setAttributeValue("mail", "jdoe-new@company.com");
        verify(ldapTemplate).modifyAttributes(mockContext);
    }

    @Test
    void update_shouldThrowWhenUserNotFound() {
        when(ldapTemplate.search(any(LdapQuery.class), any(AttributesMapper.class)))
                .thenReturn(Collections.emptyList());

        AdUser updatedUser = new AdUser();
        updatedUser.setDisplayName("Updated");

        assertThrows(IllegalArgumentException.class,
                () -> repository.update("nonexistent", updatedUser));
    }

    @Test
    void delete_shouldUnbindUserDn() {
        when(ldapTemplate.search(any(LdapQuery.class), any(AttributesMapper.class)))
                .thenReturn(List.of(sampleUser));

        doNothing().when(ldapTemplate).unbind(any(Name.class));

        repository.delete("jdoe");

        verify(ldapTemplate).unbind(any(Name.class));
    }

    @Test
    void delete_shouldThrowWhenUserNotFound() {
        when(ldapTemplate.search(any(LdapQuery.class), any(AttributesMapper.class)))
                .thenReturn(Collections.emptyList());

        assertThrows(IllegalArgumentException.class,
                () -> repository.delete("nonexistent"));
    }
}
