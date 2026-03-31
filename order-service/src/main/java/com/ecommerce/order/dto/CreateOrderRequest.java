package com.ecommerce.order.dto;

import com.ecommerce.common.dto.OrderItemDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateOrderRequest(
        @NotEmpty(message = "Order must contain at least one item")
        @Valid
        List<OrderItemDto> items,

        @NotBlank(message = "Shipping address is required")
        String shippingAddress
) {}
