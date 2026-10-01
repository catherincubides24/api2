package com.petshop.dto.report;

import java.math.BigDecimal;

public record ProductStat(
        String name,
        long units,
        BigDecimal amount,
        double percentage
) {
}