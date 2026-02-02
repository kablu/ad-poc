package com.ad.poc.service;

import com.ad.poc.dto.AdUserDto;
import com.ad.poc.entity.AdUser;
import com.ad.poc.repository.AdUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdUserServiceTest {

    @Mock
    private AdUserRepository adUserRepository;

    @InjectMocks
    private AdUserService adUserService;

    private AdUserDto sampleDto;
    private AdUser sampleEntity;

    @BeforeEach
    void setUp() {
        sampleDto = new AdUserDto("jdoe", "John Doe", "jdoe@company.com");
        sampleDto.setDepartment("Engineering");
        sampleDto.setTitle("Developer");
        sampleDto.setDistinguishedName("CN=John Doe,OU=Users,DC=company,DC=com");

        sampleEntity = new AdUser("jdoe", "John Doe", "jdoe@company.com");
        sampleEntity.setId(1L);
        sampleEntity.setDepartment("Engineering");
        sampleEntity.setTitle("Developer");
        sampleEntity.setDistinguishedName("CN=John Doe,OU=Users,DC=company,DC=com");
    }

    @Test
    void create_shouldSaveAndReturnDto() {
        when(adUserRepository.existsBySamAccountName("jdoe")).thenReturn(false);
        when(adUserRepository.save(any(AdUser.class))).thenReturn(sampleEntity);

        AdUserDto result = adUserService.create(sampleDto);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("jdoe", result.getSamAccountName());
        assertEquals("John Doe", result.getDisplayName());
        assertEquals("jdoe@company.com", result.getEmail());
        assertEquals("Engineering", result.getDepartment());
        verify(adUserRepository).save(any(AdUser.class));
    }

    @Test
    void create_shouldThrowWhenDuplicateSamAccountName() {
        when(adUserRepository.existsBySamAccountName("jdoe")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> adUserService.create(sampleDto));
        assertTrue(ex.getMessage().contains("already exists"));
        verify(adUserRepository, never()).save(any());
    }

    @Test
    void getById_shouldReturnDtoWhenFound() {
        when(adUserRepository.findById(1L)).thenReturn(Optional.of(sampleEntity));

        Optional<AdUserDto> result = adUserService.getById(1L);

        assertTrue(result.isPresent());
        assertEquals("jdoe", result.get().getSamAccountName());
    }

    @Test
    void getById_shouldReturnEmptyWhenNotFound() {
        when(adUserRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<AdUserDto> result = adUserService.getById(99L);

        assertFalse(result.isPresent());
    }

    @Test
    void getBySamAccountName_shouldReturnDtoWhenFound() {
        when(adUserRepository.findBySamAccountName("jdoe")).thenReturn(Optional.of(sampleEntity));

        Optional<AdUserDto> result = adUserService.getBySamAccountName("jdoe");

        assertTrue(result.isPresent());
        assertEquals("John Doe", result.get().getDisplayName());
    }

    @Test
    void getAll_shouldReturnAllUsers() {
        AdUser second = new AdUser("asmith", "Alice Smith", "asmith@company.com");
        second.setId(2L);
        when(adUserRepository.findAll()).thenReturn(Arrays.asList(sampleEntity, second));

        List<AdUserDto> result = adUserService.getAll();

        assertEquals(2, result.size());
        assertEquals("jdoe", result.get(0).getSamAccountName());
        assertEquals("asmith", result.get(1).getSamAccountName());
    }

    @Test
    void getByDepartment_shouldReturnFilteredUsers() {
        when(adUserRepository.findByDepartment("Engineering"))
                .thenReturn(List.of(sampleEntity));

        List<AdUserDto> result = adUserService.getByDepartment("Engineering");

        assertEquals(1, result.size());
        assertEquals("Engineering", result.get(0).getDepartment());
    }

    @Test
    void update_shouldModifyAndReturnDto() {
        when(adUserRepository.findById(1L)).thenReturn(Optional.of(sampleEntity));
        when(adUserRepository.save(any(AdUser.class))).thenReturn(sampleEntity);

        AdUserDto updateDto = new AdUserDto("jdoe", "John Updated", "jdoe-new@company.com");
        updateDto.setDepartment("Management");
        updateDto.setTitle("Manager");

        AdUserDto result = adUserService.update(1L, updateDto);

        assertNotNull(result);
        verify(adUserRepository).save(any(AdUser.class));
    }

    @Test
    void update_shouldThrowWhenNotFound() {
        when(adUserRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> adUserService.update(99L, sampleDto));
    }

    @Test
    void delete_shouldRemoveUser() {
        when(adUserRepository.existsById(1L)).thenReturn(true);
        doNothing().when(adUserRepository).deleteById(1L);

        adUserService.delete(1L);

        verify(adUserRepository).deleteById(1L);
    }

    @Test
    void delete_shouldThrowWhenNotFound() {
        when(adUserRepository.existsById(99L)).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> adUserService.delete(99L));
        verify(adUserRepository, never()).deleteById(anyLong());
    }
}
