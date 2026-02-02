package com.ad.poc.controller;

import com.ad.poc.dto.AdUserDto;
import com.ad.poc.service.AdUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdUserController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdUserService adUserService;

    @Autowired
    private ObjectMapper objectMapper;

    private AdUserDto sampleDto;

    @BeforeEach
    void setUp() {
        sampleDto = new AdUserDto("jdoe", "John", "Doe", "jdoe@company.com");
        sampleDto.setDisplayName("John Doe");
        sampleDto.setDepartment("Engineering");
        sampleDto.setTitle("Developer");
        sampleDto.setPhoneNumber("+1-555-0100");
        sampleDto.setCompany("Company Inc");
        sampleDto.setDistinguishedName("CN=John Doe,OU=Users,DC=company,DC=com");
        sampleDto.setUserPrincipalName("jdoe@company.com");
        sampleDto.setMemberOf(List.of("CN=Developers,OU=Groups,DC=company,DC=com"));
        sampleDto.setEnabled(true);
    }

    // ---- INSERT ----

    @Test
    void create_shouldReturn201WithCreatedUser() throws Exception {
        when(adUserService.create(any(AdUserDto.class))).thenReturn(sampleDto);

        AdUserDto requestBody = new AdUserDto("jdoe", "John", "Doe", "jdoe@company.com");

        mockMvc.perform(post("/api/v1/ad-users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.samAccountName", is("jdoe")))
                .andExpect(jsonPath("$.displayName", is("John Doe")))
                .andExpect(jsonPath("$.email", is("jdoe@company.com")))
                .andExpect(jsonPath("$.department", is("Engineering")));
    }

    @Test
    void create_shouldReturn400WhenSamAccountNameBlank() throws Exception {
        AdUserDto invalid = new AdUserDto("", "John", "Doe", "jdoe@company.com");

        mockMvc.perform(post("/api/v1/ad-users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_shouldReturn400WhenFirstNameBlank() throws Exception {
        AdUserDto invalid = new AdUserDto("jdoe", "", "Doe", "jdoe@company.com");

        mockMvc.perform(post("/api/v1/ad-users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    // ---- LIST ----

    @Test
    void listAll_shouldReturnAllUsers() throws Exception {
        AdUserDto second = new AdUserDto("asmith", "Alice", "Smith", "asmith@company.com");
        second.setDisplayName("Alice Smith");
        when(adUserService.listAll()).thenReturn(Arrays.asList(sampleDto, second));

        mockMvc.perform(get("/api/v1/ad-users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].samAccountName", is("jdoe")))
                .andExpect(jsonPath("$[1].samAccountName", is("asmith")));
    }

    @Test
    void listAll_withDepartmentFilter_shouldReturnFilteredList() throws Exception {
        when(adUserService.getByDepartment("Engineering")).thenReturn(List.of(sampleDto));

        mockMvc.perform(get("/api/v1/ad-users").param("department", "Engineering"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].department", is("Engineering")));
    }

    @Test
    void listAll_shouldReturnEmptyListWhenNoUsers() throws Exception {
        when(adUserService.listAll()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/ad-users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // ---- SEARCH ----

    @Test
    void search_shouldReturnMatchingUsers() throws Exception {
        when(adUserService.search("john")).thenReturn(List.of(sampleDto));

        mockMvc.perform(get("/api/v1/ad-users/search").param("q", "john"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].samAccountName", is("jdoe")));
    }

    @Test
    void search_shouldReturnEmptyWhenNoMatch() throws Exception {
        when(adUserService.search("xyz")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/ad-users/search").param("q", "xyz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // ---- READ (GET BY SAM ACCOUNT NAME) ----

    @Test
    void getBySamAccountName_shouldReturn200WhenFound() throws Exception {
        when(adUserService.getBySamAccountName("jdoe")).thenReturn(Optional.of(sampleDto));

        mockMvc.perform(get("/api/v1/ad-users/jdoe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.samAccountName", is("jdoe")))
                .andExpect(jsonPath("$.displayName", is("John Doe")))
                .andExpect(jsonPath("$.email", is("jdoe@company.com")))
                .andExpect(jsonPath("$.enabled", is(true)))
                .andExpect(jsonPath("$.memberOf", hasSize(1)));
    }

    @Test
    void getBySamAccountName_shouldReturn404WhenNotFound() throws Exception {
        when(adUserService.getBySamAccountName("nonexistent")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/ad-users/nonexistent"))
                .andExpect(status().isNotFound());
    }

    // ---- UPDATE ----

    @Test
    void update_shouldReturn200WithUpdatedUser() throws Exception {
        AdUserDto updatedDto = new AdUserDto("jdoe", "John", "Doe", "jdoe-new@company.com");
        updatedDto.setDisplayName("John Updated");
        updatedDto.setDepartment("Management");
        when(adUserService.update(eq("jdoe"), any(AdUserDto.class))).thenReturn(updatedDto);

        mockMvc.perform(put("/api/v1/ad-users/jdoe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName", is("John Updated")))
                .andExpect(jsonPath("$.department", is("Management")))
                .andExpect(jsonPath("$.email", is("jdoe-new@company.com")));
    }

    // ---- DELETE ----

    @Test
    void delete_shouldReturn204() throws Exception {
        doNothing().when(adUserService).delete("jdoe");

        mockMvc.perform(delete("/api/v1/ad-users/jdoe"))
                .andExpect(status().isNoContent());

        verify(adUserService).delete("jdoe");
    }
}
