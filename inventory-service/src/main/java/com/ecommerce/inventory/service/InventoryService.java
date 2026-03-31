package com.ecommerce.inventory.service;

import com.ecommerce.inventory.dto.StockDto;

import java.util.List;
import java.util.UUID;

public interface InventoryService {

    void reserveStock(String region, UUID productId, int quantity, String orderId);

    void releaseStock(String region, UUID productId, int quantity, String orderId);

    StockDto getStock(String region, UUID productId);

    void updateStock(String region, UUID productId, int availableQty);

    List<StockDto> getStockByRegion(String region);
}
