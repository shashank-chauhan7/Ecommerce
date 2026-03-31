package com.ecommerce.payment.controller;

import com.ecommerce.payment.dto.PaymentDto;
import com.ecommerce.payment.dto.ProcessPaymentRequest;
import com.ecommerce.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment processing endpoints")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @Operation(summary = "Process a payment", description = "Initiates payment processing for an order (idempotent)")
    public ResponseEntity<Map<String, Object>> processPayment(@Valid @RequestBody ProcessPaymentRequest request) {
        PaymentDto payment = paymentService.processPayment(
                request.orderId(),
                request.userId(),
                request.amount(),
                request.currency(),
                request.customerEmail()
        );
        return ResponseEntity.ok(apiResponse(true, "Payment processed", payment));
    }

    @GetMapping("/{paymentId}")
    @Operation(summary = "Get payment by ID")
    public ResponseEntity<Map<String, Object>> getPayment(@PathVariable UUID paymentId) {
        PaymentDto payment = paymentService.getPayment(paymentId);
        return ResponseEntity.ok(apiResponse(true, "Payment retrieved", payment));
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get payment by order ID")
    public ResponseEntity<Map<String, Object>> getPaymentByOrderId(@PathVariable UUID orderId) {
        PaymentDto payment = paymentService.getPaymentByOrderId(orderId);
        return ResponseEntity.ok(apiResponse(true, "Payment retrieved", payment));
    }

    @PostMapping("/{paymentId}/refund")
    @Operation(summary = "Refund a payment", description = "Initiates a full refund for a completed payment")
    public ResponseEntity<Map<String, Object>> refundPayment(@PathVariable UUID paymentId) {
        PaymentDto payment = paymentService.refundPayment(paymentId);
        return ResponseEntity.ok(apiResponse(true, "Refund processed", payment));
    }

    private Map<String, Object> apiResponse(boolean success, String message, Object data) {
        return Map.of(
                "success", success,
                "message", message,
                "data", data
        );
    }
}
