package com.petshop.dto.report;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailySale(
        LocalDate date,
        long orders,
        BigDecimal amount
) {
}