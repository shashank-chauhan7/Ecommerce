package com.ecommerce.order.state;

import com.ecommerce.order.model.OrderStatus;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InventoryReservedState implements OrderState {

    @Override
    public void next(OrderContext context) {
        context.setState(new PaymentProcessingState());
    }

    @Override
    public void cancel(OrderContext context) {
        log.info("Order {} cancelled from INVENTORY_RESERVED state — inventory release required",
                context.getOrder().getId());
        context.setState(new CancelledState());
    }

    @Override
    public OrderStatus getStatus() {
        return OrderStatus.INVENTORY_RESERVED;
    }
}
