package com.ecommerce.order.listener;

import com.ecommerce.common.event.DomainEvent;
import com.ecommerce.common.event.EventTypes;
import com.ecommerce.common.event.KafkaTopics;
import com.ecommerce.order.saga.SagaMediator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final SagaMediator sagaMediator;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = KafkaTopics.INVENTORY_EVENTS, groupId = "order-service-group")
    public void handleInventoryEvent(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            String eventType = root.get("eventType").asText();
            String aggregateId = root.get("aggregateId").asText();
            UUID orderId = UUID.fromString(aggregateId);

            log.info("Received inventory event: {} for order {}", eventType, orderId);

            switch (eventType) {
                case EventTypes.INVENTORY_RESERVED -> sagaMediator.handleInventoryReserved(orderId);
                case EventTypes.INVENTORY_INSUFFICIENT -> sagaMediator.handleFailure(orderId, "Insufficient inventory");
                case EventTypes.RESERVATION_FAILED -> sagaMediator.handleFailure(orderId, "Inventory reservation failed");
                default -> log.warn("Unhandled inventory event type: {}", eventType);
            }
        } catch (Exception ex) {
            log.error("Error processing inventory event: {}", ex.getMessage(), ex);
        }
    }

    @KafkaListener(topics = KafkaTopics.PAYMENT_EVENTS, groupId = "order-service-group")
    public void handlePaymentEvent(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            String eventType = root.get("eventType").asText();
            String aggregateId = root.get("aggregateId").asText();
            UUID orderId = UUID.fromString(aggregateId);

            log.info("Received payment event: {} for order {}", eventType, orderId);

            switch (eventType) {
                case EventTypes.PAYMENT_COMPLETED -> {
                    String paymentId = "";
                    JsonNode payload = root.get("payload");
                    if (payload != null && payload.has("paymentId")) {
                        paymentId = payload.get("paymentId").asText();
                    }
                    sagaMediator.handlePaymentCompleted(orderId, paymentId);
                }
                case EventTypes.PAYMENT_FAILED -> sagaMediator.handleFailure(orderId, "Payment failed");
                default -> log.warn("Unhandled payment event type: {}", eventType);
            }
        } catch (Exception ex) {
            log.error("Error processing payment event: {}", ex.getMessage(), ex);
        }
    }
}
