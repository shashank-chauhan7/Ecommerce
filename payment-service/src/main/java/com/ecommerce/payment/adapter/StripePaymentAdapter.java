package com.ecommerce.payment.adapter;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Stripe adapter — translates between our PaymentGateway interface and the Stripe SDK.
 */
@Slf4j
@Component("stripePaymentAdapter")
public class StripePaymentAdapter implements PaymentGateway {

    @Override
    public PaymentResult processPayment(PaymentRequest request) {
        try {
            long amountInCents = request.amount()
                    .multiply(BigDecimal.valueOf(100))
                    .longValueExact();

            PaymentIntentCreateParams.Builder paramsBuilder = PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency(request.currency().toLowerCase())
                    .setConfirm(true)
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .setAllowRedirects(PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER)
                                    .build()
                    )
                    .putMetadata("orderId", request.orderId().toString());

            if (request.customerEmail() != null) {
                paramsBuilder.setReceiptEmail(request.customerEmail());
            }

            com.stripe.net.RequestOptions requestOptions = com.stripe.net.RequestOptions.builder()
                    .setIdempotencyKey(request.idempotencyKey())
                    .build();

            PaymentIntent paymentIntent = PaymentIntent.create(paramsBuilder.build(), requestOptions);

            log.info("Stripe PaymentIntent created: id={}, status={}", paymentIntent.getId(), paymentIntent.getStatus());

            if ("succeeded".equals(paymentIntent.getStatus())) {
                return PaymentResult.successful(paymentIntent.getId());
            }

            return new PaymentResult(false, paymentIntent.getId(), paymentIntent.getStatus(),
                    "Payment requires additional action or failed");

        } catch (StripeException e) {
            log.error("Stripe payment failed: code={}, message={}", e.getCode(), e.getMessage());
            return PaymentResult.failed("Stripe error: " + e.getMessage());
        }
    }

    @Override
    public RefundResult refundPayment(String paymentIntentId, BigDecimal amount) {
        try {
            RefundCreateParams.Builder paramsBuilder = RefundCreateParams.builder()
                    .setPaymentIntent(paymentIntentId);

            if (amount != null) {
                long amountInCents = amount.multiply(BigDecimal.valueOf(100)).longValueExact();
                paramsBuilder.setAmount(amountInCents);
            }

            Refund refund = Refund.create(paramsBuilder.build());

            log.info("Stripe Refund created: id={}, status={}", refund.getId(), refund.getStatus());

            if ("succeeded".equals(refund.getStatus())) {
                return RefundResult.successful(refund.getId());
            }

            return new RefundResult(false, refund.getId(), refund.getStatus());

        } catch (StripeException e) {
            log.error("Stripe refund failed: code={}, message={}", e.getCode(), e.getMessage());
            return RefundResult.failed("Stripe error: " + e.getMessage());
        }
    }
}
