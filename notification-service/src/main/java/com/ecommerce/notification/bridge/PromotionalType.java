package com.ecommerce.notification.bridge;

import java.util.Map;

public class PromotionalType extends NotificationType {

    public PromotionalType(NotificationChannel channel) {
        super(channel);
    }

    @Override
    public NotificationMessage buildMessage(Map<String, Object> data) {
        String to = (String) data.get("email");
        String promoTitle = (String) data.getOrDefault("promoTitle", "Special Offer");
        String body = data.getOrDefault("htmlBody", buildPlainBody(data)).toString();
        boolean isHtml = data.containsKey("htmlBody");

        return new NotificationMessage(
                to,
                promoTitle,
                body,
                isHtml
        );
    }

    private String buildPlainBody(Map<String, Object> data) {
        String promoTitle = (String) data.getOrDefault("promoTitle", "Special Offer");
        String promoCode = (String) data.getOrDefault("promoCode", "");
        String discount = (String) data.getOrDefault("discount", "");
        return String.format(
                "%s! Use code %s to get %s off your next purchase.",
                promoTitle, promoCode, discount
        );
    }
}
