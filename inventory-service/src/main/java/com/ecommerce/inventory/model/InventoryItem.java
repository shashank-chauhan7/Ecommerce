package com.ecommerce.inventory.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("inventory_by_region")
public class InventoryItem {

    @PrimaryKey
    private InventoryKey key;

    @Column("available_qty")
    private int availableQty;

    @Column("reserved_qty")
    private int reservedQty;

    @Column("last_updated")
    private Instant lastUpdated;

    @Column("version")
    private long version;
}
