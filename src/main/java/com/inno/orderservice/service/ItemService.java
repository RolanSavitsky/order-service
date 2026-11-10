package com.inno.orderservice.service;

import com.inno.orderservice.dto.ItemRequest;
import com.inno.orderservice.dto.ItemResponse;
import com.inno.orderservice.entity.Item;
import com.inno.orderservice.exception.ItemNotFoundException;
import com.inno.orderservice.mapper.ItemMapper;
import com.inno.orderservice.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;

    public ItemResponse createItem(ItemRequest request) {
        Item item = itemMapper.toEntity(request);
        return itemMapper.toResponse(itemRepository.save(item));
    }

    public ItemResponse getItemById(Long id) {
        return itemMapper.toResponse(findItem(id));
    }

    public List<ItemResponse> getAllItems() {
        return itemRepository.findAll().stream()
                .map(itemMapper::toResponse)
                .toList();
    }

    @Transactional
    public ItemResponse updateItem(Long id, ItemRequest request) {
        Item item = findItem(id);
        itemMapper.updateEntity(request, item);
        return itemMapper.toResponse(item);
    }

    @Transactional
    public void deleteItem(Long id) {
        itemRepository.delete(findItem(id));
    }

    private Item findItem(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Item not found: " + id));
    }
}