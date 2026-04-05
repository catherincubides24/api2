package com.petshop.dto.order;

import com.petshop.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record OrderStatusUpdateRequest(
        @NotNull(message = "El estado es obligatorio")
        OrderStatus status
) {
}
