package com.ecommerce.order.state;

import com.ecommerce.order.model.OrderStatus;

public interface OrderState {

    void next(OrderContext context);

    void cancel(OrderContext context);

    OrderStatus getStatus();
}
