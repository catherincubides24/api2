package com.petshop.dto.report;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderRow(
        String ticket,
        LocalDateTime date,
        String status,
        String paymentMethod,
        BigDecimal total,
        int items
) {
}