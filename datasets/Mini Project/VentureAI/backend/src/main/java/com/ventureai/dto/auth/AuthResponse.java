package com.ventureai.dto.auth;

public record AuthResponse(
        String token,
        Long id,
        String fullName,
        String email,
        String userType
) {
}
