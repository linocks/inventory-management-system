package com.inventory.reporting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * A single inventory event from the audit history (sourced from MongoDB).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Inventory event from the audit history")
public class EventHistoryDTO {

    @Schema(description = "Unique event identifier (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
    private String eventId;

    @Schema(description = "Type of event", example = "STOCK_UPDATED")
    private String eventType;

    @Schema(description = "SKU of the affected product", example = "PROD-001")
    private String sku;

    @Schema(description = "Product ID", example = "1")
    private Long productId;

    @Schema(description = "Stock quantity before the change", example = "100")
    private int previousQuantity;

    @Schema(description = "Stock quantity after the change", example = "85")
    private int newQuantity;

    @Schema(description = "Net change in quantity (positive = increase, negative = decrease)", example = "-15")
    private int changeAmount;

    @Schema(description = "Reason for the stock change", example = "SALE")
    private String reason;

    @Schema(description = "When the event occurred", example = "2025-01-15T10:30:00")
    private LocalDateTime timestamp;
}
