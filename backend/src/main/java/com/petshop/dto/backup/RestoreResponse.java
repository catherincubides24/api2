package com.petshop.dto.backup;

import java.time.LocalDateTime;

public record RestoreResponse(
        boolean success,
        String message,
        String fileName,
        int statementsExecuted,
        LocalDateTime restoredAt
) {}
