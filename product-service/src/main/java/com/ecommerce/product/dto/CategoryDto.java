package com.ecommerce.product.dto;

import java.util.List;
import java.util.UUID;

public record CategoryDto(
        UUID id,
        String name,
        String description,
        UUID parentId,
        List<CategoryDto> children
) {}
