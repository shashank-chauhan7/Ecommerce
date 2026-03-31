package com.ecommerce.inventory.service;

import com.ecommerce.common.exception.InsufficientStockException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.inventory.dto.StockDto;
import com.ecommerce.inventory.model.InventoryItem;
import com.ecommerce.inventory.model.InventoryKey;
import com.ecommerce.inventory.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository repository;

    @Mock
    private DistributedLockService lockService;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private InventoryServiceImpl inventoryService;

    private String region;
    private UUID productId;
    private UUID warehouseId;
    private InventoryItem testItem;

    @BeforeEach
    void setUp() {
        inventoryService = new InventoryServiceImpl(repository, lockService, kafkaTemplate);

        region = "us-east-1";
        productId = UUID.randomUUID();
        warehouseId = UUID.randomUUID();

        InventoryKey key = InventoryKey.builder()
                .region(region)
                .productId(productId)
                .warehouseId(warehouseId)
                .build();

        testItem = InventoryItem.builder()
                .key(key)
                .availableQty(100)
                .reservedQty(10)
                .version(1L)
                .lastUpdated(Instant.now())
                .build();
    }

    @Test
    void reserveStock_success() {
        String lockKey = "inventory:lock:" + region + ":" + productId;
        when(lockService.buildLockKey(region, productId)).thenReturn(lockKey);
        when(lockService.acquireLock(lockKey)).thenReturn(true);
        when(repository.findByKeyRegionAndKeyProductId(region, productId))
                .thenReturn(List.of(testItem));
        when(repository.updateWithLwt(
                eq(region), eq(productId), eq(warehouseId),
                anyInt(), anyInt(), anyLong(), any(Instant.class), eq(1L)))
                .thenReturn(true);

        inventoryService.reserveStock(region, productId, 5, "order-123");

        verify(lockService).acquireLock(lockKey);
        verify(lockService).releaseLock(lockKey);
        verify(kafkaTemplate).send(anyString(), anyString(), any());
    }

    @Test
    void reserveStock_insufficientStock_throwsException() {
        String lockKey = "inventory:lock:" + region + ":" + productId;
        when(lockService.buildLockKey(region, productId)).thenReturn(lockKey);
        when(lockService.acquireLock(lockKey)).thenReturn(true);
        when(repository.findByKeyRegionAndKeyProductId(region, productId))
                .thenReturn(List.of(testItem));

        assertThatThrownBy(() -> inventoryService.reserveStock(region, productId, 200, "order-456"))
                .isInstanceOf(InsufficientStockException.class);

        verify(lockService).releaseLock(lockKey);
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    @Test
    void reserveStock_lockFailed_throwsRuntimeException() {
        String lockKey = "inventory:lock:" + region + ":" + productId;
        when(lockService.buildLockKey(region, productId)).thenReturn(lockKey);
        when(lockService.acquireLock(lockKey)).thenReturn(false);

        assertThatThrownBy(() -> inventoryService.reserveStock(region, productId, 5, "order-789"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Could not acquire lock");
    }

    @Test
    void releaseStock_success() {
        String lockKey = "inventory:lock:" + region + ":" + productId;
        when(lockService.buildLockKey(region, productId)).thenReturn(lockKey);
        when(lockService.acquireLock(lockKey)).thenReturn(true);
        when(repository.findByKeyRegionAndKeyProductId(region, productId))
                .thenReturn(List.of(testItem));
        when(repository.updateWithLwt(
                eq(region), eq(productId), eq(warehouseId),
                anyInt(), anyInt(), anyLong(), any(Instant.class), eq(1L)))
                .thenReturn(true);

        inventoryService.releaseStock(region, productId, 5, "order-123");

        verify(lockService).acquireLock(lockKey);
        verify(lockService).releaseLock(lockKey);
        verify(kafkaTemplate).send(anyString(), anyString(), any());
    }

    @Test
    void getStock_found() {
        when(repository.findByKeyRegionAndKeyProductId(region, productId))
                .thenReturn(List.of(testItem));

        StockDto result = inventoryService.getStock(region, productId);

        assertThat(result.region()).isEqualTo(region);
        assertThat(result.productId()).isEqualTo(productId);
        assertThat(result.availableQty()).isEqualTo(100);
        assertThat(result.reservedQty()).isEqualTo(10);
        assertThat(result.version()).isEqualTo(1L);
    }

    @Test
    void getStock_notFound_throwsResourceNotFoundException() {
        when(repository.findByKeyRegionAndKeyProductId(region, productId))
                .thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> inventoryService.getStock(region, productId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateStock_success() {
        when(repository.findByKeyRegionAndKeyProductId(region, productId))
                .thenReturn(List.of(testItem));
        when(repository.updateWithLwt(
                eq(region), eq(productId), eq(warehouseId),
                eq(150), eq(10), eq(2L), any(Instant.class), eq(1L)))
                .thenReturn(true);

        inventoryService.updateStock(region, productId, 150);

        verify(repository).updateWithLwt(
                eq(region), eq(productId), eq(warehouseId),
                eq(150), eq(10), eq(2L), any(Instant.class), eq(1L));
    }

    @Test
    void updateStock_concurrentModification_throwsException() {
        when(repository.findByKeyRegionAndKeyProductId(region, productId))
                .thenReturn(List.of(testItem));
        when(repository.updateWithLwt(
                eq(region), eq(productId), eq(warehouseId),
                anyInt(), anyInt(), anyLong(), any(Instant.class), eq(1L)))
                .thenReturn(false);

        assertThatThrownBy(() -> inventoryService.updateStock(region, productId, 150))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Concurrent modification");
    }

    @Test
    void getStockByRegion_returnsList() {
        when(repository.findByKeyRegion(region)).thenReturn(List.of(testItem));

        List<StockDto> results = inventoryService.getStockByRegion(region);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).region()).isEqualTo(region);
    }
}
