package com.ecommerce.common.exception;

public class PaymentException extends RuntimeException {

    private final String orderId;
    private final String reason;

    public PaymentException(String orderId, String reason) {
        super(String.format("Payment failed for order %s: %s", orderId, reason));
        this.orderId = orderId;
        this.reason = reason;
    }

    public PaymentException(String orderId, String reason, Throwable cause) {
        super(String.format("Payment failed for order %s: %s", orderId, reason), cause);
        this.orderId = orderId;
        this.reason = reason;
    }

    public String getOrderId() { return orderId; }
    public String getReason() { return reason; }
}
