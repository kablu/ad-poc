package com.ad.poc.controller;

import com.ad.poc.dto.AdUserDto;
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

    @PostMapping
    public ResponseEntity<AdUserDto> create(@Valid @RequestBody AdUserDto dto) {
        AdUserDto created = adUserService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdUserDto> getById(@PathVariable Long id) {
        return adUserService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<AdUserDto>> getAll(
            @RequestParam(required = false) String department) {
        List<AdUserDto> users;
        if (department != null && !department.isBlank()) {
            users = adUserService.getByDepartment(department);
        } else {
            users = adUserService.getAll();
        }
        return ResponseEntity.ok(users);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdUserDto> update(@PathVariable Long id,
                                            @Valid @RequestBody AdUserDto dto) {
        AdUserDto updated = adUserService.update(id, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        adUserService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
