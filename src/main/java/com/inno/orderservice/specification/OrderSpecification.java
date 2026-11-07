package com.inno.orderservice.specification;

import com.inno.orderservice.entity.Order;
import com.inno.orderservice.entity.OrderStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;

public class OrderSpecification {

    public static Specification<Order> filter(LocalDateTime createdFrom,
                                              LocalDateTime createdTo,
                                              List<OrderStatus> statuses) {

        return (root, query, criteriaBuilder) -> {
            Predicate predicate = criteriaBuilder.conjunction();

            if (createdFrom != null) {
                predicate = criteriaBuilder.and(predicate,
                        criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), createdFrom));
            }
            if (createdTo != null) {
                predicate = criteriaBuilder.and(predicate,
                        criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), createdTo));
            }
            if (statuses != null && !statuses.isEmpty()) {
                predicate = criteriaBuilder.and(predicate,
                        root.get("status").in(statuses));
            }

            return predicate;
        };
    }
}