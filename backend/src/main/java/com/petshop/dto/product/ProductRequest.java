package com.petshop.dto.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres")
        String name,

        @Size(max = 2000, message = "La descripción no puede superar 2000 caracteres")
        String description,

        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.01", message = "El precio debe ser mayor que 0")
        @Digits(integer = 8, fraction = 2, message = "El precio admite hasta 8 enteros y 2 decimales")
        BigDecimal price,

        @NotNull(message = "El stock es obligatorio")
        @Min(value = 0, message = "El stock no puede ser negativo")
        @Max(value = 1_000_000, message = "El stock es demasiado alto")
        Integer stock,

        @Size(max = 255, message = "La URL de la imagen no puede superar 255 caracteres")
        @Pattern(regexp = "^$|^https?://.+", message = "La imagen debe ser una URL http(s)")
        String imageUrl,

        @Size(max = 100, message = "La categoría no puede superar 100 caracteres")
        String category,

        Boolean active
) {
}