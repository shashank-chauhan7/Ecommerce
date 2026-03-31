package com.ecommerce.order.state;

import com.ecommerce.order.model.OrderStatus;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DeliveredState implements OrderState {

    @Override
    public void next(OrderContext context) {
        log.warn("Order {} is already DELIVERED — terminal state", context.getOrder().getId());
        throw new IllegalStateException("Order is already delivered");
    }

    @Override
    public void cancel(OrderContext context) {
        log.warn("Cannot cancel order {} — already DELIVERED", context.getOrder().getId());
        throw new IllegalStateException("Cannot cancel a delivered order");
    }

    @Override
    public OrderStatus getStatus() {
        return OrderStatus.DELIVERED;
    }
}
