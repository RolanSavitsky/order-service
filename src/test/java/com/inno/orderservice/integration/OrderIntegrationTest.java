package com.inno.orderservice.integration;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.inno.orderservice.dto.OrderItemRequest;
import com.inno.orderservice.dto.OrderRequest;
import com.inno.orderservice.dto.UpdateOrderRequest;
import com.inno.orderservice.entity.Item;
import com.inno.orderservice.entity.Order;
import com.inno.orderservice.entity.OrderItem;
import com.inno.orderservice.entity.OrderStatus;
import com.inno.orderservice.repository.ItemRepository;
import com.inno.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class OrderIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createOrder_whenValidRequest_savesOrderAndReturnsWithUser() throws Exception {
        stubUserFound("steve@example.com", 10L);
        Item item1 = saveItem("Telephone", "500.50");
        Item item2 = saveItem("Laptop", "1500.50");

        OrderRequest request = orderRequest("steve@example.com",
                List.of(orderItemRequest(item1.getId(), 2L),
                        orderItemRequest(item2.getId(), 1L)));

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.userId").value(10))
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.totalPrice").value(2501.5))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.user.id").value(10))
                .andExpect(jsonPath("$.user.email").value("steve@example.com"));

        assertThat(orderRepository.count()).isEqualTo(1);

        Order fromDb = orderRepository.findAll().getFirst();
        assertThat(fromDb.getUserId()).isEqualTo(10L);
        assertThat(fromDb.getStatus()).isEqualTo(OrderStatus.NEW);
        assertThat(fromDb.getTotalPrice()).isEqualByComparingTo("2501.50");
    }

    @Test
    void createOrder_whenUserNotFound_returnsNotFoundAndDoesNotSave() throws Exception {
        WIREMOCK_SERVER.stubFor(WireMock.get(urlPathEqualTo("/users/by-email"))
                .withQueryParam("email", equalTo("steve@example.com"))
                .willReturn(aResponse().withStatus(404)));

        Item item = saveItem("Telephone", "500.5");

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                orderRequest("steve@example.com", List.of(orderItemRequest(item.getId(), 1L))))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"));

        assertThat(orderRepository.count()).isEqualTo(0);
    }

    @Test
    void createOrder_whenUserServiceFails_returnsServiceUnavailableAndDoesNotSave() throws Exception {
        WIREMOCK_SERVER.stubFor(WireMock.get(urlPathEqualTo("/users/by-email"))
                .willReturn(aResponse().withStatus(500)));

        Item item = saveItem("Telephone", "500.5");

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                orderRequest("steve@example.com", List.of(orderItemRequest(item.getId(), 1L))))))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("User service is unavailable"));

        assertThat(orderRepository.count()).isEqualTo(0);
    }

    @Test
    void getOrderById_whenOwnOrder_returnsOrderWithUser() throws Exception {
        stubUserFound("steve@example.com", 10L);
        Item item = saveItem("Telephone", "500.5");
        Order order = saveOrder(10L, item, 2L);

        mockMvc.perform(get("/orders/" + order.getId())
                        .param("email", "steve@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(order.getId()))
                .andExpect(jsonPath("$.userId").value(10))
                .andExpect(jsonPath("$.totalPrice").value(1001.0))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].itemName").value("Telephone"))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.user.email").value("steve@example.com"));
    }

    @Test
    void getOrderById_whenOrderBelongsToAnotherUser_returnsNotFound() throws Exception {
        stubUserFound("steve@example.com", 10L);
        Item item = saveItem("Telephone", "500.5");
        Order order = saveOrder(20L, item, 1L);

        mockMvc.perform(get("/orders/" + order.getId())
                        .param("email", "steve@example.com"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Order not found: " + order.getId()));
    }

    @Test
    void getOrderById_whenOrderNotFound_returnsNotFound() throws Exception {
        stubUserFound("steve@example.com", 10L);

        mockMvc.perform(get("/orders/999")
                        .param("email", "steve@example.com"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Order not found: 999"));
    }

    @Test
    void getOrdersByEmail_whenOrderExists_returnsOnlyUserOrdersWithUser() throws Exception {
        stubUserFound("steve@example.com", 10L);
        Item item = saveItem("Telephone", "500.5");

        saveOrder(10L, item, 1L);
        saveOrder(10L, item, 2L);
        saveOrder(20L, item, 1L);

        mockMvc.perform(get("/orders/by-email")
                        .param("email", "steve@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].userId").value(10))
                .andExpect(jsonPath("$[1].userId").value(10))
                .andExpect(jsonPath("$[0].user.email").value("steve@example.com"))
                .andExpect(jsonPath("$[0].items.length()").value(1));
    }

    @Test
    void getOrdersByEmail_whenNoOrders_returnsEmptyList() throws Exception {
        stubUserFound("steve@example.com", 10L);

        mockMvc.perform(get("/orders/by-email")
                        .param("email", "steve@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getAllOrders_asUser_returnsForbidden() throws Exception {
        mockMvc.perform(get("/orders"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllOrders_asAdmin_returnsPage() throws Exception {
        Item item = saveItem("Telephone", "500.5");
        saveOrder(10L, item, 1L);
        saveOrder(10L, item, 2L);

        mockMvc.perform(get("/orders")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void getAllOrders_withStatusFilter_returnsOnlyMatchingOrders() throws Exception {
        Item item = saveItem("Telephone", "500.5");
        saveOrder(10L, item, 1L);
        Order processing = saveOrder(20L, item, 1L);
        processing.setStatus(OrderStatus.PROCESSING);
        orderRepository.save(processing);

        mockMvc.perform(get("/orders")
                        .with(user("admin").roles("ADMIN"))
                        .param("statuses", "PROCESSING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].status").value("PROCESSING"));
    }

    @Test
    void getAllOrders_withPagination_returnsOnlyMatchingOrders() throws Exception {
        Item item = saveItem("Telephone", "500.5");
        saveOrder(10L, item, 1L);
        saveOrder(20L, item, 2L);
        saveOrder(30L, item, 3L);

        mockMvc.perform(get("/orders")
                .with(user("admin").roles("ADMIN"))
                .param("page","0")
                .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void updateOrder_whenItemsNotProvided_updatesStatusAndKeepsItems() throws Exception {
        stubUserFound("steve@example.com", 10L);
        Item item = saveItem("Telephone", "500.5");
        Order order = saveOrder(10L, item, 2L);

        mockMvc.perform(put("/orders/" + order.getId())
                        .param("email", "steve@example.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                updateOrderRequest(OrderStatus.PROCESSING, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESSING"))
                .andExpect(jsonPath("$.totalPrice").value(1001.0))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.user.id").value(10));

        Order fromDb = orderRepository.findWithItemsById(order.getId()).orElseThrow();
        assertThat(fromDb.getStatus()).isEqualTo(OrderStatus.PROCESSING);
        assertThat(fromDb.getOrderItems()).hasSize(1);
        assertThat(fromDb.getTotalPrice()).isEqualByComparingTo("1001.00");
    }

    @Test
    void updateOrder_whenItemsProvided_replacesItemsAndRecalculatesTotal() throws Exception {
        stubUserFound("steve@example.com", 10L);
        Item oldItem = saveItem("Telephone", "500.5");
        Item newItem = saveItem("Laptop", "1500.5");
        Order order = saveOrder(10L, oldItem, 1L);

        mockMvc.perform(put("/orders/" + order.getId())
                        .param("email", "steve@example.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                updateOrderRequest(OrderStatus.PROCESSING, List.of(orderItemRequest(newItem.getId(), 3L))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPrice").value(4501.5))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].itemName").value("Laptop"))
                .andExpect(jsonPath("$.items[0].quantity").value(3));

        Order fromDb = orderRepository.findWithItemsById(order.getId()).orElseThrow();
        assertThat(fromDb.getOrderItems()).hasSize(1);
        assertThat(fromDb.getOrderItems().getFirst().getItem().getName()).isEqualTo("Laptop");
        assertThat(fromDb.getOrderItems().getFirst().getQuantity()).isEqualTo(3L);
        assertThat(fromDb.getTotalPrice()).isEqualByComparingTo("4501.50");
    }

    @Test
    void deleteOrder_whenOwnOrder_softDeleteOrder() throws Exception {
        stubUserFound("steve@example.com", 10L);
        Item item = saveItem("Telephone", "500.5");
        Order order = saveOrder(10L, item, 1L);

        mockMvc.perform(delete("/orders/" + order.getId())
                        .param("email", "steve@example.com"))
                .andExpect(status().isNoContent());

        assertThat(orderRepository.findById(order.getId())).isEmpty();

        Boolean deleted = jdbcTemplate.queryForObject(
                "SELECT deleted FROM orders WHERE id = ?", Boolean.class, order.getId());
        assertThat(deleted).isTrue();
    }

    private void stubUserFound(String email, Long id) {
        WIREMOCK_SERVER.stubFor(WireMock.get(urlPathEqualTo("/users/by-email"))
                .withQueryParam("email", equalTo(email))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"id": %d, "name": "Steve", "surname": "Jobs",
                                "email": "%s", "active": true}
                                """.formatted(id, email))));
    }

    private Item saveItem(String name, String price) {
        Item item = new Item();
        item.setName(name);
        item.setPrice(new BigDecimal(price));
        return itemRepository.save(item);
    }

    private Order saveOrder(Long userId, Item item, Long quantity) {
        Order order = new Order();
        order.setUserId(userId);
        order.setStatus(OrderStatus.NEW);
        order.setTotalPrice(item.getPrice().multiply(BigDecimal.valueOf(quantity)));

        OrderItem orderItem = new OrderItem();
        orderItem.setItem(item);
        orderItem.setQuantity(quantity);
        order.addItem(orderItem);

        return orderRepository.save(order);
    }

    private OrderItemRequest orderItemRequest(Long itemId, Long quantity) {
        OrderItemRequest request = new OrderItemRequest();
        request.setItemId(itemId);
        request.setQuantity(quantity);
        return request;
    }

    private OrderRequest orderRequest(String email, List<OrderItemRequest> items) {
        OrderRequest request = new OrderRequest();
        request.setUserEmail(email);
        request.setItems(items);
        return request;
    }

    private UpdateOrderRequest updateOrderRequest(OrderStatus status, List<OrderItemRequest> items) {
        UpdateOrderRequest request = new UpdateOrderRequest();
        request.setStatus(status);
        request.setItems(items);
        return request;
    }
}