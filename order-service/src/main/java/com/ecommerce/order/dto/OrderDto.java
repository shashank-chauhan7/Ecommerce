package com.ecommerce.order.dto;

import com.ecommerce.common.dto.OrderItemDto;
import com.ecommerce.order.model.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderDto(
        UUID id,
        UUID userId,
        OrderStatus status,
        List<OrderItemDto> items,
        BigDecimal totalAmount,
        String shippingAddress,
        String paymentId,
        Instant createdAt,
        Instant updatedAt
) {}
