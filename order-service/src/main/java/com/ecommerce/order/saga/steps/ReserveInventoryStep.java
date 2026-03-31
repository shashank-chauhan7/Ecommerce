package com.ecommerce.order.saga.steps;

import com.ecommerce.common.dto.OrderItemDto;
import com.ecommerce.common.event.DomainEvent;
import com.ecommerce.common.event.EventTypes;
import com.ecommerce.common.event.KafkaTopics;
import com.ecommerce.order.dto.OrderEvent;
import com.ecommerce.order.model.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReserveInventoryStep extends AbstractSagaStep<Order> {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    protected void validate(Order order) {
        if (order.getId() == null) {
            throw new IllegalStateException("Order must be persisted before reserving inventory");
        }
    }

    @Override
    protected Order execute(Order order) {
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
                EventTypes.ORDER_CREATED,
                order.getId().toString(),
                event
        );

        kafkaTemplate.send(KafkaTopics.ORDER_EVENTS, order.getId().toString(), domainEvent);
        return order;
    }

    @Override
    protected void onSuccess(Order order) {
        log.info("ORDER_CREATED event published for order {}", order.getId());
    }

    @Override
    protected void onFailure(Order order, Exception ex) {
        log.error("Failed to publish ORDER_CREATED event for order {}: {}", order.getId(), ex.getMessage());
    }
}
