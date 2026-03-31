package com.ecommerce.order.state;

import com.ecommerce.order.model.Order;
import com.ecommerce.order.model.OrderStatus;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class OrderContext {

    private OrderState currentState;
    private final Order order;

    public OrderContext(Order order) {
        this.order = order;
        this.currentState = resolveState(order.getStatus());
    }

    public void setState(OrderState state) {
        log.info("Order {} transitioning from {} to {}", order.getId(), currentState.getStatus(), state.getStatus());
        this.currentState = state;
        this.order.setStatus(state.getStatus());
    }

    public void proceed() {
        currentState.next(this);
    }

    public void cancel() {
        currentState.cancel(this);
    }

    private OrderState resolveState(OrderStatus status) {
        return switch (status) {
            case CREATED -> new CreatedState();
            case PENDING -> new PendingState();
            case INVENTORY_RESERVED -> new InventoryReservedState();
            case PAYMENT_PROCESSING -> new PaymentProcessingState();
            case CONFIRMED -> new ConfirmedState();
            case SHIPPED -> new ShippedState();
            case DELIVERED -> new DeliveredState();
            case CANCELLED -> new CancelledState();
        };
    }
}
