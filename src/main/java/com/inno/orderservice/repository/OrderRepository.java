package com.inno.orderservice.repository;

import com.inno.orderservice.entity.Order;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    @EntityGraph(attributePaths = {"orderItems", "orderItems.item"})
    List<Order> findAllByUserId(Long userId);

    @EntityGraph(attributePaths = {"orderItems", "orderItems.item"})
    Optional<Order> findWithItemsById(Long id);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Order o SET o.deleted = true, o.updatedAt = :now WHERE o.id = :id AND o.deleted = false")
    int softDeleteById(@Param("id") Long id, @Param("now") LocalDateTime now);
}