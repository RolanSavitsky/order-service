package com.inno.orderservice.service;

import com.inno.orderservice.dto.*;
import com.inno.orderservice.entity.Item;
import com.inno.orderservice.entity.Order;
import com.inno.orderservice.entity.OrderItem;
import com.inno.orderservice.entity.OrderStatus;
import com.inno.orderservice.exception.OrderNotFoundException;
import com.inno.orderservice.exception.UserServiceUnavailableException;
import com.inno.orderservice.mapper.OrderMapper;
import com.inno.orderservice.repository.ItemRepository;
import com.inno.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private UserClientService userClientService;

    @InjectMocks
    private OrderService orderService;

    @Test
    void createOrder_whenValidRequest_savesOrderWithItemsAndReturnsResponseWithUser() {
        OrderItemRequest itemRequest1 = new OrderItemRequest();
        itemRequest1.setItemId(1L);
        itemRequest1.setQuantity(2L);

        OrderItemRequest itemRequest2 = new OrderItemRequest();
        itemRequest2.setItemId(2L);
        itemRequest2.setQuantity(1L);

        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setUserEmail("steve@example.com");
        orderRequest.setItems(List.of(itemRequest1, itemRequest2));

        UserInfoResponse user = new UserInfoResponse();
        user.setId(10L);
        user.setEmail("steve@example.com");

        Item item1 = new Item();
        item1.setId(1L);
        item1.setName("Telephone");
        item1.setPrice(new BigDecimal("500.5"));

        Item item2 = new Item();
        item2.setId(2L);
        item2.setName("Laptop");
        item2.setPrice(new BigDecimal("1500.5"));

        OrderItem orderItem1 = new OrderItem();
        orderItem1.setQuantity(2L);

        OrderItem orderItem2 = new OrderItem();
        orderItem2.setQuantity(1L);

        Order order = new Order();

        Order saved = new Order();
        saved.setId(100L);

        OrderResponse expectedResponse = new OrderResponse();
        expectedResponse.setId(100L);

        when(userClientService.getUserByEmail("steve@example.com")).thenReturn(user);
        when(orderMapper.toEntity(orderRequest)).thenReturn(order);
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item1));
        when(itemRepository.findById(2L)).thenReturn(Optional.of(item2));
        when(orderMapper.toItemEntity(itemRequest1)).thenReturn(orderItem1);
        when(orderMapper.toItemEntity(itemRequest2)).thenReturn(orderItem2);
        when(orderRepository.save(order)).thenReturn(saved);
        when(orderMapper.toResponse(saved)).thenReturn(expectedResponse);

        OrderResponse result = orderService.createOrder(orderRequest);

        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertSame(user, result.getUser());

        assertEquals(10L, order.getUserId());
        assertEquals(OrderStatus.NEW, order.getStatus());
        assertEquals(new BigDecimal("2501.5"), order.getTotalPrice());
        assertEquals(2, order.getOrderItems().size());
        assertSame(item1, orderItem1.getItem());
        assertSame(item2, orderItem2.getItem());
        assertSame(order, orderItem1.getOrder());
        assertSame(order, orderItem2.getOrder());

        verify(userClientService, times(1)).getUserByEmail("steve@example.com");
        verify(orderMapper, times(1)).toEntity(orderRequest);
        verify(itemRepository, times(1)).findById(1L);
        verify(itemRepository, times(1)).findById(2L);
        verify(orderMapper, times(1)).toItemEntity(itemRequest1);
        verify(orderMapper, times(1)).toItemEntity(itemRequest2);
        verify(orderRepository, times(1)).save(order);
        verify(orderMapper, times(1)).toResponse(saved);
    }

    @Test
    void createOrder_whenUserServiceUnavailable_throwsUserServiceUnavailableException() {
        OrderRequest request = new OrderRequest();
        request.setUserEmail("steve@example.com");
        request.setItems(List.of(new OrderItemRequest()));

        when(userClientService.getUserByEmail("steve@example.com"))
                .thenThrow(new UserServiceUnavailableException("User service is unavailable"));

        UserServiceUnavailableException exception = assertThrows(
                UserServiceUnavailableException.class,
                () -> orderService.createOrder(request));

        assertEquals("User service is unavailable", exception.getMessage());

        verify(userClientService, times(1)).getUserByEmail("steve@example.com");
        verifyNoInteractions(orderMapper, orderRepository, itemRepository);
    }

    @Test
    void getOrderById_whenOrderBelongsToUser_returnsResponseWithUser() {
        Long orderId = 100L;
        String email = "steve@example.com";

        UserInfoResponse user = new UserInfoResponse();
        user.setId(10L);
        user.setEmail(email);

        Order order = new Order();
        order.setId(orderId);
        order.setUserId(10L);

        OrderResponse expectedResponse = new OrderResponse();
        expectedResponse.setId(orderId);

        when(userClientService.getUserByEmail(email)).thenReturn(user);
        when(orderRepository.findWithItemsById(orderId)).thenReturn(Optional.of(order));
        when(orderMapper.toResponse(order)).thenReturn(expectedResponse);

        OrderResponse result = orderService.getOrderById(orderId, email);

        assertNotNull(result);
        assertEquals(orderId, result.getId());
        assertSame(user, result.getUser());

        verify(userClientService, times(1)).getUserByEmail(email);
        verify(orderRepository, times(1)).findWithItemsById(orderId);
        verify(orderMapper, times(1)).toResponse(order);
    }

    @Test
    void getOrderById_whenOrderBelongsToAnotherUser_throwsOrderNotFoundException() {
        Long orderId = 100L;
        String email = "steve@example.com";

        UserInfoResponse user = new UserInfoResponse();
        user.setId(10L);

        Order order = new Order();
        order.setId(orderId);
        order.setUserId(20L);

        when(userClientService.getUserByEmail(email)).thenReturn(user);
        when(orderRepository.findWithItemsById(orderId)).thenReturn(Optional.of(order));

        OrderNotFoundException exception = assertThrows(
                OrderNotFoundException.class,
                () -> orderService.getOrderById(orderId, email));

        assertEquals("Order not found: 100", exception.getMessage());

        verify(orderRepository, times(1)).findWithItemsById(orderId);
        verifyNoInteractions(orderMapper);
    }

    @Test
    void getAllOrders_whenOrdersExists_returnsPage() {
        LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 12, 31, 23, 59);
        List<OrderStatus> statuses = List.of(OrderStatus.NEW, OrderStatus.PROCESSING);
        Pageable pageable = PageRequest.of(0, 10);

        Order order1 = new Order();
        order1.setId(1L);

        Order order2 = new Order();
        order2.setId(2L);

        Page<Order> orderPage = new PageImpl<>(List.of(order1, order2), pageable, 2);

        OrderResponse response1 = new OrderResponse();
        response1.setId(1L);

        OrderResponse response2 = new OrderResponse();
        response2.setId(2L);

        when(orderRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(orderPage);
        when(orderMapper.toResponse(order1)).thenReturn(response1);
        when(orderMapper.toResponse(order2)).thenReturn(response2);

        Page<OrderResponse> result = orderService.getAllOrders(from, to, statuses, pageable);

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());
        assertSame(response1, result.getContent().get(0));
        assertSame(response2, result.getContent().get(1));

        verify(orderRepository, times(1)).findAll(any(Specification.class), eq(pageable));
        verify(orderMapper, times(1)).toResponse(order1);
        verify(orderMapper, times(1)).toResponse(order2);
    }

    @Test
    void getOrdersByEmail_whenNoOrders_returnsEmptyList() {
        String email = "steve@example.com";

        UserInfoResponse user = new UserInfoResponse();
        user.setId(10L);

        when(userClientService.getUserByEmail(email)).thenReturn(user);
        when(orderRepository.findAllByUserId(10L)).thenReturn(List.of());

        List<OrderResponse> result = orderService.getOrdersByEmail(email);

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(userClientService, times(1)).getUserByEmail(email);
        verify(orderRepository, times(1)).findAllByUserId(10L);
        verifyNoInteractions(orderMapper);
    }

    @Test
    void updateOrder_whenItemsProvided_replacesItemsAndRecalculatesTotal() {
        Long orderId = 100L;
        String email = "steve@example.com";

        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setItemId(2L);
        itemRequest.setQuantity(3L);

        UpdateOrderRequest request = new UpdateOrderRequest();
        request.setStatus(OrderStatus.PROCESSING);
        request.setItems(List.of(itemRequest));

        UserInfoResponse user = new UserInfoResponse();
        user.setId(10L);

        OrderItem oldOrderItem = new OrderItem();
        oldOrderItem.setQuantity(1L);

        Order order = new Order();
        order.setId(orderId);
        order.setUserId(10L);
        order.setTotalPrice(new BigDecimal("500.5"));
        order.addItem(oldOrderItem);

        Item item = new Item();
        item.setId(2L);
        item.setName("Laptop");
        item.setPrice(new BigDecimal("1500.5"));

        OrderItem newOrderItem = new OrderItem();
        newOrderItem.setQuantity(3L);

        Order saved = new Order();
        saved.setId(orderId);

        OrderResponse expectedResponse = new OrderResponse();
        expectedResponse.setId(orderId);

        when(userClientService.getUserByEmail(email)).thenReturn(user);
        when(orderRepository.findWithItemsById(orderId)).thenReturn(Optional.of(order));
        when(itemRepository.findById(2L)).thenReturn(Optional.of(item));
        when(orderMapper.toItemEntity(itemRequest)).thenReturn(newOrderItem);
        when(orderRepository.saveAndFlush(order)).thenReturn(saved);
        when(orderMapper.toResponse(saved)).thenReturn(expectedResponse);

        OrderResponse result = orderService.updateOrder(orderId, email, request);

        assertNotNull(result);
        assertSame(user, result.getUser());

        assertEquals(1, order.getOrderItems().size());
        assertSame(newOrderItem, order.getOrderItems().getFirst());
        assertFalse(order.getOrderItems().contains(oldOrderItem));
        assertSame(item, newOrderItem.getItem());
        assertSame(order, newOrderItem.getOrder());
        assertEquals(new BigDecimal("4501.5"), order.getTotalPrice());

        verify(userClientService, times(1)).getUserByEmail(email);
        verify(orderMapper, times(1)).updateEntity(request, order);
        verify(itemRepository, times(1)).findById(2L);
        verify(orderMapper, times(1)).toItemEntity(itemRequest);
        verify(orderRepository, times(1)).saveAndFlush(order);
        verify(orderMapper, times(1)).toResponse(saved);
    }

    @Test
    void deleteOrder_whenOrderBelongsToUser_softDeleteOrder() {
        Long orderId = 100L;
        String email = "steve@example.com";

        UserInfoResponse user = new UserInfoResponse();
        user.setId(10L);

        Order order = new Order();
        order.setId(orderId);
        order.setUserId(10L);

        when(userClientService.getUserByEmail(email)).thenReturn(user);
        when(orderRepository.findWithItemsById(orderId)).thenReturn(Optional.of(order));

        orderService.deleteOrder(orderId, email);

        verify(userClientService, times(1)).getUserByEmail(email);
        verify(orderRepository, times(1)).findWithItemsById(orderId);
        verify(orderRepository, times(1)).softDeleteById(eq(orderId), any(LocalDateTime.class));
        verifyNoInteractions(orderMapper, itemRepository);
    }
}