package com.inventory.reporting.service;

import com.inventory.reporting.dto.EventHistoryDTO;
import com.inventory.reporting.dto.InventoryReportDTO;
import com.inventory.reporting.dto.StockLevelDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for read-only inventory reports. Queries both PostgreSQL
 * (current stock state) and MongoDB (event audit trail).
 */
public interface ReportService {

    /** Returns current stock levels for all tracked products. */
    Page<StockLevelDTO> getAllStockLevels(Pageable pageable);

    /** Returns products where quantity is at or below minimum threshold (paginated). */
    Page<StockLevelDTO> getLowStockProducts(Pageable pageable);

    /** Returns paginated inventory event history from MongoDB. */
    Page<EventHistoryDTO> getEventHistory(Pageable pageable);

    /** Returns paginated event history filtered by SKU. */
    Page<EventHistoryDTO> getEventHistoryBySku(String sku, Pageable pageable);

    /** Returns aggregated inventory metrics (totals, low-stock counts). */
    InventoryReportDTO getInventorySummary();
}
