package com.ecommerce.search.dto;

public record SearchRequest(
        String query,
        int page,
        int size
) {
}
