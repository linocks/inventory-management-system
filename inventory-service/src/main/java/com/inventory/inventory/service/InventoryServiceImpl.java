package com.inventory.inventory.service;

import com.inventory.common.constants.KafkaConstants;
import com.inventory.common.outbox.OutboxEventService;
import com.inventory.inventory.dto.StockUpdateDTO;
import com.inventory.common.event.StockUpdatedEvent;
import com.inventory.common.event.StockUpdatedEvent.StockChangeReason;
import com.inventory.common.exception.InsufficientStockException;
import com.inventory.common.exception.ProductNotFoundException;
import com.inventory.inventory.entity.InventoryEvent;
import com.inventory.inventory.entity.Stock;
import com.inventory.inventory.repository.StockRepository;
import com.inventory.inventory.websocket.StockUpdateNotifier;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryServiceImpl implements InventoryService {

    private final StockRepository stockRepository;
    private final EventStoreService eventStoreService;
    private final OutboxEventService outboxEventService;
    private final StockUpdateNotifier stockUpdateNotifier;

    /**
     * Creates a new stock record for a product. If a stock record already exists for the given SKU,
     * returns the existing record instead of creating a duplicate.
     * Publishes an INITIAL stock event to Kafka and stores it in MongoDB.
     *
     * @param productId the ID of the product from the Product Service
     * @param sku       the unique stock keeping unit identifier
     * @param initialQuantity the starting stock quantity
     * @return the created or existing stock record
     */
    @Override
    @Transactional
    public Stock createStock(Long productId, String sku, int initialQuantity) {
        if (stockRepository.existsBySku(sku)) {
            log.warn("Stock already exists for SKU: {}", sku);
            return stockRepository.findBySku(sku).orElseThrow();
        }

        Stock stock = Stock.builder()
                .productId(productId)
                .sku(sku)
                .quantity(initialQuantity)
                .build();
        Stock saved = stockRepository.save(stock);
        log.info("Stock created: sku={}, quantity={}", sku, initialQuantity);

        publishAndStoreEvent(saved, 0, initialQuantity, StockChangeReason.INITIAL);
        return saved;
    }

    /**
     * Retrieves the current stock record for a given SKU.
     *
     * @param sku the stock keeping unit identifier
     * @return the stock record
     * @throws ProductNotFoundException if no stock record exists for the SKU
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "stock", key = "#sku")
    public Stock getStockBySku(String sku) {
        return stockRepository.findBySku(sku)
                .orElseThrow(() -> new ProductNotFoundException(sku));
    }

    /**
     * Retrieves all stock records with pagination support.
     *
     * @param pageable pagination parameters (page number, size, sort)
     * @return a page of stock records
     */
    @Override
    @Transactional(readOnly = true)
    public Page<Stock> getAllStock(Pageable pageable) {
        return stockRepository.findAll(pageable);
    }

    /**
     * Adds stock quantity to an existing product. Used when new inventory arrives
     * from a supplier. Publishes a RESTOCK event to Kafka and stores it in MongoDB.
     *
     * @param sku the stock keeping unit identifier
     * @param dto contains the quantity to add and an optional reason
     * @return the updated stock record
     * @throws ProductNotFoundException if no stock record exists for the SKU
     */
    @Override
    @Transactional
    @CacheEvict(value = "stock", key = "#sku")
    @Retry(name = "stockUpdate")
    public Stock restock(String sku, StockUpdateDTO dto) {
        Stock stock = stockRepository.findBySku(sku)
                .orElseThrow(() -> new ProductNotFoundException(sku));

        int previousQuantity = stock.getQuantity();
        stock.setQuantity(previousQuantity + dto.getQuantity());
        Stock updated = stockRepository.save(stock);
        log.info("Restocked: sku={}, added={}, new quantity={}", sku, dto.getQuantity(), updated.getQuantity());

        publishAndStoreEvent(updated, previousQuantity, updated.getQuantity(), StockChangeReason.RESTOCK);
        return updated;
    }

    /**
     * Deducts stock quantity for a sale. Validates that sufficient stock is available
     * before processing. Publishes a SALE event to Kafka and stores it in MongoDB.
     *
     * @param sku the stock keeping unit identifier
     * @param dto contains the quantity to deduct and an optional reason
     * @return the updated stock record
     * @throws ProductNotFoundException    if no stock record exists for the SKU
     * @throws InsufficientStockException if available stock is less than requested quantity
     */
    @Override
    @Transactional
    @CacheEvict(value = "stock", key = "#sku")
    @Retry(name = "stockUpdate")
    public Stock sell(String sku, StockUpdateDTO dto) {
        Stock stock = stockRepository.findBySku(sku)
                .orElseThrow(() -> new ProductNotFoundException(sku));

        if (stock.getQuantity() < dto.getQuantity()) {
            throw new InsufficientStockException(sku, dto.getQuantity(), stock.getQuantity());
        }

        int previousQuantity = stock.getQuantity();
        stock.setQuantity(previousQuantity - dto.getQuantity());
        Stock updated = stockRepository.save(stock);
        log.info("Sold: sku={}, deducted={}, new quantity={}", sku, dto.getQuantity(), updated.getQuantity());

        publishAndStoreEvent(updated, previousQuantity, updated.getQuantity(), StockChangeReason.SALE);
        return updated;
    }

    /**
     * Sets the stock quantity to an exact value. Used for manual corrections
     * such as after a physical inventory count. Publishes an ADJUSTMENT event
     * to Kafka and stores it in MongoDB.
     *
     * @param sku the stock keeping unit identifier
     * @param dto contains the new absolute quantity and an optional reason
     * @return the updated stock record
     * @throws ProductNotFoundException if no stock record exists for the SKU
     */
    @Override
    @Transactional
    @CacheEvict(value = "stock", key = "#sku")
    @Retry(name = "stockUpdate")
    public Stock adjust(String sku, StockUpdateDTO dto) {
        Stock stock = stockRepository.findBySku(sku)
                .orElseThrow(() -> new ProductNotFoundException(sku));

        int previousQuantity = stock.getQuantity();
        stock.setQuantity(dto.getQuantity());
        Stock updated = stockRepository.save(stock);
        log.info("Adjusted: sku={}, from={}, to={}", sku, previousQuantity, updated.getQuantity());

        publishAndStoreEvent(updated, previousQuantity, updated.getQuantity(), StockChangeReason.ADJUSTMENT);
        return updated;
    }

    /**
     * Removes the stock record for a product. Triggered when a product is deleted
     * from the Product Service via a Kafka event.
     *
     * @param sku the stock keeping unit identifier
     * @throws ProductNotFoundException if no stock record exists for the SKU
     */
    @Override
    @Transactional
    @CacheEvict(value = "stock", key = "#sku")
    public void removeStock(String sku) {
        Stock stock = stockRepository.findBySku(sku)
                .orElseThrow(() -> new ProductNotFoundException(sku));
        stockRepository.delete(stock);
        log.info("Stock removed for SKU: {}", sku);
    }

    /**
     * Stores a stock change event in MongoDB for audit history and publishes it
     * to Kafka for downstream consumers (e.g., Reporting Service, WebSocket notifications).
     *
     * @param stock       the stock record after the change
     * @param previousQty the quantity before the change
     * @param newQty      the quantity after the change
     * @param reason      the reason for the stock change
     */
    private void publishAndStoreEvent(Stock stock, int previousQty, int newQty, StockChangeReason reason) {
        StockUpdatedEvent event = StockUpdatedEvent.builder()
                .productId(stock.getProductId())
                .sku(stock.getSku())
                .previousQuantity(previousQty)
                .newQuantity(newQty)
                .minThreshold(stock.getMinThreshold())
                .changeAmount(newQty - previousQty)
                .reason(reason)
                .build();

        // Store in MongoDB (event history / audit trail)
        InventoryEvent mongoEvent = InventoryEvent.builder()
                .eventId(event.getEventId())
                .eventType(event.getEventType().name())
                .sku(stock.getSku())
                .productId(stock.getProductId())
                .previousQuantity(previousQty)
                .newQuantity(newQty)
                .changeAmount(newQty - previousQty)
                .reason(reason.name())
                .timestamp(event.getTimestamp())
                .build();
        eventStoreService.saveEvent(mongoEvent);

        // Save to outbox (same PostgreSQL transaction as stock update)
        outboxEventService.saveEvent(KafkaConstants.TOPIC_STOCK_UPDATED, stock.getSku(), event);

        // Push real-time update to WebSocket subscribers
        stockUpdateNotifier.notifyStockUpdate(stock);
    }
}
