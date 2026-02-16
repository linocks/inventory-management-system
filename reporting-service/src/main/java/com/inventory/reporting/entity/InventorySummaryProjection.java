package com.inventory.reporting.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_summary_projection")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventorySummaryProjection {

    @Id
    private Long id;

    @Column(name = "total_products", nullable = false)
    private long totalProducts;

    @Column(name = "total_stock_units", nullable = false)
    private long totalStockUnits;

    @Column(name = "low_stock_products", nullable = false)
    private long lowStockProducts;

    @Column(name = "out_of_stock_products", nullable = false)
    private long outOfStockProducts;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
