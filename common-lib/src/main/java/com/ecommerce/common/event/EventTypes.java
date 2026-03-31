package com.ecommerce.common.event;

public final class EventTypes {

    private EventTypes() {}

    // Order events
    public static final String ORDER_CREATED = "ORDER_CREATED";
    public static final String ORDER_CONFIRMED = "ORDER_CONFIRMED";
    public static final String ORDER_CANCELLED = "ORDER_CANCELLED";
    public static final String ORDER_SHIPPED = "ORDER_SHIPPED";
    public static final String ORDER_DELIVERED = "ORDER_DELIVERED";

    // Inventory events
    public static final String INVENTORY_RESERVED = "INVENTORY_RESERVED";
    public static final String INVENTORY_RELEASED = "INVENTORY_RELEASED";
    public static final String INVENTORY_INSUFFICIENT = "INVENTORY_INSUFFICIENT";
    public static final String RESERVATION_FAILED = "RESERVATION_FAILED";

    // Payment events
    public static final String PAYMENT_COMPLETED = "PAYMENT_COMPLETED";
    public static final String PAYMENT_FAILED = "PAYMENT_FAILED";
    public static final String PAYMENT_REFUNDED = "PAYMENT_REFUNDED";

    // Notification events
    public static final String SEND_EMAIL = "SEND_EMAIL";
    public static final String SEND_SMS = "SEND_SMS";
    public static final String SEND_PUSH = "SEND_PUSH";
}
