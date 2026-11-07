package com.inno.orderservice.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class OrderItemResponse {

    private Long itemId;
    private String itemName;
    private BigDecimal itemPrice;
    private Long quantity;
}
