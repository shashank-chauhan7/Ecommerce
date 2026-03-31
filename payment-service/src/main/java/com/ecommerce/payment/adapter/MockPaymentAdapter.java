package com.ecommerce.payment.adapter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Random;
import java.util.UUID;

/**
 * Mock adapter for testing and demo — simulates payment processing with random outcomes.
 */
@Slf4j
@Component("mockPaymentAdapter")
public class MockPaymentAdapter implements PaymentGateway {

    private static final double SUCCESS_RATE = 0.85;
    private final Random random = new Random();

    @Override
    public PaymentResult processPayment(PaymentRequest request) {
        log.info("Mock payment processing: orderId={}, amount={} {}", request.orderId(), request.amount(), request.currency());

        simulateLatency();

        if (random.nextDouble() < SUCCESS_RATE) {
            String fakeIntentId = "pi_mock_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
            log.info("Mock payment succeeded: intentId={}", fakeIntentId);
            return PaymentResult.successful(fakeIntentId);
        }

        log.warn("Mock payment failed for orderId={}", request.orderId());
        return PaymentResult.failed("Mock payment declined — simulated failure");
    }

    @Override
    public RefundResult refundPayment(String paymentIntentId, BigDecimal amount) {
        log.info("Mock refund processing: paymentIntentId={}, amount={}", paymentIntentId, amount);

        simulateLatency();

        String fakeRefundId = "re_mock_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        log.info("Mock refund succeeded: refundId={}", fakeRefundId);
        return RefundResult.successful(fakeRefundId);
    }

    private void simulateLatency() {
        try {
            Thread.sleep(200 + random.nextInt(300));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
