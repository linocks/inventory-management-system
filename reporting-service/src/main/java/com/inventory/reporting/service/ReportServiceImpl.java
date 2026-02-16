package com.inventory.reporting.service;

import com.inventory.reporting.dto.EventHistoryDTO;
import com.inventory.reporting.dto.InventoryReportDTO;
import com.inventory.reporting.dto.StockLevelDTO;
import com.inventory.reporting.entity.InventoryEventView;
import com.inventory.reporting.entity.StockView;
import com.inventory.reporting.repository.EventHistoryRepository;
import com.inventory.reporting.repository.StockReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link ReportService}.
 * Queries PostgreSQL for current stock state and MongoDB for event history.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportServiceImpl implements ReportService {

    private final StockReportRepository stockReportRepository;
    private final EventHistoryRepository eventHistoryRepository;
    private final InventorySummaryProjectionService summaryProjectionService;

    @Override
    @Transactional(readOnly = true)
    public Page<StockLevelDTO> getAllStockLevels(Pageable pageable) {
        return stockReportRepository.findAll(pageable)
                .map(this::toStockLevelDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StockLevelDTO> getLowStockProducts(Pageable pageable) {
        return stockReportRepository.findLowStockProducts(pageable)
                .map(this::toStockLevelDTO);
    }

    @Override
    public Page<EventHistoryDTO> getEventHistory(Pageable pageable) {
        return eventHistoryRepository.findAll(pageable)
                .map(this::toEventHistoryDTO);
    }

    @Override
    public Page<EventHistoryDTO> getEventHistoryBySku(String sku, Pageable pageable) {
        return eventHistoryRepository.findBySku(sku, pageable)
                .map(this::toEventHistoryDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryReportDTO getInventorySummary() {
        return summaryProjectionService.getCurrentSummary();
    }

    private StockLevelDTO toStockLevelDTO(StockView stock) {
        return StockLevelDTO.builder()
                .productId(stock.getProductId())
                .sku(stock.getSku())
                .quantity(stock.getQuantity())
                .minThreshold(stock.getMinThreshold())
                .lowStock(stock.getQuantity() <= stock.getMinThreshold())
                .build();
    }

    private EventHistoryDTO toEventHistoryDTO(InventoryEventView event) {
        return EventHistoryDTO.builder()
                .eventId(event.getEventId())
                .eventType(event.getEventType())
                .sku(event.getSku())
                .productId(event.getProductId())
                .previousQuantity(event.getPreviousQuantity())
                .newQuantity(event.getNewQuantity())
                .changeAmount(event.getChangeAmount())
                .reason(event.getReason())
                .timestamp(event.getTimestamp())
                .build();
    }
}
