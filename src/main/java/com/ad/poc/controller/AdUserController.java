package com.ad.poc.controller;

import com.ad.poc.dto.AdUserDto;
import com.ad.poc.dto.LoginRequest;
import com.ad.poc.dto.LoginResponse;
import com.ad.poc.service.AdUserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ad-users")
public class AdUserController {

    private final AdUserService adUserService;

    public AdUserController(AdUserService adUserService) {
        this.adUserService = adUserService;
    }

    /**
     * LOGIN - Authenticate a user against Active Directory using username and password.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return adUserService.authenticate(request.username(), request.password())
                .map(user -> ResponseEntity.ok(
                        new LoginResponse(true, "Login successful", user)))
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new LoginResponse(false, "Invalid credentials", null)));
    }

    /**
     * INSERT - Create a new user in Active Directory.
     */
    @PostMapping
    public ResponseEntity<AdUserDto> create(@Valid @RequestBody AdUserDto dto) {
        AdUserDto created = adUserService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * LIST - Get all users from Active Directory.
     * Optional filter by department.
     */
    @GetMapping
    public ResponseEntity<List<AdUserDto>> listAll(
            @RequestParam(required = false) String department) {
        List<AdUserDto> users;
        if (department != null && !department.isBlank()) {
            users = adUserService.getByDepartment(department);
        } else {
            users = adUserService.listAll();
        }
        return ResponseEntity.ok(users);
    }

    /**
     * SEARCH - Search users by keyword across multiple AD fields.
     */
    @GetMapping("/search")
    public ResponseEntity<List<AdUserDto>> search(@RequestParam String q) {
        List<AdUserDto> results = adUserService.search(q);
        return ResponseEntity.ok(results);
    }

    /**
     * READ - Get a single user by sAMAccountName.
     */
    @GetMapping("/{samAccountName}")
    public ResponseEntity<AdUserDto> getBySamAccountName(@PathVariable String samAccountName) {
        return adUserService.getBySamAccountName(samAccountName)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * UPDATE - Update an existing user's attributes in Active Directory.
     */
    @PutMapping("/{samAccountName}")
    public ResponseEntity<AdUserDto> update(@PathVariable String samAccountName,
                                            @Valid @RequestBody AdUserDto dto) {
        AdUserDto updated = adUserService.update(samAccountName, dto);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE - Remove a user from Active Directory.
     */
    @DeleteMapping("/{samAccountName}")
    public ResponseEntity<Void> delete(@PathVariable String samAccountName) {
        adUserService.delete(samAccountName);
        return ResponseEntity.noContent().build();
    }
}
