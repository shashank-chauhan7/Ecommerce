package com.ecommerce.notification.listener;

import com.ecommerce.common.event.DomainEvent;
import com.ecommerce.common.event.EventTypes;
import com.ecommerce.common.event.KafkaTopics;
import com.ecommerce.common.util.JsonUtil;
import com.ecommerce.notification.service.NotificationService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private static final ObjectMapper MAPPER = JsonUtil.getMapper();
    private static final TypeReference<DomainEvent<Map<String, Object>>> EVENT_TYPE_REF = new TypeReference<>() {};

    private final NotificationService notificationService;

    @KafkaListener(topics = KafkaTopics.ORDER_EVENTS, groupId = "notification-group")
    public void handleOrderEvents(String message) {
        log.info("Received order event: {}", message);
        try {
            DomainEvent<Map<String, Object>> event = MAPPER.readValue(message, EVENT_TYPE_REF);
            processOrderEvent(event);
        } catch (Exception e) {
            log.error("Failed to process order event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = KafkaTopics.NOTIFICATION_EVENTS, groupId = "notification-group")
    public void handleNotificationEvents(String message) {
        log.info("Received notification event: {}", message);
        try {
            DomainEvent<Map<String, Object>> event = MAPPER.readValue(message, EVENT_TYPE_REF);
            processNotificationEvent(event);
        } catch (Exception e) {
            log.error("Failed to process notification event: {}", e.getMessage(), e);
        }
    }

    private void processOrderEvent(DomainEvent<Map<String, Object>> event) {
        Map<String, Object> payload = event.payload();
        String orderId = event.aggregateId();

        switch (event.eventType()) {
            case EventTypes.ORDER_CONFIRMED -> {
                String email = (String) payload.get("email");
                log.info("Processing ORDER_CONFIRMED for orderId={}, email={}", orderId, email);
                notificationService.sendOrderConfirmation(orderId, email, payload);
            }
            case EventTypes.ORDER_SHIPPED -> {
                String email = (String) payload.get("email");
                log.info("Processing ORDER_SHIPPED for orderId={}, email={}", orderId, email);
                notificationService.sendShippingUpdate(orderId, email, payload);
            }
            default -> log.debug("Ignoring order event type: {}", event.eventType());
        }
    }

    private void processNotificationEvent(DomainEvent<Map<String, Object>> event) {
        Map<String, Object> payload = event.payload();

        switch (event.eventType()) {
            case EventTypes.SEND_EMAIL -> {
                String email = (String) payload.get("email");
                String type = (String) payload.getOrDefault("notificationType", "PROMOTIONAL");
                log.info("Processing SEND_EMAIL for email={}, type={}", email, type);
                notificationService.sendPromotionalEmail(email, payload);
            }
            default -> log.debug("Ignoring notification event type: {}", event.eventType());
        }
    }
}
