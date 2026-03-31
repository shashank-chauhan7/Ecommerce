package com.ecommerce.order.controller;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.dto.OrderDto;
import com.ecommerce.order.facade.OrderFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order management endpoints")
public class OrderController {

    private final OrderFacade orderFacade;

    @PostMapping
    @Operation(summary = "Place a new order")
    public ResponseEntity<ApiResponse<OrderDto>> placeOrder(
            @RequestHeader("X-User-Id") String userIdHeader,
            @Valid @RequestBody CreateOrderRequest request) {
        UUID userId = UUID.fromString(userIdHeader);
        OrderDto order = orderFacade.placeOrder(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order placed successfully", order));
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get order by ID")
    public ResponseEntity<ApiResponse<OrderDto>> getOrder(
            @RequestHeader("X-User-Id") String userIdHeader,
            @Parameter(description = "Order ID") @PathVariable UUID orderId) {
        UUID userId = UUID.fromString(userIdHeader);
        OrderDto order = orderFacade.getOrderStatus(orderId, userId);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get all orders for a user")
    public ResponseEntity<ApiResponse<List<OrderDto>>> getUserOrders(
            @RequestHeader("X-User-Id") String userIdHeader,
            @Parameter(description = "User ID") @PathVariable UUID userId) {
        List<OrderDto> orders = orderFacade.getUserOrders(userId);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @PutMapping("/{orderId}/cancel")
    @Operation(summary = "Cancel an order")
    public ResponseEntity<ApiResponse<OrderDto>> cancelOrder(
            @RequestHeader("X-User-Id") String userIdHeader,
            @Parameter(description = "Order ID") @PathVariable UUID orderId) {
        UUID userId = UUID.fromString(userIdHeader);
        OrderDto order = orderFacade.cancelOrder(orderId, userId);
        return ResponseEntity.ok(ApiResponse.success("Order cancelled", order));
    }

    @GetMapping("/{orderId}/status")
    @Operation(summary = "Get order status")
    public ResponseEntity<ApiResponse<String>> getOrderStatus(
            @RequestHeader("X-User-Id") String userIdHeader,
            @Parameter(description = "Order ID") @PathVariable UUID orderId) {
        OrderDto order = orderFacade.getOrderStatus(orderId, UUID.fromString(userIdHeader));
        return ResponseEntity.ok(ApiResponse.success(order.status().name()));
    }
}
