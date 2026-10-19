package com.petshop.service.report;

import com.petshop.dto.report.ExportedReport;
import com.petshop.dto.report.ReportFilter;
import com.petshop.dto.report.ReportFormat;
import com.petshop.dto.report.ReportSummary;
import com.petshop.entity.PetOrder;
import com.petshop.exception.BadRequestException;
import com.petshop.repository.PetOrderRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReportService {

    private final PetOrderRepository orderRepository;
    private final ReportAggregator aggregator;
    private final Map<ReportFormat, ReportExporter> exporters;

    public ReportService(PetOrderRepository orderRepository,
                         ReportAggregator aggregator,
                         List<ReportExporter> exporters) {
        this.orderRepository = orderRepository;
        this.aggregator = aggregator;
        this.exporters = exporters.stream()
                .collect(Collectors.toMap(ReportExporter::format, Function.identity()));
    }

    public ReportSummary buildSummary(ReportFilter filter) {
        if (filter.from() != null && filter.to() != null && filter.from().isAfter(filter.to())) {
            throw new BadRequestException("La fecha inicial no puede ser posterior a la final");
        }
        List<PetOrder> orders = orderRepository.findAll(
                OrderReportSpecifications.from(filter),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return aggregator.aggregate(orders, filter);
    }

    public ExportedReport export(ReportFormat format, ReportFilter filter) {
        ReportExporter exporter = exporters.get(format);
        if (exporter == null) {
            throw new BadRequestException("Formato de reporte no soportado: " + format);
        }
        byte[] content = exporter.export(buildSummary(filter));
        String fileName = "reporte-huellitas-" + LocalDate.now() + "." + format.extension();
        return new ExportedReport(fileName, format.mediaType(), content);
    }
}