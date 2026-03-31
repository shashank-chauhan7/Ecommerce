package com.ecommerce.notification.bridge;

import java.util.Map;

public class OrderConfirmationType extends NotificationType {

    public OrderConfirmationType(NotificationChannel channel) {
        super(channel);
    }

    @Override
    public NotificationMessage buildMessage(Map<String, Object> data) {
        String to = (String) data.get("email");
        String orderId = (String) data.get("orderId");
        String body = data.getOrDefault("htmlBody", buildPlainBody(data)).toString();
        boolean isHtml = data.containsKey("htmlBody");

        return new NotificationMessage(
                to,
                "Order Confirmation - #" + orderId,
                body,
                isHtml
        );
    }

    private String buildPlainBody(Map<String, Object> data) {
        String orderId = (String) data.get("orderId");
        Object total = data.getOrDefault("total", "N/A");
        return String.format(
                "Thank you for your order #%s! Your total is %s. We will notify you when it ships.",
                orderId, total
        );
    }
}
