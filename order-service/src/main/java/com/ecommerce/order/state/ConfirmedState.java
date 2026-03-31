package com.ecommerce.order.state;

import com.ecommerce.order.model.OrderStatus;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConfirmedState implements OrderState {

    @Override
    public void next(OrderContext context) {
        context.setState(new ShippedState());
    }

    @Override
    public void cancel(OrderContext context) {
        log.warn("Cannot cancel order {} — already CONFIRMED", context.getOrder().getId());
        throw new IllegalStateException("Cannot cancel a confirmed order");
    }

    @Override
    public OrderStatus getStatus() {
        return OrderStatus.CONFIRMED;
    }
}
