package com.petshop.service.report.export;

import com.petshop.dto.report.ReportFormat;
import com.petshop.dto.report.ReportSummary;
import com.petshop.dto.report.StatBreakdown;
import com.petshop.service.report.ReportExporter;
import com.petshop.service.report.ReportFormatter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

@Component
public class ExcelReportExporter implements ReportExporter {

    @Override
    public ReportFormat format() {
        return ReportFormat.XLSX;
    }

    @Override
    public byte[] export(ReportSummary summary) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Styles styles = new Styles(workbook);

            addTable(workbook, styles, "Resumen", new String[]{"Indicador", "Valor"},
                    Set.of(), Set.of(1), List.of(
                            List.<Object>of("Filtros", ReportFormatter.describe(summary.filter())),
                            List.<Object>of("Pedidos totales", summary.totalOrders()),
                            List.<Object>of("Pedidos vendidos", summary.soldOrders()),
                            List.<Object>of("Ingresos", summary.totalRevenue()),
                            List.<Object>of("Ticket promedio", summary.averageTicket()),
                            List.<Object>of("Unidades vendidas", summary.unitsSold())));

            addBreakdown(workbook, styles, "Por estado", "Estado", summary.byStatus());
            addBreakdown(workbook, styles, "Por método de pago", "Método", summary.byPaymentMethod());
            addBreakdown(workbook, styles, "Por categoría", "Categoría", summary.byCategory());

            addTable(workbook, styles, "Top productos",
                    new String[]{"Producto", "Unidades", "Monto", "%"}, Set.of(3), Set.of(2),
                    summary.topProducts().stream().map(product -> List.<Object>of(
                            product.name(), product.units(), product.amount(), product.percentage())).toList());

            addTable(workbook, styles, "Ventas por día",
                    new String[]{"Fecha", "Pedidos", "Monto"}, Set.of(), Set.of(2),
                    summary.dailySales().stream().map(day -> List.<Object>of(
                            day.date().toString(), day.orders(), day.amount())).toList());

            addTable(workbook, styles, "Detalle",
                    new String[]{"Ticket", "Fecha", "Estado", "Pago", "Ítems", "Total"}, Set.of(), Set.of(5),
                    summary.orders().stream().map(order -> List.<Object>of(
                            order.ticket() == null ? "-" : order.ticket(),
                            order.date().toLocalDate().toString(),
                            order.status(),
                            order.paymentMethod(),
                            order.items(),
                            order.total())).toList());

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo generar el Excel", e);
        }
    }

    private void addBreakdown(Workbook workbook, Styles styles, String sheetName,
                              String label, List<StatBreakdown> items) {
        addTable(workbook, styles, sheetName,
                new String[]{label, "Cantidad", "Monto", "%"}, Set.of(3), Set.of(2),
                items.stream().map(item -> List.<Object>of(
                        item.label(), item.count(), item.amount(), item.percentage())).toList());
    }

    /** percentCols: valores 0-100 que se muestran como %. moneyCols: valores monetarios. */
    private void addTable(Workbook workbook, Styles styles, String sheetName, String[] headers,
                          Set<Integer> percentCols, Set<Integer> moneyCols, List<List<Object>> rows) {
        Sheet sheet = workbook.createSheet(sheetName);

        Row headerRow = sheet.createRow(0);
        for (int col = 0; col < headers.length; col++) {
            Cell cell = headerRow.createCell(col);
            cell.setCellValue(headers[col]);
            cell.setCellStyle(styles.header);
        }

        for (int r = 0; r < rows.size(); r++) {
            Row row = sheet.createRow(r + 1);
            List<Object> values = rows.get(r);
            for (int col = 0; col < values.size(); col++) {
                writeCell(row.createCell(col), values.get(col),
                        percentCols.contains(col), moneyCols.contains(col), styles);
            }
        }

        for (int col = 0; col < headers.length; col++) {
            sheet.autoSizeColumn(col);
        }
    }

    private void writeCell(Cell cell, Object value, boolean percent, boolean money, Styles styles) {
        if (value instanceof BigDecimal decimal) {
            cell.setCellValue(decimal.doubleValue());
            if (money) {
                cell.setCellStyle(styles.money);
            }
        } else if (value instanceof Double number) {
            cell.setCellValue(percent ? number / 100 : number);
            if (percent) {
                cell.setCellStyle(styles.percent);
            }
        } else if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
        } else {
            cell.setCellValue(String.valueOf(value));
        }
    }

    private static final class Styles {
        final CellStyle header;
        final CellStyle money;
        final CellStyle percent;

        Styles(Workbook workbook) {
            DataFormat format = workbook.createDataFormat();

            Font bold = workbook.createFont();
            bold.setBold(true);
            header = workbook.createCellStyle();
            header.setFont(bold);

            money = workbook.createCellStyle();
            money.setDataFormat(format.getFormat("$ #,##0"));

            percent = workbook.createCellStyle();
            percent.setDataFormat(format.getFormat("0.00%"));
        }
    }
}