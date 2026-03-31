package com.ecommerce.inventory.service;

import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.inventory.command.ReleaseInventoryCommand;
import com.ecommerce.inventory.command.ReserveInventoryCommand;
import com.ecommerce.inventory.dto.StockDto;
import com.ecommerce.inventory.model.InventoryItem;
import com.ecommerce.inventory.model.InventoryKey;
import com.ecommerce.inventory.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository repository;
    private final DistributedLockService lockService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void reserveStock(String region, UUID productId, int quantity, String orderId) {
        ReserveInventoryCommand command = new ReserveInventoryCommand(
                region, productId, quantity, orderId, repository, lockService, kafkaTemplate);
        command.execute();
    }

    @Override
    public void releaseStock(String region, UUID productId, int quantity, String orderId) {
        ReleaseInventoryCommand command = new ReleaseInventoryCommand(
                region, productId, quantity, orderId, repository, lockService, kafkaTemplate);
        command.execute();
    }

    @Override
    public StockDto getStock(String region, UUID productId) {
        List<InventoryItem> items = repository.findByKeyRegionAndKeyProductId(region, productId);
        if (items.isEmpty()) {
            throw new ResourceNotFoundException("InventoryItem", "productId", productId);
        }
        return toDto(items.getFirst());
    }

    @Override
    public void updateStock(String region, UUID productId, int availableQty) {
        List<InventoryItem> items = repository.findByKeyRegionAndKeyProductId(region, productId);
        if (items.isEmpty()) {
            throw new ResourceNotFoundException("InventoryItem", "productId", productId);
        }

        InventoryItem item = items.getFirst();
        long newVersion = item.getVersion() + 1;
        Instant now = Instant.now();

        boolean applied = repository.updateWithLwt(
                region, productId, item.getKey().getWarehouseId(),
                availableQty, item.getReservedQty(), newVersion, now, item.getVersion());

        if (!applied) {
            throw new IllegalStateException("Concurrent modification detected while updating stock for: " + productId);
        }

        log.info("Stock updated: product={}, newAvailable={}", productId, availableQty);
    }

    @Override
    public List<StockDto> getStockByRegion(String region) {
        return repository.findByKeyRegion(region).stream()
                .map(this::toDto)
                .toList();
    }

    private StockDto toDto(InventoryItem item) {
        InventoryKey key = item.getKey();
        return new StockDto(
                key.getRegion(),
                key.getProductId(),
                key.getWarehouseId(),
                item.getAvailableQty(),
                item.getReservedQty(),
                item.getVersion(),
                item.getLastUpdated()
        );
    }
}
