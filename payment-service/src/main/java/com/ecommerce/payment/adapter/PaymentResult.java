package com.ecommerce.payment.adapter;

public record PaymentResult(
        boolean success,
        String paymentIntentId,
        String status,
        String failureReason
) {
    public static PaymentResult successful(String paymentIntentId) {
        return new PaymentResult(true, paymentIntentId, "succeeded", null);
    }

    public static PaymentResult failed(String reason) {
        return new PaymentResult(false, null, "failed", reason);
    }
}
