package com.ecommerce.order.saga;

import com.ecommerce.common.dto.OrderItemDto;
import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.model.Order;
import com.ecommerce.order.model.OrderItem;
import com.ecommerce.order.model.OrderStatus;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.order.saga.steps.CreateOrderStep;
import com.ecommerce.order.saga.steps.ProcessPaymentStep;
import com.ecommerce.order.saga.steps.ReserveInventoryStep;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SagaMediatorTest {

    @Mock
    private CreateOrderStep createOrderStep;

    @Mock
    private ReserveInventoryStep reserveInventoryStep;

    @Mock
    private ProcessPaymentStep processPaymentStep;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private SagaMediator sagaMediator;

    private UUID orderId;
    private UUID userId;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        userId = UUID.randomUUID();

        OrderItem item = OrderItem.builder()
                .id(UUID.randomUUID())
                .productId(UUID.randomUUID())
                .productName("Test Product")
                .quantity(2)
                .unitPrice(BigDecimal.valueOf(25.00))
                .totalPrice(BigDecimal.valueOf(50.00))
                .build();

        testOrder = Order.builder()
                .id(orderId)
                .userId(userId)
                .status(OrderStatus.PENDING)
                .items(new ArrayList<>(List.of(item)))
                .totalAmount(BigDecimal.valueOf(50.00))
                .shippingAddress("123 Main St")
                .build();

        item.setOrder(testOrder);
    }

    @Test
    void startOrderSaga_createsOrderAndPublishesEvent() {
        CreateOrderRequest request = new CreateOrderRequest(
                List.of(new OrderItemDto(
                        UUID.randomUUID().toString(),
                        "Test Product",
                        2,
                        BigDecimal.valueOf(25.00),
                        BigDecimal.valueOf(50.00)
                )),
                "123 Main St"
        );

        when(createOrderStep.process(any(Order.class))).thenReturn(testOrder);
        when(reserveInventoryStep.process(testOrder)).thenReturn(testOrder);

        Order result = sagaMediator.startOrderSaga(request, userId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(orderId);

        verify(createOrderStep).process(any(Order.class));
        verify(reserveInventoryStep).process(testOrder);
    }

    @Test
    void handleInventoryReserved_advancesToPaymentProcessing() {
        testOrder.setStatus(OrderStatus.PENDING);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(processPaymentStep.process(any(Order.class))).thenReturn(testOrder);

        sagaMediator.handleInventoryReserved(orderId);

        verify(orderRepository).save(any(Order.class));
        verify(processPaymentStep).process(any(Order.class));
    }

    @Test
    void handlePaymentCompleted_advancesToConfirmed() {
        testOrder.setStatus(OrderStatus.PAYMENT_PROCESSING);
        String paymentId = "pi_test_123";

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        sagaMediator.handlePaymentCompleted(orderId, paymentId);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().getPaymentId()).isEqualTo(paymentId);

        verify(kafkaTemplate).send(anyString(), anyString(), any());
    }

    @Test
    void handleFailure_cancelsOrderAndTriggersCompensation() {
        testOrder.setStatus(OrderStatus.INVENTORY_RESERVED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        sagaMediator.handleFailure(orderId, "Payment failed");

        verify(orderRepository).save(any(Order.class));
        verify(kafkaTemplate).send(anyString(), anyString(), any());
    }

    @Test
    void handleFailure_fromPaymentProcessing_cancelsAndPublishesEvent() {
        testOrder.setStatus(OrderStatus.PAYMENT_PROCESSING);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        sagaMediator.handleFailure(orderId, "Payment declined");

        verify(orderRepository).save(any(Order.class));
        verify(kafkaTemplate).send(anyString(), anyString(), any());
    }

    @Test
    void handleFailure_fromCreatedState_cancelsWithoutCompensation() {
        testOrder.setStatus(OrderStatus.CREATED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        sagaMediator.handleFailure(orderId, "Validation failed");

        verify(orderRepository).save(any(Order.class));
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    @Test
    void startOrderSaga_calculatesTotalFromItems() {
        CreateOrderRequest request = new CreateOrderRequest(
                List.of(
                        new OrderItemDto(UUID.randomUUID().toString(), "Item A", 1, BigDecimal.TEN, BigDecimal.TEN),
                        new OrderItemDto(UUID.randomUUID().toString(), "Item B", 2, BigDecimal.valueOf(20), BigDecimal.valueOf(40))
                ),
                "456 Oak Ave"
        );

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        when(createOrderStep.process(orderCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));
        when(reserveInventoryStep.process(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = sagaMediator.startOrderSaga(request, userId);

        Order capturedOrder = orderCaptor.getValue();
        assertThat(capturedOrder.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(50));
        assertThat(capturedOrder.getItems()).hasSize(2);
        assertThat(capturedOrder.getShippingAddress()).isEqualTo("456 Oak Ave");
    }
}
