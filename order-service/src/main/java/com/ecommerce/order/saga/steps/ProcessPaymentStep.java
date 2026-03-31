package com.ecommerce.order.saga.steps;

import com.ecommerce.common.dto.OrderItemDto;
import com.ecommerce.common.event.DomainEvent;
import com.ecommerce.common.event.KafkaTopics;
import com.ecommerce.order.dto.OrderEvent;
import com.ecommerce.order.model.Order;
import com.ecommerce.order.model.OrderStatus;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.order.state.OrderContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProcessPaymentStep extends AbstractSagaStep<Order> {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final OrderRepository orderRepository;

    @Override
    protected void validate(Order order) {
        if (order.getStatus() != OrderStatus.INVENTORY_RESERVED) {
            throw new IllegalStateException("Order must be in INVENTORY_RESERVED state to process payment");
        }
    }

    @Override
    protected Order execute(Order order) {
        OrderContext context = new OrderContext(order);
        context.proceed();
        order.setStatus(context.getCurrentState().getStatus());
        Order saved = orderRepository.save(order);

        List<OrderItemDto> itemDtos = saved.getItems().stream()
                .map(item -> new OrderItemDto(
                        item.getProductId().toString(),
                        item.getProductName(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getTotalPrice()
                ))
                .toList();

        OrderEvent event = new OrderEvent(
                saved.getId(),
                saved.getUserId(),
                itemDtos,
                saved.getTotalAmount(),
                saved.getStatus().name()
        );

        DomainEvent<OrderEvent> domainEvent = DomainEvent.create(
                "PAYMENT_REQUESTED",
                saved.getId().toString(),
                event
        );

        kafkaTemplate.send(KafkaTopics.PAYMENT_EVENTS, saved.getId().toString(), domainEvent);
        return saved;
    }

    @Override
    protected void onSuccess(Order order) {
        log.info("Payment processing initiated for order {}", order.getId());
    }

    @Override
    protected void onFailure(Order order, Exception ex) {
        log.error("Failed to initiate payment for order {}: {}", order.getId(), ex.getMessage());
    }
}
