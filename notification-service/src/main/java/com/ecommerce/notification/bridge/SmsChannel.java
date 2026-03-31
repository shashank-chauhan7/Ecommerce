package com.ecommerce.notification.bridge;

import lombok.extern.slf4j.Slf4j;

/**
 * Stub SMS channel. Replace with Twilio or another SMS provider integration.
 */
@Slf4j
public class SmsChannel implements NotificationChannel {

    @Override
    public void send(NotificationMessage message) {
        log.info("[SMS STUB] Sending SMS to={}, body={}", message.to(), message.body());
    }
}
