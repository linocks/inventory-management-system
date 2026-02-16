package com.inventory.reporting.kafka;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.inventory.common.event.StockUpdatedEvent;
import com.inventory.reporting.dto.InventoryReportDTO;
import com.inventory.reporting.dto.StockLevelDTO;
import com.inventory.reporting.service.InventorySummaryProjectionService;
import com.inventory.reporting.websocket.ReportUpdateNotifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportEventConsumerTest {

    @Mock
    private InventorySummaryProjectionService summaryProjectionService;

    @Mock
    private ReportUpdateNotifier reportUpdateNotifier;

    private ObjectMapper objectMapper;
    private ReportEventConsumer consumer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        consumer = new ReportEventConsumer(summaryProjectionService, reportUpdateNotifier, objectMapper);
    }

    @Test
    @DisplayName("should deserialize StockUpdatedEvent and push stock level via WebSocket")
    void shouldDeserializeAndPushStockLevel() throws Exception {
        StockUpdatedEvent event = StockUpdatedEvent.builder()
                .productId(1L)
                .sku("PROD-001")
                .previousQuantity(100)
                .newQuantity(85)
                .minThreshold(10)
                .changeAmount(-15)
                .reason(StockUpdatedEvent.StockChangeReason.SALE)
                .build();
        String json = objectMapper.writeValueAsString(event);

        InventoryReportDTO summary = InventoryReportDTO.builder()
                .totalProducts(10)
                .totalStockUnits(500)
                .lowStockProducts(2)
                .outOfStockProducts(0)
                .build();
        when(summaryProjectionService.applyStockUpdate(any())).thenReturn(summary);

        consumer.handleStockUpdated(json);

        ArgumentCaptor<StockLevelDTO> stockCaptor = ArgumentCaptor.forClass(StockLevelDTO.class);
        verify(reportUpdateNotifier).notifyStockLevelChange(stockCaptor.capture());

        StockLevelDTO pushed = stockCaptor.getValue();
        assertThat(pushed.getProductId()).isEqualTo(1L);
        assertThat(pushed.getSku()).isEqualTo("PROD-001");
        assertThat(pushed.getQuantity()).isEqualTo(85);
        assertThat(pushed.getMinThreshold()).isEqualTo(10);
        assertThat(pushed.isLowStock()).isFalse();
    }

    @Test
    @DisplayName("should mark stock as low when quantity is 10 or below")
    void shouldMarkLowStock() throws Exception {
        StockUpdatedEvent event = StockUpdatedEvent.builder()
                .productId(2L)
                .sku("PROD-002")
                .previousQuantity(15)
                .newQuantity(10)
                .minThreshold(12)
                .changeAmount(-5)
                .reason(StockUpdatedEvent.StockChangeReason.SALE)
                .build();
        String json = objectMapper.writeValueAsString(event);

        when(summaryProjectionService.applyStockUpdate(any())).thenReturn(InventoryReportDTO.builder().build());

        consumer.handleStockUpdated(json);

        ArgumentCaptor<StockLevelDTO> captor = ArgumentCaptor.forClass(StockLevelDTO.class);
        verify(reportUpdateNotifier).notifyStockLevelChange(captor.capture());
        assertThat(captor.getValue().getMinThreshold()).isEqualTo(12);
        assertThat(captor.getValue().isLowStock()).isTrue();
    }

    @Test
    @DisplayName("should throw on malformed message so DLT error handler can retry")
    void shouldThrowOnMalformedMessage() {
        assertThatThrownBy(() -> consumer.handleStockUpdated("not valid json"))
                .isInstanceOf(Exception.class);

        verify(reportUpdateNotifier, never()).notifyStockLevelChange(any());
        verify(reportUpdateNotifier, never()).notifySummaryUpdate(any());
    }

}
