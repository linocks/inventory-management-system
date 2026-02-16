package com.inventory.reporting.service;

import com.inventory.reporting.dto.EventHistoryDTO;
import com.inventory.reporting.dto.InventoryReportDTO;
import com.inventory.reporting.dto.StockLevelDTO;
import com.inventory.reporting.entity.InventoryEventView;
import com.inventory.reporting.entity.StockView;
import com.inventory.reporting.repository.EventHistoryRepository;
import com.inventory.reporting.repository.StockReportRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private StockReportRepository stockReportRepository;

    @Mock
    private EventHistoryRepository eventHistoryRepository;

    @Mock
    private InventorySummaryProjectionService summaryProjectionService;

    @InjectMocks
    private ReportServiceImpl reportService;

    private StockView createStockView(Long id, Long productId, String sku, int quantity, int minThreshold) {
        try {
            StockView view = new StockView();
            setField(view, "id", id);
            setField(view, "productId", productId);
            setField(view, "sku", sku);
            setField(view, "quantity", quantity);
            setField(view, "minThreshold", minThreshold);
            return view;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private InventoryEventView createEventView(String eventId, String eventType, String sku,
                                                 Long productId, int prevQty, int newQty, int change,
                                                 String reason) {
        try {
            InventoryEventView view = new InventoryEventView();
            setField(view, "eventId", eventId);
            setField(view, "eventType", eventType);
            setField(view, "sku", sku);
            setField(view, "productId", productId);
            setField(view, "previousQuantity", prevQty);
            setField(view, "newQuantity", newQty);
            setField(view, "changeAmount", change);
            setField(view, "reason", reason);
            setField(view, "timestamp", LocalDateTime.now());
            return view;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setField(Object obj, String fieldName, Object value) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }

    @Nested
    @DisplayName("getAllStockLevels")
    class GetAllStockLevels {

        @Test
        @DisplayName("should return all stock levels as DTOs")
        void shouldReturnAllStockLevels() {
            Pageable pageable = PageRequest.of(0, 10);
            StockView stock1 = createStockView(1L, 100L, "PROD-001", 50, 10);
            StockView stock2 = createStockView(2L, 101L, "PROD-002", 5, 10);
            when(stockReportRepository.findAll(pageable))
                    .thenReturn(new PageImpl<>(List.of(stock1, stock2), pageable, 2));

            Page<StockLevelDTO> result = reportService.getAllStockLevels(pageable);

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent().get(0).getSku()).isEqualTo("PROD-001");
            assertThat(result.getContent().get(0).isLowStock()).isFalse();
            assertThat(result.getContent().get(1).getSku()).isEqualTo("PROD-002");
            assertThat(result.getContent().get(1).isLowStock()).isTrue();
        }

    }

    @Nested
    @DisplayName("getLowStockProducts")
    class GetLowStockProducts {

        @Test
        @DisplayName("should return only low stock products")
        void shouldReturnLowStock() {
            Pageable pageable = PageRequest.of(0, 10);
            StockView lowStock = createStockView(1L, 100L, "PROD-001", 5, 10);
            when(stockReportRepository.findLowStockProducts(pageable))
                    .thenReturn(new PageImpl<>(List.of(lowStock), pageable, 1));

            Page<StockLevelDTO> result = reportService.getLowStockProducts(pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().getFirst().isLowStock()).isTrue();
            assertThat(result.getContent().getFirst().getQuantity()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("getEventHistory")
    class GetEventHistory {

        @Test
        @DisplayName("should return paginated event history")
        void shouldReturnPaginated() {
            Pageable pageable = PageRequest.of(0, 10);
            InventoryEventView event = createEventView(
                    "evt-1", "STOCK_UPDATED", "PROD-001", 100L,
                    50, 40, -10, "SALE");
            Page<InventoryEventView> page = new PageImpl<>(List.of(event), pageable, 1);
            when(eventHistoryRepository.findAll(pageable)).thenReturn(page);

            Page<EventHistoryDTO> result = reportService.getEventHistory(pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().getFirst().getSku()).isEqualTo("PROD-001");
            assertThat(result.getContent().getFirst().getChangeAmount()).isEqualTo(-10);
        }
    }

    @Nested
    @DisplayName("getInventorySummary")
    class GetInventorySummary {

        @Test
        @DisplayName("should return correct inventory summary")
        void shouldReturnSummary() {
            when(summaryProjectionService.getCurrentSummary()).thenReturn(InventoryReportDTO.builder()
                    .totalProducts(10)
                    .totalStockUnits(500)
                    .lowStockProducts(2)
                    .outOfStockProducts(1)
                    .build());

            InventoryReportDTO result = reportService.getInventorySummary();

            assertThat(result.getTotalProducts()).isEqualTo(10);
            assertThat(result.getTotalStockUnits()).isEqualTo(500);
            assertThat(result.getLowStockProducts()).isEqualTo(2);
            assertThat(result.getOutOfStockProducts()).isEqualTo(1);
        }

    }
}
