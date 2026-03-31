package com.ecommerce.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private static final String EMBEDDING_KEY_PREFIX = "product:embedding:";

    private final EmbeddingModel embeddingModel;
    private final RedisTemplate<String, Object> redisTemplate;

    public List<Double> generateEmbedding(String text) {
        log.debug("Generating embedding for text (length={})", text.length());
        float[] embedding = embeddingModel.embed(text);
        List<Double> result = new ArrayList<>(embedding.length);
        for (float v : embedding) {
            result.add((double) v);
        }
        return result;
    }

    public void storeProductEmbedding(String productId, String productText) {
        List<Double> embedding = generateEmbedding(productText);
        redisTemplate.opsForValue().set(EMBEDDING_KEY_PREFIX + productId, embedding);
        log.info("Stored embedding for productId={}", productId);
    }

    public List<Map.Entry<String, Double>> findSimilarProducts(String productId, int limit) {
        @SuppressWarnings("unchecked")
        List<Double> targetEmbedding =
                (List<Double>) redisTemplate.opsForValue().get(EMBEDDING_KEY_PREFIX + productId);

        if (targetEmbedding == null) {
            log.warn("No embedding found for productId={}", productId);
            return Collections.emptyList();
        }

        Set<String> keys = redisTemplate.keys(EMBEDDING_KEY_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }

        List<Map.Entry<String, Double>> similarities = new ArrayList<>();

        for (String key : keys) {
            String candidateId = key.replace(EMBEDDING_KEY_PREFIX, "");
            if (candidateId.equals(productId)) {
                continue;
            }

            @SuppressWarnings("unchecked")
            List<Double> candidateEmbedding = (List<Double>) redisTemplate.opsForValue().get(key);
            if (candidateEmbedding == null) {
                continue;
            }

            double similarity = cosineSimilarity(targetEmbedding, candidateEmbedding);
            similarities.add(Map.entry(candidateId, similarity));
        }

        similarities.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        return similarities.stream().limit(limit).toList();
    }

    private double cosineSimilarity(List<Double> a, List<Double> b) {
        if (a.size() != b.size()) {
            throw new IllegalArgumentException("Vectors must have the same dimension");
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.size(); i++) {
            dotProduct += a.get(i) * b.get(i);
            normA += a.get(i) * a.get(i);
            normB += b.get(i) * b.get(i);
        }

        double denominator = Math.sqrt(normA) * Math.sqrt(normB);
        return denominator == 0 ? 0.0 : dotProduct / denominator;
    }
}
