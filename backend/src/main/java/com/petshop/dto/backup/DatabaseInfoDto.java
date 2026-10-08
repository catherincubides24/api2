package com.petshop.dto.backup;

import java.time.LocalDateTime;
import java.util.Map;

public record DatabaseInfoDto(
        String databaseName,
        String databaseVersion,
        Map<String, Long> tableCounts,
        Long totalRecords,
        LocalDateTime serverTime
) {}
