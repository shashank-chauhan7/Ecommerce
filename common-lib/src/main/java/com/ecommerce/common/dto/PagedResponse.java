package com.ecommerce.common.dto;

import java.util.List;

public record PagedResponse<T>(
        List<T> content,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages,
        boolean last
) {
    public static <T> PagedResponse<T> of(List<T> content, int pageNumber, int pageSize,
                                           long totalElements) {
        int totalPages = (int) Math.ceil((double) totalElements / pageSize);
        boolean last = pageNumber >= totalPages - 1;
        return new PagedResponse<>(content, pageNumber, pageSize, totalElements, totalPages, last);
    }
}
