package com.petshop.dto.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record PaymentCaptureRequest(
        @NotBlank(message = "El ID de orden PayPal es obligatorio")
        @Pattern(regexp = "^[A-Za-z0-9]{10,40}$", message = "El ID de orden PayPal no es válido")
        String paypalOrderId,

        @NotNull(message = "El ID de pedido interno es obligatorio")
        @Positive(message = "El ID de pedido no es válido")
        Long orderId
) {
}