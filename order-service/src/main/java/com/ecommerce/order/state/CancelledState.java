package com.ecommerce.order.state;

import com.ecommerce.order.model.OrderStatus;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CancelledState implements OrderState {

    @Override
    public void next(OrderContext context) {
        log.warn("Order {} is CANCELLED — terminal state", context.getOrder().getId());
        throw new IllegalStateException("Cannot advance a cancelled order");
    }

    @Override
    public void cancel(OrderContext context) {
        log.warn("Order {} is already CANCELLED", context.getOrder().getId());
        throw new IllegalStateException("Order is already cancelled");
    }

    @Override
    public OrderStatus getStatus() {
        return OrderStatus.CANCELLED;
    }
}
