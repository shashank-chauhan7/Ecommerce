package com.ecommerce.payment.service;

import com.ecommerce.payment.dto.PaymentDto;

import java.math.BigDecimal;
import java.util.UUID;

public interface PaymentService {

    PaymentDto processPayment(UUID orderId, UUID userId, BigDecimal amount, String currency, String customerEmail);

    PaymentDto refundPayment(UUID paymentId);

    PaymentDto getPayment(UUID paymentId);

    PaymentDto getPaymentByOrderId(UUID orderId);
}
