package com.ecommerce.ai.controller;

import com.ecommerce.ai.dto.ProductRecommendation;
import com.ecommerce.ai.dto.RecommendationResponse;
import com.ecommerce.ai.service.EmbeddingService;
import com.ecommerce.ai.service.RecommendationService;
import com.ecommerce.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
@Tag(name = "Recommendations", description = "AI-powered product recommendation APIs")
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final EmbeddingService embeddingService;

    @GetMapping("/{productId}")
    @Operation(summary = "Get AI recommendations for a product")
    public ResponseEntity<ApiResponse<RecommendationResponse>> getRecommendations(
            @PathVariable String productId,
            @RequestParam(defaultValue = "5") int limit,
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "anonymous") String userId) {
        List<ProductRecommendation> recommendations =
                recommendationService.getRecommendations(userId, productId, limit);
        RecommendationResponse response = new RecommendationResponse(recommendations, Instant.now());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get personalized recommendations for a user")
    public ResponseEntity<ApiResponse<RecommendationResponse>> getUserRecommendations(
            @PathVariable String userId,
            @RequestParam(defaultValue = "10") int limit) {
        List<ProductRecommendation> recommendations =
                recommendationService.getUserRecommendations(userId, limit);
        RecommendationResponse response = new RecommendationResponse(recommendations, Instant.now());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/similar/{productId}")
    @Operation(summary = "Find similar products via embeddings")
    public ResponseEntity<ApiResponse<List<Map.Entry<String, Double>>>> findSimilarProducts(
            @PathVariable String productId,
            @RequestParam(defaultValue = "5") int limit) {
        List<Map.Entry<String, Double>> similar = embeddingService.findSimilarProducts(productId, limit);
        return ResponseEntity.ok(ApiResponse.success(similar));
    }
}
