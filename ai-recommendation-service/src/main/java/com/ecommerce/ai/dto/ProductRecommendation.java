package com.ecommerce.ai.dto;

public record ProductRecommendation(
        String productId,
        String name,
        String reason,
        double score
) {
}
