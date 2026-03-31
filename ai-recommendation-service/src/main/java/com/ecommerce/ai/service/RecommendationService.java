package com.ecommerce.ai.service;

import com.ecommerce.ai.dto.ProductRecommendation;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private static final Duration RECOMMENDATION_CACHE_TTL = Duration.ofHours(1);

    private final ChatClient chatClient;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public List<ProductRecommendation> getRecommendations(String userId, String productId, int limit) {
        String cacheKey = "recommendations:product:" + productId + ":user:" + userId + ":limit:" + limit;

        @SuppressWarnings("unchecked")
        List<ProductRecommendation> cached =
                (List<ProductRecommendation>) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("Cache hit for recommendations productId={}, userId={}", productId, userId);
            return cached;
        }

        String browsingHistory = getUserBrowsingHistory(userId);
        String prompt = buildRecommendationPrompt(productId, browsingHistory, limit);

        log.info("Generating AI recommendations for productId={}, userId={}", productId, userId);

        String response = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        List<ProductRecommendation> recommendations = parseRecommendations(response);
        redisTemplate.opsForValue().set(cacheKey, recommendations, RECOMMENDATION_CACHE_TTL);

        return recommendations;
    }

    public List<ProductRecommendation> getUserRecommendations(String userId, int limit) {
        String cacheKey = "recommendations:user:" + userId + ":limit:" + limit;

        @SuppressWarnings("unchecked")
        List<ProductRecommendation> cached =
                (List<ProductRecommendation>) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }

        String browsingHistory = getUserBrowsingHistory(userId);
        String prompt = buildUserRecommendationPrompt(browsingHistory, limit);

        log.info("Generating personalized AI recommendations for userId={}", userId);

        String response = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        List<ProductRecommendation> recommendations = parseRecommendations(response);
        redisTemplate.opsForValue().set(cacheKey, recommendations, RECOMMENDATION_CACHE_TTL);

        return recommendations;
    }

    private String getUserBrowsingHistory(String userId) {
        Object history = redisTemplate.opsForValue().get("user:history:" + userId);
        return history != null ? history.toString() : "No browsing history available";
    }

    private String buildRecommendationPrompt(String productId, String browsingHistory, int limit) {
        return """
                You are an e-commerce product recommendation engine. Based on the following context, \
                recommend %d similar or complementary products from the catalog.

                Current Product ID: %s
                User Browsing History: %s

                Return your response as a JSON array with objects containing:
                - "productId": string
                - "name": string
                - "reason": string (why this product is recommended)
                - "score": number (relevance score between 0.0 and 1.0)

                Return ONLY the JSON array, no other text.
                """.formatted(limit, productId, browsingHistory);
    }

    private String buildUserRecommendationPrompt(String browsingHistory, int limit) {
        return """
                You are an e-commerce product recommendation engine. Based on the user's browsing \
                and purchase history, recommend %d products they might be interested in.

                User History: %s

                Return your response as a JSON array with objects containing:
                - "productId": string
                - "name": string
                - "reason": string (why this product is recommended)
                - "score": number (relevance score between 0.0 and 1.0)

                Return ONLY the JSON array, no other text.
                """.formatted(limit, browsingHistory);
    }

    private List<ProductRecommendation> parseRecommendations(String response) {
        try {
            String json = response.strip();
            if (json.startsWith("```")) {
                json = json.replaceAll("```json?\\s*", "").replaceAll("```\\s*$", "").strip();
            }
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.error("Failed to parse AI recommendation response: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
