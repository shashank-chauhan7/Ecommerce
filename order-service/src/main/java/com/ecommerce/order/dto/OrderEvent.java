package com.ecommerce.order.dto;

import com.ecommerce.common.dto.OrderItemDto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderEvent(
        UUID orderId,
        UUID userId,
        List<OrderItemDto> items,
        BigDecimal totalAmount,
        String status
) {}
