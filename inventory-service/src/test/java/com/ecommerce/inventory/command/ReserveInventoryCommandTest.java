package com.ecommerce.inventory.command;

import com.ecommerce.common.exception.InsufficientStockException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.inventory.model.InventoryItem;
import com.ecommerce.inventory.model.InventoryKey;
import com.ecommerce.inventory.repository.InventoryRepository;
import com.ecommerce.inventory.service.DistributedLockService;
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReserveInventoryCommandTest {

    @Mock
    private InventoryRepository repository;

    @Mock
    private DistributedLockService lockService;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private String region;
    private UUID productId;
    private UUID warehouseId;
    private InventoryItem testItem;

    @BeforeEach
    void setUp() {
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
                .availableQty(50)
                .reservedQty(5)
                .version(1L)
                .lastUpdated(Instant.now())
                .build();
    }

    @Test
    void execute_fullFlow_lockReadUpdatePublish() {
        String lockKey = "inventory:lock:" + region + ":" + productId;
        when(lockService.buildLockKey(region, productId)).thenReturn(lockKey);
        when(lockService.acquireLock(lockKey)).thenReturn(true);
        when(repository.findByKeyRegionAndKeyProductId(region, productId))
                .thenReturn(List.of(testItem));
        when(repository.updateWithLwt(
                eq(region), eq(productId), eq(warehouseId),
                eq(40), eq(15), eq(2L), any(Instant.class), eq(1L)))
                .thenReturn(true);

        ReserveInventoryCommand command = new ReserveInventoryCommand(
                region, productId, 10, "order-100", repository, lockService, kafkaTemplate);

        command.execute();

        verify(lockService).acquireLock(lockKey);
        verify(repository).findByKeyRegionAndKeyProductId(region, productId);
        verify(repository).updateWithLwt(
                eq(region), eq(productId), eq(warehouseId),
                eq(40), eq(15), eq(2L), any(Instant.class), eq(1L));
        verify(kafkaTemplate).send(anyString(), eq("order-100"), any());
        verify(lockService).releaseLock(lockKey);
    }

    @Test
    void execute_insufficientStock_throwsAndReleasesLock() {
        String lockKey = "inventory:lock:" + region + ":" + productId;
        when(lockService.buildLockKey(region, productId)).thenReturn(lockKey);
        when(lockService.acquireLock(lockKey)).thenReturn(true);
        when(repository.findByKeyRegionAndKeyProductId(region, productId))
                .thenReturn(List.of(testItem));

        ReserveInventoryCommand command = new ReserveInventoryCommand(
                region, productId, 100, "order-200", repository, lockService, kafkaTemplate);

        assertThatThrownBy(command::execute)
                .isInstanceOf(InsufficientStockException.class);

        verify(lockService).releaseLock(lockKey);
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    @Test
    void execute_lockFailed_throwsException() {
        String lockKey = "inventory:lock:" + region + ":" + productId;
        when(lockService.buildLockKey(region, productId)).thenReturn(lockKey);
        when(lockService.acquireLock(lockKey)).thenReturn(false);

        ReserveInventoryCommand command = new ReserveInventoryCommand(
                region, productId, 5, "order-300", repository, lockService, kafkaTemplate);

        assertThatThrownBy(command::execute)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Could not acquire lock");
    }

    @Test
    void execute_itemNotFound_throwsResourceNotFoundException() {
        String lockKey = "inventory:lock:" + region + ":" + productId;
        when(lockService.buildLockKey(region, productId)).thenReturn(lockKey);
        when(lockService.acquireLock(lockKey)).thenReturn(true);
        when(repository.findByKeyRegionAndKeyProductId(region, productId))
                .thenReturn(Collections.emptyList());

        ReserveInventoryCommand command = new ReserveInventoryCommand(
                region, productId, 5, "order-400", repository, lockService, kafkaTemplate);

        assertThatThrownBy(command::execute)
                .isInstanceOf(ResourceNotFoundException.class);

        verify(lockService).releaseLock(lockKey);
    }

    @Test
    void undo_compensatingTransaction_releasesStock() {
        String lockKey = "inventory:lock:" + region + ":" + productId;
        when(lockService.buildLockKey(region, productId)).thenReturn(lockKey);
        when(lockService.acquireLock(lockKey)).thenReturn(true);
        when(repository.findByKeyRegionAndKeyProductId(region, productId))
                .thenReturn(List.of(testItem));
        when(repository.updateWithLwt(
                eq(region), eq(productId), eq(warehouseId),
                anyInt(), anyInt(), anyLong(), any(Instant.class), anyLong()))
                .thenReturn(true);

        ReserveInventoryCommand command = new ReserveInventoryCommand(
                region, productId, 10, "order-500", repository, lockService, kafkaTemplate);

        command.execute();

        reset(repository, lockService, kafkaTemplate);

        InventoryItem updatedItem = InventoryItem.builder()
                .key(testItem.getKey())
                .availableQty(40)
                .reservedQty(15)
                .version(2L)
                .lastUpdated(Instant.now())
                .build();

        when(lockService.buildLockKey(region, productId)).thenReturn(lockKey);
        when(lockService.acquireLock(lockKey)).thenReturn(true);
        when(repository.findByKeyRegionAndKeyProductId(region, productId))
                .thenReturn(List.of(updatedItem));
        when(repository.updateWithLwt(
                eq(region), eq(productId), eq(warehouseId),
                eq(50), eq(5), eq(3L), any(Instant.class), eq(2L)))
                .thenReturn(true);

        command.undo();

        verify(repository).updateWithLwt(
                eq(region), eq(productId), eq(warehouseId),
                eq(50), eq(5), eq(3L), any(Instant.class), eq(2L));
        verify(kafkaTemplate).send(anyString(), eq("order-500"), any());
        verify(lockService).releaseLock(lockKey);
    }

    @Test
    void undo_withoutPriorReservation_isNoOp() {
        ReserveInventoryCommand command = new ReserveInventoryCommand(
                region, productId, 10, "order-600", repository, lockService, kafkaTemplate);

        command.undo();

        verify(lockService, never()).acquireLock(anyString());
        verify(repository, never()).findByKeyRegionAndKeyProductId(anyString(), any());
    }
}
