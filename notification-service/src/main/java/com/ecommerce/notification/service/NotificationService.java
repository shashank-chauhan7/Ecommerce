package com.ecommerce.notification.service;

import java.util.Map;

public interface NotificationService {

    void sendOrderConfirmation(String orderId, String email, Map<String, Object> orderDetails);

    void sendShippingUpdate(String orderId, String email, Map<String, Object> trackingInfo);

    void sendPromotionalEmail(String email, Map<String, Object> promoDetails);
}
