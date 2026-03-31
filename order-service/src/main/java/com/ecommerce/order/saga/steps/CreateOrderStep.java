package com.ecommerce.order.saga.steps;

import com.ecommerce.order.model.Order;
import com.ecommerce.order.model.OrderStatus;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.order.state.OrderContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CreateOrderStep extends AbstractSagaStep<Order> {

    private final OrderRepository orderRepository;

    @Override
    protected void validate(Order order) {
        if (order.getItems() == null || order.getItems().isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item");
        }
        if (order.getUserId() == null) {
            throw new IllegalArgumentException("User ID is required");
        }
    }

    @Override
    protected Order execute(Order order) {
        order.setStatus(OrderStatus.CREATED);
        Order saved = orderRepository.save(order);

        OrderContext context = new OrderContext(saved);
        context.proceed();
        saved.setStatus(context.getCurrentState().getStatus());

        return orderRepository.save(saved);
    }

    @Override
    protected void onSuccess(Order order) {
        log.info("Order {} created successfully in PENDING state", order.getId());
    }

    @Override
    protected void onFailure(Order order, Exception ex) {
        log.error("Failed to create order for user {}: {}", order.getUserId(), ex.getMessage());
    }
}
