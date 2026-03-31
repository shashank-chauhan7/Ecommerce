package com.ecommerce.inventory.command;

import com.ecommerce.common.event.DomainEvent;
import com.ecommerce.common.event.EventTypes;
import com.ecommerce.common.event.KafkaTopics;
import com.ecommerce.common.exception.InsufficientStockException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.inventory.model.InventoryItem;
import com.ecommerce.inventory.repository.InventoryRepository;
import com.ecommerce.inventory.service.DistributedLockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
public class ReserveInventoryCommand implements InventoryCommand {

    private final String region;
    private final UUID productId;
    private final int quantity;
    private final String orderId;
    private final InventoryRepository repository;
    private final DistributedLockService lockService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private InventoryItem reservedItem;

    public ReserveInventoryCommand(String region, UUID productId, int quantity, String orderId,
                                   InventoryRepository repository, DistributedLockService lockService,
                                   KafkaTemplate<String, Object> kafkaTemplate) {
        this.region = region;
        this.productId = productId;
        this.quantity = quantity;
        this.orderId = orderId;
        this.repository = repository;
        this.lockService = lockService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void execute() {
        String lockKey = lockService.buildLockKey(region, productId);
        boolean locked = lockService.acquireLock(lockKey);

        if (!locked) {
            throw new IllegalStateException("Could not acquire lock for inventory: " + lockKey);
        }

        try {
            List<InventoryItem> items = repository.findByKeyRegionAndKeyProductId(region, productId);
            if (items.isEmpty()) {
                throw new ResourceNotFoundException("InventoryItem", "productId", productId);
            }

            InventoryItem item = items.getFirst();

            if (item.getAvailableQty() < quantity) {
                throw new InsufficientStockException(
                        productId.toString(), quantity, item.getAvailableQty());
            }

            int newAvailable = item.getAvailableQty() - quantity;
            int newReserved = item.getReservedQty() + quantity;
            long newVersion = item.getVersion() + 1;
            Instant now = Instant.now();

            boolean applied = repository.updateWithLwt(
                    region, productId, item.getKey().getWarehouseId(),
                    newAvailable, newReserved, newVersion, now, item.getVersion());

            if (!applied) {
                throw new IllegalStateException("Concurrent modification detected for inventory: " + productId);
            }

            item.setAvailableQty(newAvailable);
            item.setReservedQty(newReserved);
            item.setVersion(newVersion);
            item.setLastUpdated(now);
            this.reservedItem = item;

            DomainEvent<Map<String, Object>> event = DomainEvent.create(
                    EventTypes.INVENTORY_RESERVED,
                    orderId,
                    Map.of(
                            "orderId", orderId,
                            "productId", productId.toString(),
                            "region", region,
                            "quantity", quantity
                    )
            );
            kafkaTemplate.send(KafkaTopics.INVENTORY_EVENTS, orderId, event);
            log.info("Inventory reserved: product={}, qty={}, order={}", productId, quantity, orderId);

        } finally {
            lockService.releaseLock(lockKey);
        }
    }

    @Override
    public void undo() {
        if (reservedItem == null) {
            log.warn("No reservation to undo for order={}, product={}", orderId, productId);
            return;
        }

        String lockKey = lockService.buildLockKey(region, productId);
        boolean locked = lockService.acquireLock(lockKey);

        if (!locked) {
            throw new IllegalStateException("Could not acquire lock for undo: " + lockKey);
        }

        try {
            List<InventoryItem> items = repository.findByKeyRegionAndKeyProductId(region, productId);
            if (items.isEmpty()) {
                log.error("Cannot undo reservation - item not found: product={}", productId);
                return;
            }

            InventoryItem current = items.getFirst();
            int newAvailable = current.getAvailableQty() + quantity;
            int newReserved = Math.max(0, current.getReservedQty() - quantity);
            long newVersion = current.getVersion() + 1;
            Instant now = Instant.now();

            boolean applied = repository.updateWithLwt(
                    region, productId, current.getKey().getWarehouseId(),
                    newAvailable, newReserved, newVersion, now, current.getVersion());

            if (!applied) {
                throw new IllegalStateException("Concurrent modification during undo for: " + productId);
            }

            DomainEvent<Map<String, Object>> event = DomainEvent.create(
                    EventTypes.INVENTORY_RELEASED,
                    orderId,
                    Map.of(
                            "orderId", orderId,
                            "productId", productId.toString(),
                            "region", region,
                            "quantity", quantity
                    )
            );
            kafkaTemplate.send(KafkaTopics.INVENTORY_EVENTS, orderId, event);
            log.info("Inventory reservation undone: product={}, qty={}, order={}", productId, quantity, orderId);

        } finally {
            lockService.releaseLock(lockKey);
        }
    }
}
