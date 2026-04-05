package com.petshop.dto.auth;

import com.petshop.entity.Role;

public record AuthResponse(
        String token,
        Long id,
        String fullName,
        String email,
        Role role
) {
}
