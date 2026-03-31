package com.ecommerce.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateProductRequest(
        @NotBlank(message = "Product name is required")
        String name,

        String description,

        String sku,

        String brand,

        @NotNull(message = "Base price is required")
        @Positive(message = "Base price must be positive")
        BigDecimal basePrice,

        UUID categoryId
) {}
