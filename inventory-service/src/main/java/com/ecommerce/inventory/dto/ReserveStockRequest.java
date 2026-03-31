package com.ecommerce.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ReserveStockRequest(
        @NotBlank(message = "Region is required")
        String region,

        @NotNull(message = "Product ID is required")
        UUID productId,

        @Min(value = 1, message = "Quantity must be at least 1")
        int quantity,

        @NotBlank(message = "Order ID is required")
        String orderId
) {}
