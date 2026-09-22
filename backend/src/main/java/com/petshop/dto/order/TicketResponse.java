package com.petshop.dto.order;

import com.petshop.entity.OrderStatus;
import com.petshop.entity.PaymentMethod;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record TicketResponse(
        String ticketNumber,
        Long orderId,
        LocalDateTime issuedAt,
        LocalDateTime orderDate,
        String customerName,
        String customerEmail,
        OrderStatus status,
        PaymentMethod paymentMethod,
        List<OrderItemResponse> items,
        BigDecimal totalAmount
) {
}
