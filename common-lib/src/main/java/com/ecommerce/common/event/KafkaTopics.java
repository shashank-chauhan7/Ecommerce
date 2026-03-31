package com.ecommerce.common.event;

public final class KafkaTopics {

    private KafkaTopics() {}

    public static final String ORDER_EVENTS = "order-events";
    public static final String INVENTORY_EVENTS = "inventory-events";
    public static final String PAYMENT_EVENTS = "payment-events";
    public static final String NOTIFICATION_EVENTS = "notification-events";
}
