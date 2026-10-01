package com.petshop.service.report;

import com.petshop.dto.report.ReportFilter;
import com.petshop.entity.PetOrder;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class OrderReportSpecifications {

    private OrderReportSpecifications() {
    }

    public static Specification<PetOrder> from(ReportFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.from() != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("createdAt"), filter.from().atStartOfDay()));
            }
            if (filter.to() != null) {
                predicates.add(cb.lessThan(
                        root.get("createdAt"), filter.to().plusDays(1).atStartOfDay()));
            }
            if (filter.status() != null) {
                predicates.add(cb.equal(root.get("status"), filter.status()));
            }
            if (filter.paymentMethod() != null) {
                predicates.add(cb.equal(root.get("paymentMethod"), filter.paymentMethod()));
            }
            if (filter.category() != null && !filter.category().isBlank()) {
                query.distinct(true);
                predicates.add(cb.equal(
                        root.join("items").join("product").get("category"), filter.category()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}