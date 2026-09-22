package com.petshop.dto.order;

import com.petshop.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;

public record PaymentUpdateRequest(
        @NotNull(message = "El metodo de pago es obligatorio")
        PaymentMethod paymentMethod,

        boolean markAsPaid
) {
}
