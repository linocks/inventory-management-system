package com.inventory.reporting.service;

import com.inventory.common.event.StockUpdatedEvent;
import com.inventory.reporting.dto.InventoryReportDTO;
import com.inventory.reporting.entity.InventorySummaryProjection;
import com.inventory.reporting.repository.InventorySummaryProjectionRepository;
import com.inventory.reporting.repository.StockReportRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventorySummaryProjectionServiceTest {

    @Mock
    private InventorySummaryProjectionRepository projectionRepository;

    @Mock
    private StockReportRepository stockReportRepository;

    @InjectMocks
    private InventorySummaryProjectionService service;

    @Test
    @DisplayName("should rebuild summary from source of truth when projection does not exist")
    void shouldRebuildSummaryWhenMissing() {
        when(projectionRepository.findById(1L)).thenReturn(Optional.empty());
        when(stockReportRepository.count()).thenReturn(10L);
        when(stockReportRepository.sumTotalStockUnits()).thenReturn(250L);
        when(stockReportRepository.countLowStockProducts()).thenReturn(3L);
        when(stockReportRepository.countOutOfStockProducts()).thenReturn(1L);
        when(projectionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        InventoryReportDTO summary = service.getCurrentSummary();

        assertThat(summary.getTotalProducts()).isEqualTo(10);
        assertThat(summary.getTotalStockUnits()).isEqualTo(250);
        assertThat(summary.getLowStockProducts()).isEqualTo(3);
        assertThat(summary.getOutOfStockProducts()).isEqualTo(1);
    }

    @Test
    @DisplayName("should apply stock update delta to projection counters")
    void shouldApplyStockUpdateDelta() {
        InventorySummaryProjection projection = InventorySummaryProjection.builder()
                .id(1L)
                .totalProducts(10)
                .totalStockUnits(100)
                .lowStockProducts(2)
                .outOfStockProducts(1)
                .build();
        when(projectionRepository.findById(1L)).thenReturn(Optional.of(projection));
        when(stockReportRepository.count()).thenReturn(10L);
        when(projectionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        StockUpdatedEvent event = StockUpdatedEvent.builder()
                .productId(1L)
                .sku("PROD-1")
                .previousQuantity(0)
                .newQuantity(5)
                .minThreshold(10)
                .changeAmount(5)
                .reason(StockUpdatedEvent.StockChangeReason.RESTOCK)
                .build();

        InventoryReportDTO summary = service.applyStockUpdate(event);

        assertThat(summary.getTotalStockUnits()).isEqualTo(105);
        assertThat(summary.getLowStockProducts()).isEqualTo(2);
        assertThat(summary.getOutOfStockProducts()).isZero();

        ArgumentCaptor<InventorySummaryProjection> captor = ArgumentCaptor.forClass(InventorySummaryProjection.class);
        verify(projectionRepository).save(captor.capture());
        assertThat(captor.getValue().getUpdatedAt()).isNotNull();
    }
}
