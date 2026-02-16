package com.inventory.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Data transfer object for product create and update operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Product data for create and update operations")
public class ProductDTO {

    @Schema(description = "Product ID (auto-generated)", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotBlank(message = "SKU is required")
    @Schema(description = "Unique stock keeping unit identifier", example = "PROD-001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String sku;

    @NotBlank(message = "Product name is required")
    @Schema(description = "Product display name", example = "Wireless Mouse", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "Detailed product description", example = "Ergonomic wireless mouse with USB-C receiver")
    private String description;

    @Schema(description = "Product category for grouping", example = "Electronics")
    private String category;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than zero")
    @Schema(description = "Product price in USD", example = "29.99", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal price;

    @Min(value = 0, message = "Initial stock cannot be negative")
    @Schema(description = "Initial stock quantity (used only on creation)", example = "100")
    private int initialStock;
}
