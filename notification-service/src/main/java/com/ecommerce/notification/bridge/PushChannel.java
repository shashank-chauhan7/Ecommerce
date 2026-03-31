package com.ecommerce.notification.bridge;

import lombok.extern.slf4j.Slf4j;

/**
 * Stub push notification channel. Replace with Firebase Cloud Messaging or similar.
 */
@Slf4j
public class PushChannel implements NotificationChannel {

    @Override
    public void send(NotificationMessage message) {
        log.info("[PUSH STUB] Sending push notification to={}, subject={}, body={}",
                message.to(), message.subject(), message.body());
    }
}
