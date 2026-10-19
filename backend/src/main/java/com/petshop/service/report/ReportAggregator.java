package com.petshop.service.report;

import com.petshop.dto.report.DailySale;
import com.petshop.dto.report.OrderRow;
import com.petshop.dto.report.ProductStat;
import com.petshop.dto.report.ReportFilter;
import com.petshop.dto.report.ReportSummary;
import com.petshop.dto.report.StatBreakdown;
import com.petshop.entity.OrderItem;
import com.petshop.entity.OrderStatus;
import com.petshop.entity.PetOrder;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

@Component
public class ReportAggregator {

    private static final Set<OrderStatus> REVENUE_STATUSES =
            EnumSet.of(OrderStatus.PAID, OrderStatus.SHIPPED);
    private static final int TOP_PRODUCTS = 10;

    public ReportSummary aggregate(List<PetOrder> orders, ReportFilter filter) {
        List<PetOrder> sold = orders.stream()
                .filter(order -> REVENUE_STATUSES.contains(order.getStatus()))
                .toList();
        List<OrderItem> soldItems = sold.stream()
                .flatMap(order -> order.getItems().stream())
                .toList();

        BigDecimal revenue = sum(sold.stream().map(PetOrder::getTotalAmount));
        BigDecimal average = sold.isEmpty()
                ? BigDecimal.ZERO
                : revenue.divide(BigDecimal.valueOf(sold.size()), 2, RoundingMode.HALF_UP);

        return new ReportSummary(
                LocalDateTime.now(),
                filter,
                orders.size(),
                sold.size(),
                revenue,
                average,
                soldItems.stream().mapToLong(OrderItem::getQuantity).sum(),
                breakdown(orders, order -> ReportFormatter.status(order.getStatus()),
                        PetOrder::getTotalAmount, false),
                breakdown(sold, order -> ReportFormatter.payment(order.getPaymentMethod()),
                        PetOrder::getTotalAmount, true),
                breakdown(soldItems, this::categoryOf, this::subtotal, true),
                topProducts(soldItems),
                dailySales(sold),
                rows(orders));
    }

    private <T> List<StatBreakdown> breakdown(Collection<T> source, Function<T, String> key,
                                              Function<T, BigDecimal> amount,
                                              boolean percentByAmount) {
        BigDecimal totalAmount = sum(source.stream().map(amount));
        Map<String, List<T>> groups = source.stream()
                .collect(Collectors.groupingBy(key, TreeMap::new, Collectors.toList()));

        return groups.entrySet().stream().map(entry -> {
            BigDecimal groupAmount = sum(entry.getValue().stream().map(amount));
            long count = entry.getValue().size();
            double pct = percentByAmount
                    ? percent(groupAmount, totalAmount)
                    : percent(BigDecimal.valueOf(count), BigDecimal.valueOf(source.size()));
            return new StatBreakdown(entry.getKey(), count, groupAmount, pct);
        }).toList();
    }

    private List<ProductStat> topProducts(List<OrderItem> items) {
        BigDecimal total = sum(items.stream().map(this::subtotal));
        return items.stream()
                .collect(Collectors.groupingBy(item -> item.getProduct().getName()))
                .entrySet().stream().map(entry -> {
                    BigDecimal amount = sum(entry.getValue().stream().map(this::subtotal));
                    long units = entry.getValue().stream().mapToLong(OrderItem::getQuantity).sum();
                    return new ProductStat(entry.getKey(), units, amount, percent(amount, total));
                })
                .sorted(Comparator.comparing(ProductStat::amount).reversed())
                .limit(TOP_PRODUCTS)
                .toList();
    }

    private List<DailySale> dailySales(List<PetOrder> sold) {
        return sold.stream()
                .collect(Collectors.groupingBy(
                        order -> order.getCreatedAt().toLocalDate(), TreeMap::new, Collectors.toList()))
                .entrySet().stream()
                .map(entry -> new DailySale(
                        entry.getKey(),
                        entry.getValue().size(),
                        sum(entry.getValue().stream().map(PetOrder::getTotalAmount))))
                .toList();
    }

    private List<OrderRow> rows(List<PetOrder> orders) {
        return orders.stream().map(order -> new OrderRow(
                order.getTicketNumber(),
                order.getCreatedAt(),
                ReportFormatter.status(order.getStatus()),
                ReportFormatter.payment(order.getPaymentMethod()),
                order.getTotalAmount(),
                order.getItems().size())).toList();
    }

    private String categoryOf(OrderItem item) {
        String category = item.getProduct().getCategory();
        return (category == null || category.isBlank()) ? "General" : category;
    }

    private BigDecimal subtotal(OrderItem item) {
        return item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
    }

    private BigDecimal sum(Stream<BigDecimal> values) {
        return values.reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private double percent(BigDecimal part, BigDecimal total) {
        if (total.signum() == 0) {
            return 0;
        }
        return part.multiply(BigDecimal.valueOf(100))
                .divide(total, 2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}