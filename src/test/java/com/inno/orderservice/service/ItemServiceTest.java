package com.inno.orderservice.service;

import com.inno.orderservice.dto.ItemRequest;
import com.inno.orderservice.dto.ItemResponse;
import com.inno.orderservice.entity.Item;
import com.inno.orderservice.exception.ItemNotFoundException;
import com.inno.orderservice.mapper.ItemMapper;
import com.inno.orderservice.repository.ItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private ItemMapper itemMapper;

    @InjectMocks
    private ItemService itemService;

    @Test
    void createItem_whenValidRequest_returnsCreatedResponse() {

        ItemRequest request = new ItemRequest();
        request.setName("Telephone");
        request.setPrice(new BigDecimal("500.5"));

        Item item = new Item();
        item.setName("Telephone");
        item.setPrice(new BigDecimal("500.5"));

        Item saved = new Item();
        saved.setId(1L);
        saved.setName("Telephone");
        saved.setPrice(new BigDecimal("500.5"));

        ItemResponse expectedResponse = new ItemResponse();
        expectedResponse.setId(1L);
        expectedResponse.setName("Telephone");
        expectedResponse.setPrice(new BigDecimal("500.5"));

        when(itemMapper.toEntity(request)).thenReturn(item);
        when(itemRepository.save(item)).thenReturn(saved);
        when(itemMapper.toResponse(saved)).thenReturn(expectedResponse);

        ItemResponse result = itemService.createItem(request);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Telephone", result.getName());
        assertEquals(new BigDecimal("500.5"), result.getPrice());

        verify(itemMapper, times(1)).toEntity(request);
        verify(itemRepository, times(1)).save(item);
        verify(itemMapper, times(1)).toResponse(saved);
    }

    @Test
    void getItemById_whenItemExists_returnResponse() {
        Long itemId = 1L;

        Item item = new Item();
        item.setId(itemId);
        item.setName("Telephone");
        item.setPrice(new BigDecimal("500.5"));

        ItemResponse expectedResponse = new ItemResponse();
        expectedResponse.setId(1L);
        expectedResponse.setName("Telephone");
        expectedResponse.setPrice(new BigDecimal("500.5"));

        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(itemMapper.toResponse(item)).thenReturn(expectedResponse);

        ItemResponse result = itemService.getItemById(itemId);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Telephone", result.getName());
        assertEquals(new BigDecimal("500.5"), result.getPrice());

        verify(itemRepository, times(1)).findById(itemId);
        verify(itemMapper, times(1)).toResponse(item);
    }

    @Test
    void getAllItems_whenItemsExist_returnsMappedResponses() {

        Item item1 = new Item();
        item1.setId(1L);
        item1.setName("Telephone");
        item1.setPrice(new BigDecimal("500.5"));

        Item item2 = new Item();
        item2.setId(2L);
        item2.setName("Laptop");
        item2.setPrice(new BigDecimal("1500.5"));

        ItemResponse response1 = new ItemResponse();
        response1.setId(1L);
        response1.setName("Telephone");
        response1.setPrice(new BigDecimal("500.5"));

        ItemResponse response2 = new ItemResponse();
        response2.setId(2L);
        response2.setName("Laptop");
        response2.setPrice(new BigDecimal("1500.5"));

        when(itemRepository.findAll()).thenReturn(List.of(item1, item2));
        when(itemMapper.toResponse(item1)).thenReturn(response1);
        when(itemMapper.toResponse(item2)).thenReturn(response2);

        List<ItemResponse> result = itemService.getAllItems();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertSame(response1, result.getFirst());
        assertSame(response2, result.get(1));

        verify(itemRepository, times(1)).findAll();
        verify(itemMapper, times(1)).toResponse(item1);
        verify(itemMapper, times(1)).toResponse(item2);
    }

    @Test
    void getAllItems_whenNoItems_returnsEmptyList() {
        when(itemRepository.findAll()).thenReturn(List.of());

        List<ItemResponse> result = itemService.getAllItems();

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(itemRepository, times(1)).findAll();
        verifyNoInteractions(itemMapper);
    }

    @Test
    void updateItem_whenItemExists_updatesEntityAndReturnResponse() {
        Long itemId = 1L;

        ItemRequest request = new ItemRequest();
        request.setName("Mobile telephone");
        request.setPrice(new BigDecimal("500.5"));

        Item item = new Item();
        item.setId(itemId);
        item.setName("Telephone");
        item.setPrice(new BigDecimal("400.4"));

        ItemResponse expectedResponse = new ItemResponse();
        expectedResponse.setId(1L);
        expectedResponse.setName("Mobile telephone");
        expectedResponse.setPrice(new BigDecimal("500.5"));

        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(itemMapper.toResponse(item)).thenReturn(expectedResponse);

        ItemResponse result = itemService.updateItem(itemId, request);

        assertNotNull(result);
        assertEquals(itemId, result.getId());
        assertEquals("Mobile telephone", result.getName());
        assertEquals(new BigDecimal("500.5"), result.getPrice());

        verify(itemRepository, times(1)).findById(itemId);
        verify(itemMapper, times(1)).updateEntity(request, item);
        verify(itemMapper, times(1)).toResponse(item);
    }

    @Test
    void deleteItem_whenItemNotFound_throwsItemNotFoundException() {
        Long itemId = 999L;

        when(itemRepository.findById(itemId)).thenReturn(Optional.empty());

        ItemNotFoundException exception = assertThrows(
                ItemNotFoundException.class,
                () -> itemService.deleteItem(itemId));

        assertEquals("Item not found: 999", exception.getMessage());

        verify(itemRepository, times(1)).findById(itemId);
        verify(itemRepository, never()).delete(any(Item.class));
    }
}