package com.ecommerce.order.service;

import com.ecommerce.common.dto.OrderItemDto;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.dto.OrderDto;
import com.ecommerce.order.model.Order;
import com.ecommerce.order.model.OrderStatus;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.order.saga.SagaMediator;
import com.ecommerce.order.state.OrderContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final SagaMediator sagaMediator;

    @Override
    @Transactional
    public OrderDto createOrder(CreateOrderRequest request, UUID userId) {
        Order order = sagaMediator.startOrderSaga(request, userId);
        return toDto(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDto getOrder(UUID orderId) {
        Order order = findOrderOrThrow(orderId);
        return toDto(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDto> getOrdersByUser(UUID userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public OrderDto cancelOrder(UUID orderId, UUID userId) {
        Order order = findOrderOrThrow(orderId);
        if (!order.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Order does not belong to user");
        }

        if (order.getStatus() == OrderStatus.CONFIRMED ||
            order.getStatus() == OrderStatus.SHIPPED ||
            order.getStatus() == OrderStatus.DELIVERED) {
            throw new IllegalStateException("Cannot cancel order in " + order.getStatus() + " state");
        }

        sagaMediator.handleFailure(orderId, "Cancelled by user");
        return toDto(orderRepository.findById(orderId).orElseThrow());
    }

    @Override
    @Transactional(readOnly = true)
    public String getOrderStatus(UUID orderId) {
        Order order = findOrderOrThrow(orderId);
        return order.getStatus().name();
    }

    private Order findOrderOrThrow(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
    }

    private OrderDto toDto(Order order) {
        List<OrderItemDto> itemDtos = order.getItems().stream()
                .map(item -> new OrderItemDto(
                        item.getProductId().toString(),
                        item.getProductName(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getTotalPrice()
                ))
                .toList();

        return new OrderDto(
                order.getId(),
                order.getUserId(),
                order.getStatus(),
                itemDtos,
                order.getTotalAmount(),
                order.getShippingAddress(),
                order.getPaymentId(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
