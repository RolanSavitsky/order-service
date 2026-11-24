package com.inno.orderservice.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class ItemRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotNull
    @Positive
    @Digits(integer = 10, fraction = 2)
    private BigDecimal price;
}
