package com.inventory.inventory.service;

import com.inventory.inventory.dto.StockUpdateDTO;
import com.inventory.inventory.entity.Stock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for managing stock levels. Implementations publish
 * {@code StockUpdatedEvent}s via the transactional outbox and push
 * real-time updates over WebSocket.
 */
public interface InventoryService {

    /** Creates a stock record for a newly created product. */
    Stock createStock(Long productId, String sku, int initialQuantity);

    /** Retrieves the current stock record for a SKU. */
    Stock getStockBySku(String sku);

    /** Returns a paginated list of all stock records. */
    Page<Stock> getAllStock(Pageable pageable);

    /** Adds quantity to existing stock (supplier delivery). */
    Stock restock(String sku, StockUpdateDTO dto);

    /** Deducts quantity from stock (sale). Throws if insufficient. */
    Stock sell(String sku, StockUpdateDTO dto);

    /** Sets stock to an exact quantity (manual correction). */
    Stock adjust(String sku, StockUpdateDTO dto);

    /** Removes the stock record for a deleted product. */
    void removeStock(String sku);
}
