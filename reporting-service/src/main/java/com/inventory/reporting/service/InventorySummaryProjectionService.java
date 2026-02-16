package com.inventory.reporting.service;

import com.inventory.common.event.StockUpdatedEvent;
import com.inventory.reporting.dto.InventoryReportDTO;
import com.inventory.reporting.entity.InventorySummaryProjection;
import com.inventory.reporting.repository.InventorySummaryProjectionRepository;
import com.inventory.reporting.repository.StockReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class InventorySummaryProjectionService {

    private static final long SUMMARY_ID = 1L;

    private final InventorySummaryProjectionRepository projectionRepository;
    private final StockReportRepository stockReportRepository;

    @Transactional(readOnly = true)
    public InventoryReportDTO getCurrentSummary() {
        return projectionRepository.findById(SUMMARY_ID)
                .map(this::toDto)
                .orElseGet(this::rebuildSummaryFromSourceOfTruth);
    }

    @Transactional
    public InventoryReportDTO applyStockUpdate(StockUpdatedEvent event) {
        InventorySummaryProjection projection = projectionRepository.findById(SUMMARY_ID)
                .orElseGet(this::rebuildProjectionEntity);

        int threshold = event.getMinThreshold() > 0 ? event.getMinThreshold() : 10;
        boolean wasLow = event.getPreviousQuantity() <= threshold;
        boolean isLow = event.getNewQuantity() <= threshold;
        boolean wasOut = event.getPreviousQuantity() == 0;
        boolean isOut = event.getNewQuantity() == 0;

        projection.setTotalProducts(stockReportRepository.count());
        projection.setTotalStockUnits(Math.max(0, projection.getTotalStockUnits()
                + (long) event.getNewQuantity() - event.getPreviousQuantity()));
        projection.setLowStockProducts(adjustCounter(projection.getLowStockProducts(), wasLow, isLow));
        projection.setOutOfStockProducts(adjustCounter(projection.getOutOfStockProducts(), wasOut, isOut));
        projection.setUpdatedAt(LocalDateTime.now());

        return toDto(projectionRepository.save(projection));
    }

    private long adjustCounter(long current, boolean before, boolean after) {
        if (before == after) {
            return current;
        }
        if (after) {
            return current + 1;
        }
        return Math.max(0, current - 1);
    }

    private InventoryReportDTO rebuildSummaryFromSourceOfTruth() {
        return toDto(projectionRepository.save(rebuildProjectionEntity()));
    }

    private InventorySummaryProjection rebuildProjectionEntity() {
        return InventorySummaryProjection.builder()
                .id(SUMMARY_ID)
                .totalProducts(stockReportRepository.count())
                .totalStockUnits(stockReportRepository.sumTotalStockUnits())
                .lowStockProducts(stockReportRepository.countLowStockProducts())
                .outOfStockProducts(stockReportRepository.countOutOfStockProducts())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private InventoryReportDTO toDto(InventorySummaryProjection projection) {
        return InventoryReportDTO.builder()
                .totalProducts(projection.getTotalProducts())
                .totalStockUnits(projection.getTotalStockUnits())
                .lowStockProducts(projection.getLowStockProducts())
                .outOfStockProducts(projection.getOutOfStockProducts())
                .build();
    }
}
