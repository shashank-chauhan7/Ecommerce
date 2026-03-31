package com.ecommerce.ai.dto;

import java.time.Instant;
import java.util.List;

public record RecommendationResponse(
        List<ProductRecommendation> recommendations,
        Instant generatedAt
) {
}
