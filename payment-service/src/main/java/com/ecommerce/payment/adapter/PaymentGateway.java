package com.ecommerce.payment.adapter;

import java.math.BigDecimal;

/**
 * Adapter pattern interface — abstracts away the specific payment provider.
 * Implementations translate between our domain model and external payment APIs.
 */
public interface PaymentGateway {

    PaymentResult processPayment(PaymentRequest request);

    RefundResult refundPayment(String paymentIntentId, BigDecimal amount);
}
