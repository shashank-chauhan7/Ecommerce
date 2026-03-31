package com.ecommerce.payment.service;

import com.ecommerce.payment.adapter.PaymentGateway;
import com.ecommerce.payment.adapter.PaymentResult;
import com.ecommerce.payment.adapter.RefundResult;
import com.ecommerce.payment.dto.PaymentDto;
import com.ecommerce.payment.dto.PaymentEvent;
import com.ecommerce.payment.factory.PaymentProcessorFactory;
import com.ecommerce.payment.model.IdempotencyRecord;
import com.ecommerce.payment.model.Payment;
import com.ecommerce.payment.model.PaymentStatus;
import com.ecommerce.payment.repository.IdempotencyRecordRepository;
import com.ecommerce.payment.repository.PaymentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final String PAYMENT_EVENTS_TOPIC = "payment-events";

    private final PaymentRepository paymentRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final PaymentProcessorFactory paymentProcessorFactory;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public PaymentDto processPayment(UUID orderId, UUID userId, BigDecimal amount, String currency, String customerEmail) {
        String idempotencyKey = "payment-" + orderId;
        log.info("Processing payment: orderId={}, idempotencyKey={}", orderId, idempotencyKey);

        Optional<IdempotencyRecord> existingRecord = idempotencyRecordRepository.findByIdempotencyKey(idempotencyKey);

        if (existingRecord.isPresent()) {
            IdempotencyRecord record = existingRecord.get();

            if ("COMPLETED".equals(record.getStatus())) {
                log.info("Returning cached result for idempotencyKey={}", idempotencyKey);
                return deserializePaymentDto(record.getResponseBody());
            }

            if ("FAILED".equals(record.getStatus())) {
                log.info("Previous attempt failed, retrying for idempotencyKey={}", idempotencyKey);
                idempotencyRecordRepository.delete(record);
            }
        }

        IdempotencyRecord idempotencyRecord = IdempotencyRecord.builder()
                .idempotencyKey(idempotencyKey)
                .status("PENDING")
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();
        idempotencyRecordRepository.save(idempotencyRecord);

        PaymentGateway gateway = paymentProcessorFactory.createPaymentGateway();
        com.ecommerce.payment.adapter.PaymentRequest gatewayRequest =
                new com.ecommerce.payment.adapter.PaymentRequest(orderId, amount, currency != null ? currency : "USD", idempotencyKey, customerEmail);

        PaymentResult result = gateway.processPayment(gatewayRequest);

        Payment payment = Payment.builder()
                .orderId(orderId)
                .userId(userId)
                .amount(amount)
                .currency(currency != null ? currency : "USD")
                .idempotencyKey(idempotencyKey)
                .stripePaymentIntentId(result.paymentIntentId())
                .status(result.success() ? PaymentStatus.COMPLETED : PaymentStatus.FAILED)
                .failureReason(result.failureReason())
                .build();

        payment = paymentRepository.save(payment);

        PaymentDto paymentDto = toDto(payment);

        idempotencyRecord.setStatus(result.success() ? "COMPLETED" : "FAILED");
        idempotencyRecord.setResponseBody(serializePaymentDto(paymentDto));
        idempotencyRecordRepository.save(idempotencyRecord);

        String eventType = result.success() ? "PAYMENT_COMPLETED" : "PAYMENT_FAILED";
        publishEvent(eventType, paymentDto);

        log.info("Payment processed: id={}, status={}", payment.getId(), payment.getStatus());
        return paymentDto;
    }

    @Override
    @Transactional
    public PaymentDto refundPayment(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new IllegalStateException("Only completed payments can be refunded. Current status: " + payment.getStatus());
        }

        PaymentGateway gateway = paymentProcessorFactory.createPaymentGateway();
        RefundResult result = gateway.refundPayment(payment.getStripePaymentIntentId(), payment.getAmount());

        if (result.success()) {
            payment.setStatus(PaymentStatus.REFUNDED);
        } else {
            payment.setFailureReason("Refund failed: " + result.status());
        }

        payment = paymentRepository.save(payment);
        PaymentDto paymentDto = toDto(payment);

        if (result.success()) {
            publishEvent("PAYMENT_REFUNDED", paymentDto);
        }

        log.info("Refund processed: paymentId={}, success={}", paymentId, result.success());
        return paymentDto;
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentDto getPayment(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));
        return toDto(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentDto getPaymentByOrderId(UUID orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found for order: " + orderId));
        return toDto(payment);
    }

    private void publishEvent(String eventType, PaymentDto payment) {
        try {
            PaymentEvent event = PaymentEvent.of(eventType, payment);
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(PAYMENT_EVENTS_TOPIC, payment.orderId().toString(), payload);
            log.info("Published {} event for orderId={}", eventType, payment.orderId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize payment event", e);
        }
    }

    private PaymentDto toDto(Payment payment) {
        return new PaymentDto(
                payment.getId(),
                payment.getOrderId(),
                payment.getUserId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getStripePaymentIntentId(),
                payment.getIdempotencyKey(),
                payment.getFailureReason(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }

    private String serializePaymentDto(PaymentDto dto) {
        try {
            return objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize PaymentDto", e);
            return null;
        }
    }

    private PaymentDto deserializePaymentDto(String json) {
        try {
            return objectMapper.readValue(json, PaymentDto.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize PaymentDto", e);
            throw new RuntimeException("Corrupted idempotency record", e);
        }
    }
}
