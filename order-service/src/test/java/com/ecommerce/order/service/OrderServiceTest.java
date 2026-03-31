package com.ecommerce.order.service;

import com.ecommerce.common.dto.OrderItemDto;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.dto.OrderDto;
import com.ecommerce.order.model.Order;
import com.ecommerce.order.model.OrderItem;
import com.ecommerce.order.model.OrderStatus;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.order.saga.SagaMediator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private SagaMediator sagaMediator;

    @InjectMocks
    private OrderServiceImpl orderService;

    private UUID orderId;
    private UUID userId;
    private Order testOrder;
    private OrderItem testItem;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        userId = UUID.randomUUID();

        testItem = OrderItem.builder()
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
                .status(OrderStatus.CREATED)
                .items(new java.util.ArrayList<>(List.of(testItem)))
                .totalAmount(BigDecimal.valueOf(50.00))
                .shippingAddress("123 Main St")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        testItem.setOrder(testOrder);
    }

    @Test
    void createOrder_success() {
        CreateOrderRequest request = new CreateOrderRequest(
                List.of(new OrderItemDto(
                        testItem.getProductId().toString(),
                        "Test Product",
                        2,
                        BigDecimal.valueOf(25.00),
                        BigDecimal.valueOf(50.00)
                )),
                "123 Main St"
        );

        when(sagaMediator.startOrderSaga(request, userId)).thenReturn(testOrder);

        OrderDto result = orderService.createOrder(request, userId);

        assertThat(result.id()).isEqualTo(orderId);
        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.status()).isEqualTo(OrderStatus.CREATED);
        assertThat(result.totalAmount()).isEqualByComparingTo(BigDecimal.valueOf(50.00));
        assertThat(result.items()).hasSize(1);

        verify(sagaMediator).startOrderSaga(request, userId);
    }

    @Test
    void getOrderById_found() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

        OrderDto result = orderService.getOrder(orderId);

        assertThat(result.id()).isEqualTo(orderId);
        assertThat(result.shippingAddress()).isEqualTo("123 Main St");

        verify(orderRepository).findById(orderId);
    }

    @Test
    void getOrderById_notFound_throwsResourceNotFoundException() {
        UUID missingId = UUID.randomUUID();
        when(orderRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrder(missingId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(orderRepository).findById(missingId);
    }

    @Test
    void cancelOrder_success() {
        testOrder.setStatus(OrderStatus.CREATED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

        Order cancelledOrder = Order.builder()
                .id(orderId)
                .userId(userId)
                .status(OrderStatus.CANCELLED)
                .items(new java.util.ArrayList<>(List.of(testItem)))
                .totalAmount(BigDecimal.valueOf(50.00))
                .shippingAddress("123 Main St")
                .build();
        when(orderRepository.findById(orderId))
                .thenReturn(Optional.of(testOrder))
                .thenReturn(Optional.of(cancelledOrder));

        OrderDto result = orderService.cancelOrder(orderId, userId);

        assertThat(result.status()).isEqualTo(OrderStatus.CANCELLED);
        verify(sagaMediator).handleFailure(orderId, "Cancelled by user");
    }

    @Test
    void cancelOrder_alreadyShipped_throwsIllegalStateException() {
        testOrder.setStatus(OrderStatus.SHIPPED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

        assertThatThrownBy(() -> orderService.cancelOrder(orderId, userId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot cancel order");

        verify(sagaMediator, never()).handleFailure(any(), any());
    }

    @Test
    void cancelOrder_alreadyDelivered_throwsIllegalStateException() {
        testOrder.setStatus(OrderStatus.DELIVERED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

        assertThatThrownBy(() -> orderService.cancelOrder(orderId, userId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot cancel order");
    }

    @Test
    void cancelOrder_confirmed_throwsIllegalStateException() {
        testOrder.setStatus(OrderStatus.CONFIRMED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

        assertThatThrownBy(() -> orderService.cancelOrder(orderId, userId))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cancelOrder_wrongUser_throwsIllegalArgumentException() {
        testOrder.setStatus(OrderStatus.CREATED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

        UUID wrongUserId = UUID.randomUUID();

        assertThatThrownBy(() -> orderService.cancelOrder(orderId, wrongUserId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong to user");
    }

    @Test
    void getOrderStatus_returnsStatusString() {
        testOrder.setStatus(OrderStatus.PENDING);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

        String status = orderService.getOrderStatus(orderId);

        assertThat(status).isEqualTo("PENDING");
    }

    @Test
    void getOrdersByUser_returnsList() {
        when(orderRepository.findByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(List.of(testOrder));

        List<OrderDto> results = orderService.getOrdersByUser(userId);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).userId()).isEqualTo(userId);
    }
}
