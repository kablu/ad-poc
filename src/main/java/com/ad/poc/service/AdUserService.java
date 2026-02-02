package com.ad.poc.service;

import com.ad.poc.dto.AdUserDto;
import com.ad.poc.entity.AdUser;
import com.ad.poc.repository.AdUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class AdUserService {

    private final AdUserRepository adUserRepository;

    public AdUserService(AdUserRepository adUserRepository) {
        this.adUserRepository = adUserRepository;
    }

    public AdUserDto create(AdUserDto dto) {
        if (adUserRepository.existsBySamAccountName(dto.getSamAccountName())) {
            throw new IllegalArgumentException(
                    "User with SAM account name '" + dto.getSamAccountName() + "' already exists");
        }
        AdUser entity = toEntity(dto);
        AdUser saved = adUserRepository.save(entity);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public Optional<AdUserDto> getById(Long id) {
        return adUserRepository.findById(id).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<AdUserDto> getBySamAccountName(String samAccountName) {
        return adUserRepository.findBySamAccountName(samAccountName).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<AdUserDto> getAll() {
        return adUserRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AdUserDto> getByDepartment(String department) {
        return adUserRepository.findByDepartment(department).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public AdUserDto update(Long id, AdUserDto dto) {
        AdUser existing = adUserRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));

        existing.setSamAccountName(dto.getSamAccountName());
        existing.setDisplayName(dto.getDisplayName());
        existing.setEmail(dto.getEmail());
        existing.setDistinguishedName(dto.getDistinguishedName());
        existing.setDepartment(dto.getDepartment());
        existing.setTitle(dto.getTitle());
        existing.setEnabled(dto.isEnabled());

        AdUser updated = adUserRepository.save(existing);
        return toDto(updated);
    }

    public void delete(Long id) {
        if (!adUserRepository.existsById(id)) {
            throw new IllegalArgumentException("User not found with id: " + id);
        }
        adUserRepository.deleteById(id);
    }

    private AdUser toEntity(AdUserDto dto) {
        AdUser entity = new AdUser();
        entity.setSamAccountName(dto.getSamAccountName());
        entity.setDisplayName(dto.getDisplayName());
        entity.setEmail(dto.getEmail());
        entity.setDistinguishedName(dto.getDistinguishedName());
        entity.setDepartment(dto.getDepartment());
        entity.setTitle(dto.getTitle());
        entity.setEnabled(dto.isEnabled());
        return entity;
    }

    private AdUserDto toDto(AdUser entity) {
        AdUserDto dto = new AdUserDto();
        dto.setId(entity.getId());
        dto.setSamAccountName(entity.getSamAccountName());
        dto.setDisplayName(entity.getDisplayName());
        dto.setEmail(entity.getEmail());
        dto.setDistinguishedName(entity.getDistinguishedName());
        dto.setDepartment(entity.getDepartment());
        dto.setTitle(entity.getTitle());
        dto.setEnabled(entity.isEnabled());
        return dto;
    }
}
