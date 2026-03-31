package com.ecommerce.payment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentEvent(
        String eventType,
        UUID paymentId,
        UUID orderId,
        UUID userId,
        BigDecimal amount,
        String currency,
        String status,
        String stripePaymentIntentId,
        String failureReason,
        LocalDateTime timestamp
) {
    public static PaymentEvent of(String eventType, PaymentDto payment) {
        return new PaymentEvent(
                eventType,
                payment.id(),
                payment.orderId(),
                payment.userId(),
                payment.amount(),
                payment.currency(),
                payment.status().name(),
                payment.stripePaymentIntentId(),
                payment.failureReason(),
                LocalDateTime.now()
        );
    }
}
