package com.inno.orderservice.controller;

import com.inno.orderservice.dto.OrderRequest;
import com.inno.orderservice.dto.OrderResponse;
import com.inno.orderservice.dto.UpdateOrderRequest;
import com.inno.orderservice.entity.OrderStatus;
import com.inno.orderservice.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody OrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getById(@PathVariable Long id,
                                                 @RequestParam String email) {

        return ResponseEntity.ok(orderService.getOrderById(id, email));
    }

    @GetMapping
    public ResponseEntity<Page<OrderResponse>> getAll(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdTo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) List<OrderStatus> statuses,
            Pageable pageable) {

        return ResponseEntity.ok(orderService.getAllOrders(createdFrom, createdTo, statuses, pageable));
    }

    @GetMapping("/by-email")
    public ResponseEntity<List<OrderResponse>> getByEmail(@RequestParam String email) {
        return ResponseEntity.ok(orderService.getOrdersByEmail(email));
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrderResponse> update(@PathVariable Long id,
                                                @RequestParam String email,
                                                @Valid @RequestBody UpdateOrderRequest request) {

        return ResponseEntity.ok(orderService.updateOrder(id, email, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @RequestParam String email) {

        orderService.deleteOrder(id, email);
        return ResponseEntity.noContent().build();
    }
}
