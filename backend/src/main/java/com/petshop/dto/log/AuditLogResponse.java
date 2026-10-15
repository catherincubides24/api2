package com.petshop.dto.log;

import com.petshop.entity.AuditAction;
import java.time.LocalDateTime;

public record AuditLogResponse(
        Long id,
        AuditAction action,
        String entityName,
        String entityId,
        String actor,
        String details,
        LocalDateTime createdAt
) {
}