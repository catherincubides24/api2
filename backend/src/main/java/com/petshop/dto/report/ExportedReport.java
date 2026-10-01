package com.petshop.dto.report;

public record ExportedReport(
        String fileName,
        String mediaType,
        byte[] content
) {
}