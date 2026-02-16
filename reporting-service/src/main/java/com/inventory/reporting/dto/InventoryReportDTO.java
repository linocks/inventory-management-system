package com.inventory.reporting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Aggregated inventory summary with key metrics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Aggregated inventory summary report")
public class InventoryReportDTO {

    @Schema(description = "Total number of distinct products tracked", example = "150")
    private long totalProducts;

    @Schema(description = "Sum of all stock quantities across all products", example = "12500")
    private long totalStockUnits;

    @Schema(description = "Number of products at or below their minimum stock threshold", example = "8")
    private long lowStockProducts;

    @Schema(description = "Number of products with zero stock", example = "2")
    private long outOfStockProducts;
}
