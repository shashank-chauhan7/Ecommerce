package com.ecommerce.order.model;

public enum OrderStatus {
    CREATED,
    PENDING,
    INVENTORY_RESERVED,
    PAYMENT_PROCESSING,
    CONFIRMED,
    SHIPPED,
    DELIVERED,
    CANCELLED
}
