package com.inventory.inventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for stock update operations (restock, sell, adjust).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Stock update request payload")
public class StockUpdateDTO {

    @Min(value = 1, message = "Quantity must be at least 1")
    @Schema(description = "Quantity to add (restock), deduct (sell), or set (adjust)", example = "25", requiredMode = Schema.RequiredMode.REQUIRED)
    private int quantity;

    @Schema(description = "Optional reason for the stock change", example = "Supplier delivery #4521")
    private String reason;
}
