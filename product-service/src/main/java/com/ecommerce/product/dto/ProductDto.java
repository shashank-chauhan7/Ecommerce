package com.ecommerce.product.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProductDto(
        UUID id,
        String name,
        String description,
        String sku,
        String brand,
        BigDecimal basePrice,
        BigDecimal currentPrice,
        UUID categoryId,
        String categoryName,
        List<String> imageUrls,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {}
