package com.ecommerce.search.dto;

public record ProductSearchResult(
        String id,
        String name,
        String description,
        String brand,
        String categoryName,
        Double price,
        String imageUrl,
        float score
) {
}
