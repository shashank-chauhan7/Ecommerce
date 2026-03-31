package com.ecommerce.order.service;

import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.dto.OrderDto;

import java.util.List;
import java.util.UUID;

public interface OrderService {

    OrderDto createOrder(CreateOrderRequest request, UUID userId);

    OrderDto getOrder(UUID orderId);

    List<OrderDto> getOrdersByUser(UUID userId);

    OrderDto cancelOrder(UUID orderId, UUID userId);

    String getOrderStatus(UUID orderId);
}
