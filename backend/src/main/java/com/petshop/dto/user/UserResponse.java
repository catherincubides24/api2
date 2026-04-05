package com.petshop.dto.user;

import com.petshop.entity.Role;
import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String fullName,
        String email,
        Role role,
        LocalDateTime createdAt
) {
}
