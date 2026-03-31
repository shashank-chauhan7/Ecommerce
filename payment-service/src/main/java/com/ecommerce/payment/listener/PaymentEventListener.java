package com.ecommerce.payment.listener;

import com.ecommerce.payment.dto.PaymentDto;
import com.ecommerce.payment.service.PaymentService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "inventory-events", groupId = "${spring.kafka.consumer.group-id}")
    public void onInventoryEvent(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        try {
            log.info("Received inventory event: key={}", record.key());

            JsonNode event = objectMapper.readTree(record.value());
            String eventType = event.get("eventType").asText();

            if ("INVENTORY_RESERVED".equals(eventType)) {
                UUID orderId = UUID.fromString(event.get("orderId").asText());
                UUID userId = UUID.fromString(event.get("userId").asText());
                BigDecimal amount = new BigDecimal(event.get("amount").asText());
                String currency = event.has("currency") ? event.get("currency").asText() : "USD";
                String customerEmail = event.has("customerEmail") ? event.get("customerEmail").asText() : null;

                log.info("Processing payment for INVENTORY_RESERVED: orderId={}, amount={}", orderId, amount);

                PaymentDto result = paymentService.processPayment(orderId, userId, amount, currency, customerEmail);
                log.info("Payment result for orderId={}: status={}", orderId, result.status());
            } else {
                log.debug("Ignoring inventory event: type={}", eventType);
            }

            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process inventory event: key={}", record.key(), e);
            acknowledgment.acknowledge();
        }
    }
}
