package com.ecommerce.notification.bridge;

public record NotificationMessage(
        String to,
        String subject,
        String body,
        boolean isHtml
) {
}
