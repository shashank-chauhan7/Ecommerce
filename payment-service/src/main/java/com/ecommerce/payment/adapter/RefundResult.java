package com.ecommerce.payment.adapter;

public record RefundResult(
        boolean success,
        String refundId,
        String status
) {
    public static RefundResult successful(String refundId) {
        return new RefundResult(true, refundId, "succeeded");
    }

    public static RefundResult failed(String status) {
        return new RefundResult(false, null, status);
    }
}
