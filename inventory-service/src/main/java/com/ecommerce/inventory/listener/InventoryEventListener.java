package com.ecommerce.inventory.listener;

import com.ecommerce.common.event.DomainEvent;
import com.ecommerce.common.event.EventTypes;
import com.ecommerce.common.event.KafkaTopics;
import com.ecommerce.common.exception.InsufficientStockException;
import com.ecommerce.inventory.command.InventoryCommand;
import com.ecommerce.inventory.command.ReleaseInventoryCommand;
import com.ecommerce.inventory.command.ReserveInventoryCommand;
import com.ecommerce.inventory.repository.InventoryRepository;
import com.ecommerce.inventory.service.DistributedLockService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryEventListener {

    private final InventoryRepository repository;
    private final DistributedLockService lockService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = KafkaTopics.ORDER_EVENTS, groupId = "inventory-group")
    public void handleOrderEvent(String message) {
        try {
            DomainEvent<Map<String, Object>> event = objectMapper.readValue(
                    message, new TypeReference<>() {});

            log.info("Received event: type={}, aggregateId={}", event.eventType(), event.aggregateId());

            switch (event.eventType()) {
                case EventTypes.ORDER_CREATED -> handleOrderCreated(event);
                case EventTypes.ORDER_CANCELLED -> handleOrderCancelled(event);
                case EventTypes.PAYMENT_FAILED -> handlePaymentFailed(event);
                default -> log.debug("Ignoring event type: {}", event.eventType());
            }
        } catch (InsufficientStockException e) {
            log.warn("Insufficient stock: {}", e.getMessage());
            publishInsufficientStockEvent(e, message);
        } catch (Exception e) {
            log.error("Error processing order event: {}", e.getMessage(), e);
        }
    }

    private void handleOrderCreated(DomainEvent<Map<String, Object>> event) {
        Map<String, Object> payload = event.payload();
        String region = (String) payload.get("region");
        UUID productId = UUID.fromString((String) payload.get("productId"));
        int quantity = ((Number) payload.get("quantity")).intValue();
        String orderId = event.aggregateId();

        InventoryCommand command = new ReserveInventoryCommand(
                region, productId, quantity, orderId, repository, lockService, kafkaTemplate);
        command.execute();
    }

    private void handleOrderCancelled(DomainEvent<Map<String, Object>> event) {
        Map<String, Object> payload = event.payload();
        String region = (String) payload.get("region");
        UUID productId = UUID.fromString((String) payload.get("productId"));
        int quantity = ((Number) payload.get("quantity")).intValue();
        String orderId = event.aggregateId();

        InventoryCommand command = new ReleaseInventoryCommand(
                region, productId, quantity, orderId, repository, lockService, kafkaTemplate);
        command.execute();
    }

    private void handlePaymentFailed(DomainEvent<Map<String, Object>> event) {
        Map<String, Object> payload = event.payload();
        String region = (String) payload.get("region");
        UUID productId = UUID.fromString((String) payload.get("productId"));
        int quantity = ((Number) payload.get("quantity")).intValue();
        String orderId = event.aggregateId();

        InventoryCommand command = new ReleaseInventoryCommand(
                region, productId, quantity, orderId, repository, lockService, kafkaTemplate);
        command.execute();
        log.info("Compensating release for payment failure: order={}", orderId);
    }

    private void publishInsufficientStockEvent(InsufficientStockException e, String rawMessage) {
        try {
            DomainEvent<Map<String, Object>> original = objectMapper.readValue(
                    rawMessage, new TypeReference<>() {});

            DomainEvent<Map<String, Object>> failEvent = DomainEvent.create(
                    EventTypes.INVENTORY_INSUFFICIENT,
                    original.aggregateId(),
                    Map.of(
                            "orderId", original.aggregateId(),
                            "productId", e.getProductId(),
                            "requested", e.getRequested(),
                            "available", e.getAvailable()
                    )
            );
            kafkaTemplate.send(KafkaTopics.INVENTORY_EVENTS, original.aggregateId(), failEvent);
        } catch (Exception ex) {
            log.error("Failed to publish insufficient stock event", ex);
        }
    }
}
