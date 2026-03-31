package com.ecommerce.notification.bridge;

import java.util.Map;

public class ShippingUpdateType extends NotificationType {

    public ShippingUpdateType(NotificationChannel channel) {
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
                "Shipping Update - Order #" + orderId,
                body,
                isHtml
        );
    }

    private String buildPlainBody(Map<String, Object> data) {
        String orderId = (String) data.get("orderId");
        String trackingNumber = (String) data.getOrDefault("trackingNumber", "N/A");
        String carrier = (String) data.getOrDefault("carrier", "N/A");
        return String.format(
                "Your order #%s has been shipped! Tracking: %s via %s",
                orderId, trackingNumber, carrier
        );
    }
}
