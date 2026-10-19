package com.petshop.service.report;

import com.petshop.dto.report.ReportFormat;
import com.petshop.dto.report.ReportSummary;

public interface ReportExporter {

    ReportFormat format();

    byte[] export(ReportSummary summary);
}