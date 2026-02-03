package com.ad.poc.dto;

public record LoginResponse(
        boolean success,
        String message,
        AdUserDto user
) {}
