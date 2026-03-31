package com.ecommerce.payment.dto;

import com.ecommerce.payment.model.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentDto(
        UUID id,
        UUID orderId,
        UUID userId,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        String stripePaymentIntentId,
        String idempotencyKey,
        String failureReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
