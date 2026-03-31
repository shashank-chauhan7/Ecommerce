package com.ecommerce.notification.bridge;

/**
 * Bridge pattern - Abstraction interface.
 * Represents a delivery channel (email, SMS, push) independent of notification type.
 */
public interface NotificationChannel {

    void send(NotificationMessage message);
}
