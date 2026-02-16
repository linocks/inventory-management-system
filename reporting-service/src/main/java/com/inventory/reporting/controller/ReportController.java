package com.inventory.reporting.controller;

import com.inventory.common.dto.ApiResponse;
import com.inventory.reporting.dto.EventHistoryDTO;
import com.inventory.reporting.dto.InventoryReportDTO;
import com.inventory.reporting.dto.StockLevelDTO;
import com.inventory.reporting.service.ReportService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Inventory reporting endpoints")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/stock-levels")
    @Operation(summary = "Get current stock levels for all products", description = "Returns the current stock level, minimum threshold, and low-stock status for every tracked product.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Stock levels returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    @RateLimiter(name = "readApi")
    public ResponseEntity<ApiResponse<Page<StockLevelDTO>>> getStockLevels(Pageable pageable) {
        Page<StockLevelDTO> levels = reportService.getAllStockLevels(pageable);
        return ResponseEntity.ok(ApiResponse.success(levels));
    }

    @GetMapping("/low-stock")
    @Operation(summary = "Get products below minimum stock threshold", description = "Returns only products where the current quantity is at or below the configured minimum threshold.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Low-stock products returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    @RateLimiter(name = "readApi")
    public ResponseEntity<ApiResponse<Page<StockLevelDTO>>> getLowStock(Pageable pageable) {
        Page<StockLevelDTO> lowStock = reportService.getLowStockProducts(pageable);
        return ResponseEntity.ok(ApiResponse.success(lowStock));
    }

    @GetMapping("/history")
    @Operation(summary = "Get all inventory events (paginated)", description = "Returns the complete audit trail of all stock changes from MongoDB. Use query parameters: page, size, sort.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Page of inventory events returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    @RateLimiter(name = "readApi")
    public ResponseEntity<ApiResponse<Page<EventHistoryDTO>>> getEventHistory(Pageable pageable) {
        Page<EventHistoryDTO> history = reportService.getEventHistory(pageable);
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    @GetMapping("/history/{sku}")
    @Operation(summary = "Get inventory event history for a specific product", description = "Returns the audit trail for a single product, filtered by SKU.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product event history returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    @RateLimiter(name = "readApi")
    public ResponseEntity<ApiResponse<Page<EventHistoryDTO>>> getEventHistoryBySku(
            @Parameter(description = "Stock keeping unit identifier", example = "PROD-001") @PathVariable String sku,
            Pageable pageable) {
        Page<EventHistoryDTO> history = reportService.getEventHistoryBySku(sku, pageable);
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    @GetMapping("/summary")
    @Operation(summary = "Get inventory summary report", description = "Returns aggregated metrics: total products, total stock units, low-stock count, and out-of-stock count.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Inventory summary returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    @RateLimiter(name = "readApi")
    public ResponseEntity<ApiResponse<InventoryReportDTO>> getSummary() {
        InventoryReportDTO summary = reportService.getInventorySummary();
        return ResponseEntity.ok(ApiResponse.success(summary));
    }
}
