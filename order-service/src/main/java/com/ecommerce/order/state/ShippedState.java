package com.ecommerce.order.state;

import com.ecommerce.order.model.OrderStatus;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ShippedState implements OrderState {

    @Override
    public void next(OrderContext context) {
        context.setState(new DeliveredState());
    }

    @Override
    public void cancel(OrderContext context) {
        log.warn("Cannot cancel order {} — already SHIPPED", context.getOrder().getId());
        throw new IllegalStateException("Cannot cancel a shipped order");
    }

    @Override
    public OrderStatus getStatus() {
        return OrderStatus.SHIPPED;
    }
}
