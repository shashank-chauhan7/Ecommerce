package com.ecommerce.notification.controller;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Notification management APIs (mostly Kafka-driven)")
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/test/email")
    @Operation(summary = "Send test email", description = "Send a test email for verification purposes")
    public ResponseEntity<ApiResponse<String>> sendTestEmail(@RequestBody Map<String, Object> request) {
        log.info("Sending test email with payload: {}", request);

        String email = (String) request.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Email address is required"));
        }

        String type = (String) request.getOrDefault("type", "ORDER_CONFIRMATION");

        switch (type.toUpperCase()) {
            case "ORDER_CONFIRMATION" -> {
                request.putIfAbsent("orderId", "TEST-001");
                request.putIfAbsent("total", "$99.99");
                notificationService.sendOrderConfirmation(
                        (String) request.get("orderId"), email, request);
            }
            case "SHIPPING_UPDATE" -> {
                request.putIfAbsent("orderId", "TEST-001");
                request.putIfAbsent("trackingNumber", "TRACK-123456");
                request.putIfAbsent("carrier", "FedEx");
                notificationService.sendShippingUpdate(
                        (String) request.get("orderId"), email, request);
            }
            case "PROMOTIONAL" -> notificationService.sendPromotionalEmail(email, request);
            default -> {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Unknown notification type: " + type));
            }
        }

        return ResponseEntity.ok(ApiResponse.success("Test email sent", "Notification sent to " + email));
    }
}
