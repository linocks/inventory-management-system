package com.inventory.inventory.controller;

import com.inventory.common.dto.ApiResponse;
import com.inventory.inventory.dto.StockResponseDTO;
import com.inventory.inventory.dto.StockUpdateDTO;
import com.inventory.inventory.entity.Stock;
import com.inventory.inventory.mapper.StockMapper;
import com.inventory.inventory.service.InventoryService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory", description = "Stock management endpoints")
public class InventoryController {

    private final InventoryService inventoryService;
    private final StockMapper stockMapper;

    @GetMapping("/{sku}")
    @Operation(summary = "Get stock level for a product by SKU")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Stock record found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No stock record for this SKU"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    @RateLimiter(name = "readApi")
    public ResponseEntity<ApiResponse<StockResponseDTO>> getStockBySku(
            @Parameter(description = "Stock keeping unit identifier", example = "PROD-001") @PathVariable String sku) {
        Stock stock = inventoryService.getStockBySku(sku);
        return ResponseEntity.ok(ApiResponse.success(stockMapper.toDTO(stock)));
    }

    @GetMapping
    @Operation(summary = "Get all stock levels (paginated)", description = "Returns a paginated list of all stock records. Use query parameters: page, size, sort.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Page of stock records returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    @RateLimiter(name = "readApi")
    public ResponseEntity<ApiResponse<Page<StockResponseDTO>>> getAllStock(Pageable pageable) {
        Page<StockResponseDTO> stocks = inventoryService.getAllStock(pageable).map(stockMapper::toDTO);
        return ResponseEntity.ok(ApiResponse.success(stocks));
    }

    @PutMapping("/{sku}/restock")
    @Operation(summary = "Restock a product", description = "Adds quantity to existing stock. Publishes a StockUpdatedEvent with reason RESTOCK. Automatically retries on concurrent modification conflicts.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product restocked successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error (quantity < 1)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No stock record for this SKU"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Concurrent modification conflict (retry exhausted)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    @RateLimiter(name = "writeApi")
    public ResponseEntity<ApiResponse<StockResponseDTO>> restock(
            @Parameter(description = "Stock keeping unit identifier", example = "PROD-001") @PathVariable String sku,
            @Valid @RequestBody StockUpdateDTO dto) {
        Stock updated = inventoryService.restock(sku, dto);
        return ResponseEntity.ok(ApiResponse.success("Product restocked successfully", stockMapper.toDTO(updated)));
    }

    @PutMapping("/{sku}/sell")
    @Operation(summary = "Deduct stock for a sale", description = "Deducts quantity from stock. Fails if insufficient stock is available. Publishes a StockUpdatedEvent with reason SALE.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Stock deducted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Insufficient stock or validation error"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No stock record for this SKU"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Concurrent modification conflict (retry exhausted)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    @RateLimiter(name = "writeApi")
    public ResponseEntity<ApiResponse<StockResponseDTO>> sell(
            @Parameter(description = "Stock keeping unit identifier", example = "PROD-001") @PathVariable String sku,
            @Valid @RequestBody StockUpdateDTO dto) {
        Stock updated = inventoryService.sell(sku, dto);
        return ResponseEntity.ok(ApiResponse.success("Stock deducted successfully", stockMapper.toDTO(updated)));
    }

    @PutMapping("/{sku}/adjust")
    @Operation(summary = "Manual stock adjustment", description = "Sets stock to an exact quantity. Used for corrections after physical inventory counts. Publishes a StockUpdatedEvent with reason ADJUSTMENT.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Stock adjusted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error (quantity < 1)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No stock record for this SKU"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Concurrent modification conflict (retry exhausted)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    @RateLimiter(name = "writeApi")
    public ResponseEntity<ApiResponse<StockResponseDTO>> adjust(
            @Parameter(description = "Stock keeping unit identifier", example = "PROD-001") @PathVariable String sku,
            @Valid @RequestBody StockUpdateDTO dto) {
        Stock updated = inventoryService.adjust(sku, dto);
        return ResponseEntity.ok(ApiResponse.success("Stock adjusted successfully", stockMapper.toDTO(updated)));
    }
}
