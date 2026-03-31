package com.ecommerce.inventory.command;

import com.ecommerce.common.event.DomainEvent;
import com.ecommerce.common.event.EventTypes;
import com.ecommerce.common.event.KafkaTopics;
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
public class ReleaseInventoryCommand implements InventoryCommand {

    private final String region;
    private final UUID productId;
    private final int quantity;
    private final String orderId;
    private final InventoryRepository repository;
    private final DistributedLockService lockService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public ReleaseInventoryCommand(String region, UUID productId, int quantity, String orderId,
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
            throw new IllegalStateException("Could not acquire lock for release: " + lockKey);
        }

        try {
            List<InventoryItem> items = repository.findByKeyRegionAndKeyProductId(region, productId);
            if (items.isEmpty()) {
                throw new ResourceNotFoundException("InventoryItem", "productId", productId);
            }

            InventoryItem item = items.getFirst();
            int newAvailable = item.getAvailableQty() + quantity;
            int newReserved = Math.max(0, item.getReservedQty() - quantity);
            long newVersion = item.getVersion() + 1;
            Instant now = Instant.now();

            boolean applied = repository.updateWithLwt(
                    region, productId, item.getKey().getWarehouseId(),
                    newAvailable, newReserved, newVersion, now, item.getVersion());

            if (!applied) {
                throw new IllegalStateException("Concurrent modification during release for: " + productId);
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
            log.info("Inventory released: product={}, qty={}, order={}", productId, quantity, orderId);

        } finally {
            lockService.releaseLock(lockKey);
        }
    }

    @Override
    public void undo() {
        log.info("Release undo is a no-op for order={}, product={}", orderId, productId);
    }
}
