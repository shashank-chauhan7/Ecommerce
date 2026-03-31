package com.ecommerce.notification.factory;

import com.ecommerce.notification.bridge.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationFactory {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.from:noreply@ecommerce.com}")
    private String fromAddress;

    public NotificationType createNotification(String type, String channel) {
        NotificationChannel notificationChannel = createChannel(channel);
        return createType(type, notificationChannel);
    }

    private NotificationChannel createChannel(String channel) {
        return switch (channel.toUpperCase()) {
            case "EMAIL" -> new EmailChannel(mailSender, fromAddress);
            case "SMS" -> new SmsChannel();
            case "PUSH" -> new PushChannel();
            default -> throw new IllegalArgumentException("Unknown notification channel: " + channel);
        };
    }

    private NotificationType createType(String type, NotificationChannel channel) {
        return switch (type.toUpperCase()) {
            case "ORDER_CONFIRMATION" -> new OrderConfirmationType(channel);
            case "SHIPPING_UPDATE" -> new ShippingUpdateType(channel);
            case "PROMOTIONAL" -> new PromotionalType(channel);
            default -> throw new IllegalArgumentException("Unknown notification type: " + type);
        };
    }
}
