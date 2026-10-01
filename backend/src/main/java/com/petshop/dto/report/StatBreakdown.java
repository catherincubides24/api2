package com.petshop.dto.report;

import java.math.BigDecimal;

public record StatBreakdown(
        String label,
        long count,
        BigDecimal amount,
        double percentage
) {
}