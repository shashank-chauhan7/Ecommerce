package com.ecommerce.order.state;

import com.ecommerce.order.model.Order;
import com.ecommerce.order.model.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderStateTest {

    private Order order;

    @BeforeEach
    void setUp() {
        order = Order.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .status(OrderStatus.CREATED)
                .totalAmount(BigDecimal.valueOf(100.00))
                .shippingAddress("123 Main St")
                .build();
    }

    @Test
    void fullHappyPath_CREATED_to_DELIVERED() {
        OrderContext context = new OrderContext(order);
        assertThat(context.getCurrentState().getStatus()).isEqualTo(OrderStatus.CREATED);

        context.proceed();
        assertThat(context.getCurrentState().getStatus()).isEqualTo(OrderStatus.PENDING);

        context.proceed();
        assertThat(context.getCurrentState().getStatus()).isEqualTo(OrderStatus.INVENTORY_RESERVED);

        context.proceed();
        assertThat(context.getCurrentState().getStatus()).isEqualTo(OrderStatus.PAYMENT_PROCESSING);

        context.proceed();
        assertThat(context.getCurrentState().getStatus()).isEqualTo(OrderStatus.CONFIRMED);

        context.proceed();
        assertThat(context.getCurrentState().getStatus()).isEqualTo(OrderStatus.SHIPPED);

        context.proceed();
        assertThat(context.getCurrentState().getStatus()).isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    void cancelFromCreated_transitionsToCancelled() {
        OrderContext context = new OrderContext(order);
        assertThat(context.getCurrentState().getStatus()).isEqualTo(OrderStatus.CREATED);

        context.cancel();
        assertThat(context.getCurrentState().getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void cancelFromPending_transitionsToCancelled() {
        order.setStatus(OrderStatus.PENDING);
        OrderContext context = new OrderContext(order);

        context.cancel();
        assertThat(context.getCurrentState().getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void cancelFromInventoryReserved_transitionsToCancelled() {
        order.setStatus(OrderStatus.INVENTORY_RESERVED);
        OrderContext context = new OrderContext(order);

        context.cancel();
        assertThat(context.getCurrentState().getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void cancelFromPaymentProcessing_transitionsToCancelled() {
        order.setStatus(OrderStatus.PAYMENT_PROCESSING);
        OrderContext context = new OrderContext(order);

        context.cancel();
        assertThat(context.getCurrentState().getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void cancelFromConfirmed_throwsIllegalStateException() {
        order.setStatus(OrderStatus.CONFIRMED);
        OrderContext context = new OrderContext(order);

        assertThatThrownBy(context::cancel)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot cancel a confirmed order");
    }

    @Test
    void cancelFromShipped_throwsIllegalStateException() {
        order.setStatus(OrderStatus.SHIPPED);
        OrderContext context = new OrderContext(order);

        assertThatThrownBy(context::cancel)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot cancel a shipped order");
    }

    @Test
    void cancelFromDelivered_throwsIllegalStateException() {
        order.setStatus(OrderStatus.DELIVERED);
        OrderContext context = new OrderContext(order);

        assertThatThrownBy(context::cancel)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot cancel a delivered order");
    }

    @Test
    void proceedFromDelivered_throwsIllegalStateException() {
        order.setStatus(OrderStatus.DELIVERED);
        OrderContext context = new OrderContext(order);

        assertThatThrownBy(context::proceed)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already delivered");
    }

    @Test
    void proceedFromCancelled_throwsIllegalStateException() {
        order.setStatus(OrderStatus.CANCELLED);
        OrderContext context = new OrderContext(order);

        assertThatThrownBy(context::proceed)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot advance a cancelled order");
    }

    @Test
    void cancelFromCancelled_throwsIllegalStateException() {
        order.setStatus(OrderStatus.CANCELLED);
        OrderContext context = new OrderContext(order);

        assertThatThrownBy(context::cancel)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already cancelled");
    }

    @Test
    void contextUpdatesOrderStatus() {
        OrderContext context = new OrderContext(order);

        context.proceed();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);

        context.proceed();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.INVENTORY_RESERVED);
    }

    @Test
    void createdState_hasCorrectStatus() {
        CreatedState state = new CreatedState();
        assertThat(state.getStatus()).isEqualTo(OrderStatus.CREATED);
    }

    @Test
    void pendingState_hasCorrectStatus() {
        PendingState state = new PendingState();
        assertThat(state.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void inventoryReservedState_hasCorrectStatus() {
        InventoryReservedState state = new InventoryReservedState();
        assertThat(state.getStatus()).isEqualTo(OrderStatus.INVENTORY_RESERVED);
    }

    @Test
    void paymentProcessingState_hasCorrectStatus() {
        PaymentProcessingState state = new PaymentProcessingState();
        assertThat(state.getStatus()).isEqualTo(OrderStatus.PAYMENT_PROCESSING);
    }

    @Test
    void confirmedState_hasCorrectStatus() {
        ConfirmedState state = new ConfirmedState();
        assertThat(state.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void shippedState_hasCorrectStatus() {
        ShippedState state = new ShippedState();
        assertThat(state.getStatus()).isEqualTo(OrderStatus.SHIPPED);
    }

    @Test
    void deliveredState_hasCorrectStatus() {
        DeliveredState state = new DeliveredState();
        assertThat(state.getStatus()).isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    void cancelledState_hasCorrectStatus() {
        CancelledState state = new CancelledState();
        assertThat(state.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }
}
