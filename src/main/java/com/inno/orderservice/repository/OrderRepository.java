package com.inno.orderservice.repository;

import com.inno.orderservice.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    List<Order> findAllByUserId(Long userId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Order order SET order.deleted = true, order.updatedAt = :now WHERE order.id = :id AND order.deleted = false")
    int softDeleteById(@Param("id") Long id, @Param("now") LocalDateTime now);
}