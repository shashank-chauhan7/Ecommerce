package com.ecommerce.inventory.repository;

import com.ecommerce.inventory.model.InventoryItem;
import com.ecommerce.inventory.model.InventoryKey;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface InventoryRepository extends CassandraRepository<InventoryItem, InventoryKey> {

    List<InventoryItem> findByKeyRegion(String region);

    List<InventoryItem> findByKeyRegionAndKeyProductId(String region, UUID productId);

    @Query("UPDATE inventory_by_region SET available_qty = ?3, reserved_qty = ?4, version = ?5, last_updated = ?6 " +
            "WHERE region = ?0 AND product_id = ?1 AND warehouse_id = ?2 IF version = ?7")
    boolean updateWithLwt(String region, UUID productId, UUID warehouseId,
                          int availableQty, int reservedQty, long newVersion,
                          Instant lastUpdated, long expectedVersion);
}
