package com.inno.orderservice.service;

import com.inno.orderservice.dto.*;
import com.inno.orderservice.entity.Item;
import com.inno.orderservice.entity.Order;
import com.inno.orderservice.entity.OrderItem;
import com.inno.orderservice.entity.OrderStatus;
import com.inno.orderservice.exception.ItemNotFoundException;
import com.inno.orderservice.exception.OrderNotFoundException;
import com.inno.orderservice.mapper.OrderMapper;
import com.inno.orderservice.repository.ItemRepository;
import com.inno.orderservice.repository.OrderRepository;
import com.inno.orderservice.specification.OrderSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ItemRepository itemRepository;
    private final OrderMapper orderMapper;
    private final UserClientService userClientService;

    public OrderResponse createOrder(OrderRequest request) {
        UserInfoResponse user = userClientService.getUserByEmail(request.getUserEmail());

        Order order = orderMapper.toEntity(request);
        order.setUserId(user.getId());
        order.setStatus(OrderStatus.NEW);

        fillItems(order, request.getItems());

        Order saved = orderRepository.save(order);
        return toResponseWithUser(saved, user);
    }

    public OrderResponse getOrderById(Long id, String email) {
        UserInfoResponse user = userClientService.getUserByEmail(email);
        Order order = getOwnedOrder(id, user);
        return toResponseWithUser(order, user);
    }

    public Page<OrderResponse> getAllOrders(LocalDateTime createdFrom,
                                            LocalDateTime createdTo,
                                            List<OrderStatus> statuses,
                                            Pageable pageable) {

        return orderRepository
                .findAll(OrderSpecification.filter(createdFrom, createdTo, statuses), pageable)
                .map(orderMapper::toResponse);
    }

    public List<OrderResponse> getOrdersByEmail(String email) {
        UserInfoResponse user = userClientService.getUserByEmail(email);

        return orderRepository.findAllByUserId(user.getId()).stream()
                .map(order -> toResponseWithUser(order, user))
                .toList();
    }

    @Transactional
    public OrderResponse updateOrder(Long id, String email, UpdateOrderRequest request) {
        UserInfoResponse user = userClientService.getUserByEmail(email);
        Order order = getOwnedOrder(id, user);

        orderMapper.updateEntity(request, order);

        if (request.getItems() != null) {
            order.getOrderItems().clear();
            fillItems(order, request.getItems());
        }

        Order saved = orderRepository.saveAndFlush(order);
        return toResponseWithUser(saved, user);
    }

    @Transactional
    public void deleteOrder(Long id, String email) {
        UserInfoResponse user = userClientService.getUserByEmail(email);
        getOwnedOrder(id, user);
        orderRepository.softDeleteById(id, LocalDateTime.now());
    }

    private OrderResponse toResponseWithUser(Order order, UserInfoResponse user) {
        OrderResponse response = orderMapper.toResponse(order);
        response.setUser(user);
        return response;
    }

    private void fillItems(Order order, List<OrderItemRequest> requests) {
        BigDecimal totalPrice = BigDecimal.ZERO;

        for (OrderItemRequest orderItemRequest : requests) {
            Item item = itemRepository.findById(orderItemRequest.getItemId())
                    .orElseThrow(() -> new ItemNotFoundException("Item not found: " + orderItemRequest.getItemId()));

            OrderItem orderItem = orderMapper.toItemEntity(orderItemRequest);
            orderItem.setItem(item);
            order.addItem(orderItem);

            totalPrice = totalPrice.add(item.getPrice().multiply(BigDecimal.valueOf(orderItemRequest.getQuantity())));
        }

        order.setTotalPrice(totalPrice);
    }

    private Order getOwnedOrder(Long id, UserInfoResponse user) {
        Order order = orderRepository.findWithItemsById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + id));

        if (!user.getId().equals(order.getUserId())) {
            throw new OrderNotFoundException("Order not found: " + id);
        }

        return order;
    }
}