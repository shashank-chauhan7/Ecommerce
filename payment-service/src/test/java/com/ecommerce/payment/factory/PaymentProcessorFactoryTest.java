package com.ecommerce.payment.factory;

import com.ecommerce.payment.adapter.MockPaymentAdapter;
import com.ecommerce.payment.adapter.PaymentGateway;
import com.ecommerce.payment.adapter.StripePaymentAdapter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentProcessorFactoryTest {

    @Mock
    private ApplicationContext applicationContext;

    @Test
    void createPaymentGateway_stripe_returnsStripeAdapter() {
        StripePaymentAdapter stripeAdapter = new StripePaymentAdapter();
        when(applicationContext.getBean("stripePaymentAdapter", PaymentGateway.class))
                .thenReturn(stripeAdapter);

        PaymentProcessorFactory factory = new PaymentProcessorFactory(applicationContext, "stripe");

        PaymentGateway gateway = factory.createPaymentGateway();

        assertThat(gateway).isInstanceOf(StripePaymentAdapter.class);
    }

    @Test
    void createPaymentGateway_mock_returnsMockAdapter() {
        MockPaymentAdapter mockAdapter = new MockPaymentAdapter();
        when(applicationContext.getBean("mockPaymentAdapter", PaymentGateway.class))
                .thenReturn(mockAdapter);

        PaymentProcessorFactory factory = new PaymentProcessorFactory(applicationContext, "mock");

        PaymentGateway gateway = factory.createPaymentGateway();

        assertThat(gateway).isInstanceOf(MockPaymentAdapter.class);
    }

    @Test
    void createPaymentGateway_caseInsensitive() {
        StripePaymentAdapter stripeAdapter = new StripePaymentAdapter();
        when(applicationContext.getBean("stripePaymentAdapter", PaymentGateway.class))
                .thenReturn(stripeAdapter);

        PaymentProcessorFactory factory = new PaymentProcessorFactory(applicationContext, "STRIPE");

        PaymentGateway gateway = factory.createPaymentGateway();

        assertThat(gateway).isInstanceOf(StripePaymentAdapter.class);
    }

    @Test
    void createPaymentGateway_unknownType_throwsIllegalArgumentException() {
        PaymentProcessorFactory factory = new PaymentProcessorFactory(applicationContext, "paypal");

        assertThatThrownBy(factory::createPaymentGateway)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown payment processor type");
    }

    @Test
    void createPaymentGateway_defaultIsMock() {
        MockPaymentAdapter mockAdapter = new MockPaymentAdapter();
        when(applicationContext.getBean("mockPaymentAdapter", PaymentGateway.class))
                .thenReturn(mockAdapter);

        PaymentProcessorFactory factory = new PaymentProcessorFactory(applicationContext, "mock");

        PaymentGateway gateway = factory.createPaymentGateway();

        assertThat(gateway).isNotNull();
    }
}
