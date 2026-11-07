package com.inno.orderservice.dto;

import com.inno.orderservice.entity.OrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class UpdateOrderRequest {

    @NotNull
    private OrderStatus status;

    @Valid
    @Size(min = 1)
    private List<OrderItemRequest> items;
}
