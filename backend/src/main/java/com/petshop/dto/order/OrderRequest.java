package com.petshop.dto.order;

import com.petshop.entity.OrderStatus;
import com.petshop.entity.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record OrderRequest(
        @NotNull(message = "El usuario es obligatorio")
        Long userId,

        OrderStatus status,

        PaymentMethod paymentMethod,

        @NotEmpty(message = "El pedido debe tener al menos un producto")
        List<@Valid OrderItemRequest> items
) {
}
