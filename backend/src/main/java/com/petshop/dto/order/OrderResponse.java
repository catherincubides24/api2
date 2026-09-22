package com.petshop.dto.order;

import com.petshop.entity.OrderStatus;
import com.petshop.entity.PaymentMethod;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        Long userId,
        String userName,
        OrderStatus status,
        PaymentMethod paymentMethod,
        String ticketNumber,
        BigDecimal totalAmount,
        LocalDateTime createdAt,
        List<OrderItemResponse> items
) {
}
