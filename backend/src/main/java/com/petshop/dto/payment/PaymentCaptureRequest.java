package com.petshop.dto.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PaymentCaptureRequest(
        @NotBlank(message = "El ID de orden PayPal es obligatorio")
        String paypalOrderId,

        @NotNull(message = "El ID de pedido interno es obligatorio")
        Long orderId
) {}