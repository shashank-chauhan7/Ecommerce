package com.ecommerce.inventory.dto;

import java.time.Instant;
import java.util.UUID;

public record StockDto(
        String region,
        UUID productId,
        UUID warehouseId,
        int availableQty,
        int reservedQty,
        long version,
        Instant lastUpdated
) {}
