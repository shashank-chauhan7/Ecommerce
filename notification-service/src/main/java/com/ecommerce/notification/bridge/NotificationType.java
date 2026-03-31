package com.ecommerce.notification.bridge;

import java.util.Map;

/**
 * Bridge pattern - Implementor.
 * Each subclass defines how to build a notification message for a specific business scenario,
 * while the channel (email, SMS, push) is injected via the bridge.
 */
public abstract class NotificationType {

    protected final NotificationChannel channel;

    protected NotificationType(NotificationChannel channel) {
        this.channel = channel;
    }

    public abstract NotificationMessage buildMessage(Map<String, Object> data);

    public void notify(Map<String, Object> data) {
        NotificationMessage message = buildMessage(data);
        channel.send(message);
    }
}
