package com.petshop.service.report;

import com.petshop.dto.report.ReportFilter;
import com.petshop.entity.OrderStatus;
import com.petshop.entity.PaymentMethod;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ReportFormatter {

    private static final Locale CO = Locale.forLanguageTag("es-CO");

    private ReportFormatter() {
    }

    public static String status(OrderStatus status) {
        return switch (status) {
            case PENDING -> "Pendiente";
            case PAID -> "Pagado";
            case SHIPPED -> "Enviado";
            case CANCELLED -> "Cancelado";
        };
    }

    public static String payment(PaymentMethod method) {
        if (method == null) {
            return "Sin confirmar";
        }
        return switch (method) {
            case CASH -> "Efectivo";
            case TRANSFER -> "Transferencia";
            case PAYPAL -> "PayPal";
        };
    }

    public static String money(BigDecimal value) {
        NumberFormat format = NumberFormat.getCurrencyInstance(CO);
        format.setMaximumFractionDigits(0);
        return format.format(value);
    }

    public static String percent(double value) {
        return String.format(CO, "%.2f%%", value);
    }

    public static String describe(ReportFilter filter) {
        List<String> parts = new ArrayList<>();
        if (filter.from() != null) parts.add("Desde " + filter.from());
        if (filter.to() != null) parts.add("Hasta " + filter.to());
        if (filter.status() != null) parts.add("Estado: " + status(filter.status()));
        if (filter.paymentMethod() != null) parts.add("Pago: " + payment(filter.paymentMethod()));
        if (filter.category() != null && !filter.category().isBlank()) {
            parts.add("Categoría: " + filter.category());
        }
        return parts.isEmpty()
                ? "Filtros: toda la información"
                : "Filtros: " + String.join(" | ", parts);
    }
}