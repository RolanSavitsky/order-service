package com.inno.orderservice.integration;

import com.inno.orderservice.dto.ItemRequest;
import com.inno.orderservice.entity.Item;
import com.inno.orderservice.repository.ItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


public class ItemIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ItemRepository itemRepository;

    @Test
    void createItem_asAdmin_returnsCreatedAndSavesToDb() throws Exception {
        mockMvc.perform(post("/items")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request("Telephone", "500.5"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Telephone"))
                .andExpect(jsonPath("$.price").value(500.5));

        assertThat(itemRepository.count()).isEqualTo(1);
    }

    @Test
    void createItem_asUser_returnsForbidden() throws Exception {
        mockMvc.perform(post("/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request("Telephone", "500.5"))))
                .andExpect(status().isForbidden());

        assertThat(itemRepository.count()).isEqualTo(0);
    }

    @Test
    void createItem_withInvalidBody_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/items")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request("", "-5"))))
                .andExpect(status().isBadRequest());

        assertThat(itemRepository.count()).isEqualTo(0);
    }

    @Test
    void getById_whenItemExists_returnsItem() throws Exception {
        Item saved = saveItem("Telephone", "500.5");

        mockMvc.perform(get("/items/" + saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.name").value("Telephone"))
                .andExpect(jsonPath("$.price").value(500.5));
    }

    @Test
    void getById_whenItemNotFound_returnsNotFound() throws Exception {
        mockMvc.perform(get("/items/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Item not found: 999"));
    }

    @Test
    void getAllItems_returnsAllItems() throws Exception {
        saveItem("Telephone", "500.5");
        saveItem("Laptop", "1500.5");

        mockMvc.perform(get("/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void updateItem_asAdmin_updatesItemInDb() throws Exception {
        Item saved = saveItem("Telephone", "500.5");

        mockMvc.perform(put("/items/" + saved.getId())
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request("Mobile telephone", "600"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Mobile telephone"))
                .andExpect(jsonPath("$.price").value(600));

        Item fromDb = itemRepository.findById(saved.getId()).orElseThrow();
        assertThat(fromDb.getName()).isEqualTo("Mobile telephone");
        assertThat(fromDb.getPrice()).isEqualByComparingTo("600");
    }

    @Test
    void deleteItem_asAdmin_removesItem() throws Exception {
        Item saved = saveItem("Telephone", "500.5");

        mockMvc.perform(delete("/items/" + saved.getId())
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isNoContent());

        assertThat(itemRepository.existsById(saved.getId())).isFalse();
    }

    private ItemRequest request(String name, String price) {
        ItemRequest request = new ItemRequest();
        request.setName(name);
        request.setPrice(price == null ? null : new BigDecimal(price));
        return request;
    }

    private Item saveItem(String name, String price) {
        Item item = new Item();
        item.setName(name);
        item.setPrice(new BigDecimal(price));
        return itemRepository.save(item);
    }
}