package com.ecommerce.notification.service;

import com.ecommerce.notification.bridge.NotificationType;
import com.ecommerce.notification.factory.NotificationFactory;
import com.ecommerce.notification.template.EmailTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationFactory notificationFactory;
    private final EmailTemplateService emailTemplateService;

    @Override
    public void sendOrderConfirmation(String orderId, String email, Map<String, Object> orderDetails) {
        log.info("Sending order confirmation for orderId={} to={}", orderId, email);

        Map<String, Object> data = new HashMap<>(orderDetails);
        data.put("orderId", orderId);
        data.put("email", email);

        String htmlBody = emailTemplateService.renderOrderConfirmation(data);
        data.put("htmlBody", htmlBody);

        NotificationType notification = notificationFactory.createNotification("ORDER_CONFIRMATION", "EMAIL");
        notification.notify(data);

        log.info("Order confirmation sent for orderId={}", orderId);
    }

    @Override
    public void sendShippingUpdate(String orderId, String email, Map<String, Object> trackingInfo) {
        log.info("Sending shipping update for orderId={} to={}", orderId, email);

        Map<String, Object> data = new HashMap<>(trackingInfo);
        data.put("orderId", orderId);
        data.put("email", email);

        String htmlBody = emailTemplateService.renderShippingUpdate(data);
        data.put("htmlBody", htmlBody);

        NotificationType notification = notificationFactory.createNotification("SHIPPING_UPDATE", "EMAIL");
        notification.notify(data);

        log.info("Shipping update sent for orderId={}", orderId);
    }

    @Override
    public void sendPromotionalEmail(String email, Map<String, Object> promoDetails) {
        log.info("Sending promotional email to={}", email);

        Map<String, Object> data = new HashMap<>(promoDetails);
        data.put("email", email);

        NotificationType notification = notificationFactory.createNotification("PROMOTIONAL", "EMAIL");
        notification.notify(data);

        log.info("Promotional email sent to={}", email);
    }
}
