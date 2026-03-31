package com.ecommerce.payment.adapter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.stripe.exception.ApiException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StripePaymentAdapterTest {

    private StripePaymentAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new StripePaymentAdapter();
    }

    @Test
    void processPayment_success_returnsSuccessfulResult() {
        try (MockedStatic<PaymentIntent> mocked = mockStatic(PaymentIntent.class)) {
            PaymentIntent mockIntent = mock(PaymentIntent.class);
            when(mockIntent.getId()).thenReturn("pi_stripe_123");
            when(mockIntent.getStatus()).thenReturn("succeeded");

            mocked.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class), any()))
                    .thenReturn(mockIntent);

            PaymentRequest request = new PaymentRequest(
                    UUID.randomUUID(),
                    BigDecimal.valueOf(49.99),
                    "USD",
                    "idem-key-123",
                    "customer@example.com"
            );

            PaymentResult result = adapter.processPayment(request);

            assertThat(result.success()).isTrue();
            assertThat(result.paymentIntentId()).isEqualTo("pi_stripe_123");
            assertThat(result.status()).isEqualTo("succeeded");
            assertThat(result.failureReason()).isNull();
        }
    }

    @Test
    void processPayment_stripeError_returnsFailedResult() {
        try (MockedStatic<PaymentIntent> mocked = mockStatic(PaymentIntent.class)) {
            mocked.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class), any()))
                    .thenThrow(new ApiException("Card declined", "req_123", "card_declined", 402, null));

            PaymentRequest request = new PaymentRequest(
                    UUID.randomUUID(),
                    BigDecimal.valueOf(100.00),
                    "USD",
                    "idem-key-456",
                    "customer@example.com"
            );

            PaymentResult result = adapter.processPayment(request);

            assertThat(result.success()).isFalse();
            assertThat(result.failureReason()).contains("Stripe error");
        }
    }

    @Test
    void processPayment_requiresAction_returnsFailedResult() {
        try (MockedStatic<PaymentIntent> mocked = mockStatic(PaymentIntent.class)) {
            PaymentIntent mockIntent = mock(PaymentIntent.class);
            when(mockIntent.getId()).thenReturn("pi_stripe_456");
            when(mockIntent.getStatus()).thenReturn("requires_action");

            mocked.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class), any()))
                    .thenReturn(mockIntent);

            PaymentRequest request = new PaymentRequest(
                    UUID.randomUUID(),
                    BigDecimal.valueOf(200.00),
                    "EUR",
                    "idem-key-789",
                    null
            );

            PaymentResult result = adapter.processPayment(request);

            assertThat(result.success()).isFalse();
            assertThat(result.paymentIntentId()).isEqualTo("pi_stripe_456");
            assertThat(result.status()).isEqualTo("requires_action");
        }
    }

    @Test
    void refundPayment_success_returnsSuccessfulRefundResult() {
        try (MockedStatic<Refund> mocked = mockStatic(Refund.class)) {
            Refund mockRefund = mock(Refund.class);
            when(mockRefund.getId()).thenReturn("re_stripe_123");
            when(mockRefund.getStatus()).thenReturn("succeeded");

            mocked.when(() -> Refund.create(any(RefundCreateParams.class)))
                    .thenReturn(mockRefund);

            RefundResult result = adapter.refundPayment("pi_stripe_123", BigDecimal.valueOf(49.99));

            assertThat(result.success()).isTrue();
            assertThat(result.refundId()).isEqualTo("re_stripe_123");
        }
    }

    @Test
    void refundPayment_stripeError_returnsFailedRefundResult() {
        try (MockedStatic<Refund> mocked = mockStatic(Refund.class)) {
            mocked.when(() -> Refund.create(any(RefundCreateParams.class)))
                    .thenThrow(new ApiException("Refund failed", "req_456", "refund_error", 400, null));

            RefundResult result = adapter.refundPayment("pi_stripe_123", BigDecimal.valueOf(49.99));

            assertThat(result.success()).isFalse();
            assertThat(result.status()).contains("Stripe error");
        }
    }
}
