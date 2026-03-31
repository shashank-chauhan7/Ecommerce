package com.ecommerce.payment.service;

import com.ecommerce.payment.adapter.PaymentGateway;
import com.ecommerce.payment.adapter.PaymentResult;
import com.ecommerce.payment.adapter.RefundResult;
import com.ecommerce.payment.dto.PaymentDto;
import com.ecommerce.payment.factory.PaymentProcessorFactory;
import com.ecommerce.payment.model.IdempotencyRecord;
import com.ecommerce.payment.model.Payment;
import com.ecommerce.payment.model.PaymentStatus;
import com.ecommerce.payment.repository.IdempotencyRecordRepository;
import com.ecommerce.payment.repository.PaymentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private IdempotencyRecordRepository idempotencyRecordRepository;

    @Mock
    private PaymentProcessorFactory paymentProcessorFactory;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private PaymentGateway paymentGateway;

    private PaymentServiceImpl paymentService;

    private UUID orderId;
    private UUID userId;
    private UUID paymentId;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        paymentService = new PaymentServiceImpl(
                paymentRepository,
                idempotencyRecordRepository,
                paymentProcessorFactory,
                kafkaTemplate,
                objectMapper
        );

        orderId = UUID.randomUUID();
        userId = UUID.randomUUID();
        paymentId = UUID.randomUUID();
    }

    @Test
    void processPayment_success() {
        when(idempotencyRecordRepository.findByIdempotencyKey("payment-" + orderId))
                .thenReturn(Optional.empty());
        when(idempotencyRecordRepository.save(any(IdempotencyRecord.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(paymentProcessorFactory.createPaymentGateway()).thenReturn(paymentGateway);
        when(paymentGateway.processPayment(any())).thenReturn(PaymentResult.successful("pi_test_123"));

        Payment savedPayment = Payment.builder()
                .id(paymentId)
                .orderId(orderId)
                .userId(userId)
                .amount(BigDecimal.valueOf(100.00))
                .currency("USD")
                .status(PaymentStatus.COMPLETED)
                .stripePaymentIntentId("pi_test_123")
                .idempotencyKey("payment-" + orderId)
                .build();
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        PaymentDto result = paymentService.processPayment(orderId, userId, BigDecimal.valueOf(100.00), "USD", "test@example.com");

        assertThat(result.status()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(result.stripePaymentIntentId()).isEqualTo("pi_test_123");

        verify(paymentRepository).save(any(Payment.class));
        verify(kafkaTemplate).send(eq("payment-events"), anyString(), anyString());
    }

    @Test
    void processPayment_idempotent_returnsCachedResult() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        PaymentDto cachedDto = new PaymentDto(
                paymentId, orderId, userId, BigDecimal.valueOf(100.00), "USD",
                PaymentStatus.COMPLETED, "pi_test_123", "payment-" + orderId,
                null, LocalDateTime.now(), LocalDateTime.now()
        );
        String cachedJson = objectMapper.writeValueAsString(cachedDto);

        IdempotencyRecord record = IdempotencyRecord.builder()
                .idempotencyKey("payment-" + orderId)
                .status("COMPLETED")
                .responseBody(cachedJson)
                .build();

        when(idempotencyRecordRepository.findByIdempotencyKey("payment-" + orderId))
                .thenReturn(Optional.of(record));

        PaymentDto result = paymentService.processPayment(orderId, userId, BigDecimal.valueOf(100.00), "USD", "test@example.com");

        assertThat(result.status()).isEqualTo(PaymentStatus.COMPLETED);
        verify(paymentRepository, never()).save(any());
        verify(paymentGateway, never()).processPayment(any());
    }

    @Test
    void processPayment_retryAfterFailure() {
        IdempotencyRecord failedRecord = IdempotencyRecord.builder()
                .idempotencyKey("payment-" + orderId)
                .status("FAILED")
                .build();

        when(idempotencyRecordRepository.findByIdempotencyKey("payment-" + orderId))
                .thenReturn(Optional.of(failedRecord));
        when(idempotencyRecordRepository.save(any(IdempotencyRecord.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(paymentProcessorFactory.createPaymentGateway()).thenReturn(paymentGateway);
        when(paymentGateway.processPayment(any())).thenReturn(PaymentResult.successful("pi_retry_456"));

        Payment savedPayment = Payment.builder()
                .id(paymentId)
                .orderId(orderId)
                .userId(userId)
                .amount(BigDecimal.valueOf(100.00))
                .currency("USD")
                .status(PaymentStatus.COMPLETED)
                .stripePaymentIntentId("pi_retry_456")
                .idempotencyKey("payment-" + orderId)
                .build();
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        PaymentDto result = paymentService.processPayment(orderId, userId, BigDecimal.valueOf(100.00), "USD", "test@example.com");

        assertThat(result.status()).isEqualTo(PaymentStatus.COMPLETED);
        verify(idempotencyRecordRepository).delete(failedRecord);
        verify(paymentGateway).processPayment(any());
    }

    @Test
    void refundPayment_success() {
        Payment completedPayment = Payment.builder()
                .id(paymentId)
                .orderId(orderId)
                .userId(userId)
                .amount(BigDecimal.valueOf(100.00))
                .currency("USD")
                .status(PaymentStatus.COMPLETED)
                .stripePaymentIntentId("pi_test_123")
                .build();

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(completedPayment));
        when(paymentProcessorFactory.createPaymentGateway()).thenReturn(paymentGateway);
        when(paymentGateway.refundPayment("pi_test_123", BigDecimal.valueOf(100.00)))
                .thenReturn(RefundResult.successful("re_test_789"));

        Payment refundedPayment = Payment.builder()
                .id(paymentId)
                .orderId(orderId)
                .userId(userId)
                .amount(BigDecimal.valueOf(100.00))
                .currency("USD")
                .status(PaymentStatus.REFUNDED)
                .stripePaymentIntentId("pi_test_123")
                .build();
        when(paymentRepository.save(any(Payment.class))).thenReturn(refundedPayment);
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        PaymentDto result = paymentService.refundPayment(paymentId);

        assertThat(result.status()).isEqualTo(PaymentStatus.REFUNDED);
        verify(paymentGateway).refundPayment("pi_test_123", BigDecimal.valueOf(100.00));
    }

    @Test
    void refundPayment_notCompleted_throwsIllegalStateException() {
        Payment pendingPayment = Payment.builder()
                .id(paymentId)
                .orderId(orderId)
                .status(PaymentStatus.PENDING)
                .build();

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(pendingPayment));

        assertThatThrownBy(() -> paymentService.refundPayment(paymentId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only completed payments can be refunded");
    }

    @Test
    void getPayment_found() {
        Payment payment = Payment.builder()
                .id(paymentId)
                .orderId(orderId)
                .userId(userId)
                .amount(BigDecimal.valueOf(50.00))
                .currency("USD")
                .status(PaymentStatus.COMPLETED)
                .build();

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        PaymentDto result = paymentService.getPayment(paymentId);

        assertThat(result.id()).isEqualTo(paymentId);
        assertThat(result.amount()).isEqualByComparingTo(BigDecimal.valueOf(50.00));
    }

    @Test
    void getPayment_notFound_throwsException() {
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getPayment(paymentId))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void processPayment_defaultsCurrencyToUSD() {
        when(idempotencyRecordRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(idempotencyRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paymentProcessorFactory.createPaymentGateway()).thenReturn(paymentGateway);
        when(paymentGateway.processPayment(any())).thenReturn(PaymentResult.successful("pi_def"));

        Payment saved = Payment.builder()
                .id(paymentId).orderId(orderId).userId(userId)
                .amount(BigDecimal.TEN).currency("USD").status(PaymentStatus.COMPLETED)
                .stripePaymentIntentId("pi_def").idempotencyKey("payment-" + orderId).build();
        when(paymentRepository.save(any())).thenReturn(saved);
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        PaymentDto result = paymentService.processPayment(orderId, userId, BigDecimal.TEN, null, "e@e.com");

        assertThat(result.currency()).isEqualTo("USD");
    }
}
