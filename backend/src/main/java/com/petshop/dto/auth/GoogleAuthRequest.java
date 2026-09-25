package com.petshop.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record GoogleAuthRequest(
        @NotBlank(message = "El token de Google es obligatorio")
        String credential
) {
}