package com.ecommerce.order.state;

import com.ecommerce.order.model.OrderStatus;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CreatedState implements OrderState {

    @Override
    public void next(OrderContext context) {
        context.setState(new PendingState());
    }

    @Override
    public void cancel(OrderContext context) {
        log.info("Order {} cancelled from CREATED state", context.getOrder().getId());
        context.setState(new CancelledState());
    }

    @Override
    public OrderStatus getStatus() {
        return OrderStatus.CREATED;
    }
}
