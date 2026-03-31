package com.ecommerce.order.facade;

import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.dto.OrderDto;
import com.ecommerce.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Facade pattern: provides a simplified, unified interface to the order subsystem.
 * Coordinates validation, order creation, event publishing, and response mapping.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderFacade {

    private final OrderService orderService;

    public OrderDto placeOrder(CreateOrderRequest request, UUID userId) {
        log.info("Placing order for user {}", userId);
        validateRequest(request);
        OrderDto order = orderService.createOrder(request, userId);
        log.info("Order {} placed successfully for user {}", order.id(), userId);
        return order;
    }

    public OrderDto getOrderStatus(UUID orderId, UUID userId) {
        log.debug("Fetching order {} for user {}", orderId, userId);
        return orderService.getOrder(orderId);
    }

    public OrderDto cancelOrder(UUID orderId, UUID userId) {
        log.info("Cancelling order {} for user {}", orderId, userId);
        OrderDto cancelled = orderService.cancelOrder(orderId, userId);
        log.info("Order {} cancelled by user {}", orderId, userId);
        return cancelled;
    }

    public List<OrderDto> getUserOrders(UUID userId) {
        log.debug("Fetching all orders for user {}", userId);
        return orderService.getOrdersByUser(userId);
    }

    private void validateRequest(CreateOrderRequest request) {
        if (request.items() == null || request.items().isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item");
        }
        if (request.shippingAddress() == null || request.shippingAddress().isBlank()) {
            throw new IllegalArgumentException("Shipping address is required");
        }
    }
}
