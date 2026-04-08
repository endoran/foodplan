package com.endoran.foodplan.dto;

public record AuthResponse(
        String accessToken,
        long expiresIn,
        String email,
        String orgId,
        String role
) {}
