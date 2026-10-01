package com.petshop.service.report.export;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.petshop.dto.report.ProductStat;
import com.petshop.dto.report.ReportFormat;
import com.petshop.dto.report.ReportSummary;
import com.petshop.dto.report.StatBreakdown;
import com.petshop.service.report.ReportExporter;
import com.petshop.service.report.ReportFormatter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PdfReportExporter implements ReportExporter {

    private static final Font TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
    private static final Font SECTION = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
    private static final Font BODY = FontFactory.getFont(FontFactory.HELVETICA, 9);
    private static final Font HEAD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
    private static final Color HEAD_BG = new Color(0x1d, 0x28, 0x38);

    @Override
    public ReportFormat format() {
        return ReportFormat.PDF;
    }

    @Override
    public byte[] export(ReportSummary summary) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(doc, out);
            doc.open();

            doc.add(new Paragraph("Huellitas Shop - Reporte de ventas", TITLE));
            doc.add(new Paragraph(ReportFormatter.describe(summary.filter()), BODY));
            doc.add(new Paragraph("Generado: " + summary.generatedAt().toLocalDate() + "\n\n", BODY));

            addSection(doc, "Indicadores clave", new String[]{"Indicador", "Valor"}, List.of(
                    new String[]{"Pedidos totales", String.valueOf(summary.totalOrders())},
                    new String[]{"Pedidos vendidos (pagados/enviados)", String.valueOf(summary.soldOrders())},
                    new String[]{"Ingresos", ReportFormatter.money(summary.totalRevenue())},
                    new String[]{"Ticket promedio", ReportFormatter.money(summary.averageTicket())},
                    new String[]{"Unidades vendidas", String.valueOf(summary.unitsSold())}));

            addSection(doc, "Pedidos por estado (% por cantidad)",
                    breakdownHeaders("Estado"), breakdownRows(summary.byStatus()));
            addSection(doc, "Ventas por método de pago (% de ingresos)",
                    breakdownHeaders("Método"), breakdownRows(summary.byPaymentMethod()));
            addSection(doc, "Ventas por categoría (% de ingresos)",
                    breakdownHeaders("Categoría"), breakdownRows(summary.byCategory()));
            addSection(doc, "Top productos",
                    new String[]{"Producto", "Unidades", "Monto", "%"},
                    productRows(summary.topProducts()));
            addSection(doc, "Ventas por día",
                    new String[]{"Fecha", "Pedidos", "Monto"},
                    summary.dailySales().stream().map(day -> new String[]{
                            day.date().toString(),
                            String.valueOf(day.orders()),
                            ReportFormatter.money(day.amount())}).toList());

            doc.newPage();
            addSection(doc, "Detalle de pedidos",
                    new String[]{"Ticket", "Fecha", "Estado", "Pago", "Ítems", "Total"},
                    summary.orders().stream().map(order -> new String[]{
                            order.ticket() == null ? "-" : order.ticket(),
                            order.date().toLocalDate().toString(),
                            order.status(),
                            order.paymentMethod(),
                            String.valueOf(order.items()),
                            ReportFormatter.money(order.total())}).toList());

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el PDF", e);
        }
    }

    private String[] breakdownHeaders(String label) {
        return new String[]{label, "Cantidad", "Monto", "%"};
    }

    private List<String[]> breakdownRows(List<StatBreakdown> items) {
        return items.stream().map(item -> new String[]{
                item.label(),
                String.valueOf(item.count()),
                ReportFormatter.money(item.amount()),
                ReportFormatter.percent(item.percentage())}).toList();
    }

    private List<String[]> productRows(List<ProductStat> items) {
        return items.stream().map(item -> new String[]{
                item.name(),
                String.valueOf(item.units()),
                ReportFormatter.money(item.amount()),
                ReportFormatter.percent(item.percentage())}).toList();
    }

    private void addSection(Document doc, String title, String[] headers, List<String[]> rows) {
        doc.add(new Paragraph(title, SECTION));
        PdfPTable table = new PdfPTable(headers.length);
        table.setWidthPercentage(100);
        table.setSpacingBefore(4);
        table.setSpacingAfter(12);
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, HEAD));
            cell.setBackgroundColor(HEAD_BG);
            table.addCell(cell);
        }
        for (String[] row : rows) {
            for (String value : row) {
                table.addCell(new Phrase(value, BODY));
            }
        }
        doc.add(table);
    }
}