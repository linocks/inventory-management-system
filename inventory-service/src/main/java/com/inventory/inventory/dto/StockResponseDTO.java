package com.inventory.inventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO representing a stock record. Decouples the API response
 * from the internal JPA entity, hiding fields like {@code version}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Stock level for a product")
public class StockResponseDTO {

    @Schema(description = "Stock record ID", example = "1")
    private Long id;

    @Schema(description = "Product ID from the Product Service", example = "42")
    private Long productId;

    @Schema(description = "Stock keeping unit identifier", example = "PROD-001")
    private String sku;

    @Schema(description = "Current stock quantity", example = "150")
    private int quantity;

    @Schema(description = "Minimum stock threshold for low-stock alerts", example = "10")
    private int minThreshold;

    @Schema(description = "Timestamp when the stock record was created", example = "2026-02-15T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "Timestamp of the last stock update", example = "2026-02-15T14:45:00")
    private LocalDateTime updatedAt;
}
