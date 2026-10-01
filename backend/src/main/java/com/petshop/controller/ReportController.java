package com.petshop.controller;

import com.petshop.dto.report.ExportedReport;
import com.petshop.dto.report.ReportFilter;
import com.petshop.dto.report.ReportFormat;
import com.petshop.dto.report.ReportSummary;
import com.petshop.service.report.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/summary")
    public ResponseEntity<ReportSummary> summary(ReportFilter filter) {
        return ResponseEntity.ok(reportService.buildSummary(filter));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam ReportFormat format, ReportFilter filter) {
        ExportedReport report = reportService.export(format, filter);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(report.mediaType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(report.fileName()).build().toString())
                .body(report.content());
    }
}