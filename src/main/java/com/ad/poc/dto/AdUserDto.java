package com.ad.poc.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AdUserDto(

        @NotBlank(message = "SAM account name is required")
        @Size(max = 20, message = "SAM account name must not exceed 20 characters")
        String samAccountName,

        @NotBlank(message = "First name is required")
        @Size(max = 64, message = "First name must not exceed 64 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 64, message = "Last name must not exceed 64 characters")
        String lastName,

        @Size(max = 256, message = "Display name must not exceed 256 characters")
        String displayName,

        @Email(message = "Email must be valid")
        String email,

        String department,
        String title,
        String phoneNumber,
        String company,
        String distinguishedName,
        String userPrincipalName,
        List<String> memberOf,
        boolean enabled
) {
}
