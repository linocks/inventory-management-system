package com.inventory.reporting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Current stock level for a single product, including low-stock status.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Current stock level for a product")
public class StockLevelDTO {

    @Schema(description = "Product ID", example = "1")
    private Long productId;

    @Schema(description = "Stock keeping unit identifier", example = "PROD-001")
    private String sku;

    @Schema(description = "Current quantity in stock", example = "42")
    private int quantity;

    @Schema(description = "Minimum stock threshold before low-stock alert", example = "10")
    private int minThreshold;

    @Schema(description = "True if quantity is at or below the minimum threshold", example = "false")
    private boolean lowStock;
}
