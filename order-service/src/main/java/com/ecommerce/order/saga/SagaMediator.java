package com.ecommerce.order.saga;

import com.ecommerce.common.dto.OrderItemDto;
import com.ecommerce.common.event.DomainEvent;
import com.ecommerce.common.event.EventTypes;
import com.ecommerce.common.event.KafkaTopics;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.dto.OrderEvent;
import com.ecommerce.order.model.Order;
import com.ecommerce.order.model.OrderItem;
import com.ecommerce.order.model.OrderStatus;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.order.saga.steps.CreateOrderStep;
import com.ecommerce.order.saga.steps.ProcessPaymentStep;
import com.ecommerce.order.saga.steps.ReserveInventoryStep;
import com.ecommerce.order.state.OrderContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Mediator pattern: coordinates saga steps and handles inter-step communication.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SagaMediator {

    private final CreateOrderStep createOrderStep;
    private final ReserveInventoryStep reserveInventoryStep;
    private final ProcessPaymentStep processPaymentStep;
    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public Order startOrderSaga(CreateOrderRequest request, UUID userId) {
        log.info("Starting order saga for user {}", userId);

        List<OrderItem> items = request.items().stream()
                .map(dto -> OrderItem.builder()
                        .productId(UUID.fromString(dto.productId()))
                        .productName(dto.productName())
                        .quantity(dto.quantity())
                        .unitPrice(dto.unitPrice())
                        .totalPrice(dto.totalPrice())
                        .build())
                .toList();

        BigDecimal totalAmount = items.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = Order.builder()
                .userId(userId)
                .shippingAddress(request.shippingAddress())
                .totalAmount(totalAmount)
                .status(OrderStatus.CREATED)
                .build();

        items.forEach(order::addItem);

        Order createdOrder = createOrderStep.process(order);
        reserveInventoryStep.process(createdOrder);

        return createdOrder;
    }

    @Transactional
    public void handleInventoryReserved(UUID orderId) {
        log.info("Handling inventory reserved for order {}", orderId);

        Order order = findOrder(orderId);
        OrderContext context = new OrderContext(order);
        context.proceed();
        order.setStatus(context.getCurrentState().getStatus());
        orderRepository.save(order);

        processPaymentStep.process(order);
    }

    @Transactional
    public void handlePaymentCompleted(UUID orderId, String paymentId) {
        log.info("Handling payment completed for order {}", orderId);

        Order order = findOrder(orderId);
        order.setPaymentId(paymentId);

        OrderContext context = new OrderContext(order);
        context.proceed();
        order.setStatus(context.getCurrentState().getStatus());
        Order saved = orderRepository.save(order);

        publishOrderEvent(saved, EventTypes.ORDER_CONFIRMED);
    }

    @Transactional
    public void handleFailure(UUID orderId, String reason) {
        log.warn("Handling saga failure for order {}: {}", orderId, reason);

        Order order = findOrder(orderId);
        OrderStatus previousStatus = order.getStatus();

        OrderContext context = new OrderContext(order);
        context.cancel();
        order.setStatus(context.getCurrentState().getStatus());
        orderRepository.save(order);

        if (previousStatus == OrderStatus.INVENTORY_RESERVED ||
            previousStatus == OrderStatus.PAYMENT_PROCESSING) {
            publishOrderEvent(order, EventTypes.ORDER_CANCELLED);
        }
    }

    private Order findOrder(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
    }

    private void publishOrderEvent(Order order, String eventType) {
        List<OrderItemDto> itemDtos = order.getItems().stream()
                .map(item -> new OrderItemDto(
                        item.getProductId().toString(),
                        item.getProductName(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getTotalPrice()
                ))
                .toList();

        OrderEvent event = new OrderEvent(
                order.getId(),
                order.getUserId(),
                itemDtos,
                order.getTotalAmount(),
                order.getStatus().name()
        );

        DomainEvent<OrderEvent> domainEvent = DomainEvent.create(
                eventType,
                order.getId().toString(),
                event
        );

        kafkaTemplate.send(KafkaTopics.ORDER_EVENTS, order.getId().toString(), domainEvent);
        log.info("Published {} event for order {}", eventType, order.getId());
    }
}
