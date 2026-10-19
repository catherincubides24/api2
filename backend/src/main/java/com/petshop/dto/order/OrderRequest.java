package com.petshop.dto.order;

import com.petshop.entity.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record OrderRequest(
        @NotNull(message = "El usuario es obligatorio")
        @Positive(message = "El usuario no es válido")
        Long userId,

        PaymentMethod paymentMethod,

        @NotEmpty(message = "El pedido debe tener al menos un producto")
        @Size(max = 50, message = "Máximo 50 productos distintos por pedido")
        List<@Valid OrderItemRequest> items
) {
}