package com.ecommerce.payment.factory;

import com.ecommerce.payment.adapter.PaymentGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Factory Method pattern — returns the appropriate PaymentGateway implementation
 * based on the configured processor type.
 */
@Slf4j
@Component
public class PaymentProcessorFactory {

    private final ApplicationContext applicationContext;
    private final String processorType;

    public PaymentProcessorFactory(ApplicationContext applicationContext,
                                   @Value("${app.payment.processor:mock}") String processorType) {
        this.applicationContext = applicationContext;
        this.processorType = processorType;
        log.info("Payment processor configured: {}", processorType);
    }

    public PaymentGateway createPaymentGateway() {
        return switch (processorType.toLowerCase()) {
            case "stripe" -> applicationContext.getBean("stripePaymentAdapter", PaymentGateway.class);
            case "mock" -> applicationContext.getBean("mockPaymentAdapter", PaymentGateway.class);
            default -> throw new IllegalArgumentException("Unknown payment processor type: " + processorType);
        };
    }
}
