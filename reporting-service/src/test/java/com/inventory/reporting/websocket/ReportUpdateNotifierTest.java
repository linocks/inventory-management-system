package com.inventory.reporting.websocket;

import com.inventory.reporting.dto.InventoryReportDTO;
import com.inventory.reporting.dto.StockLevelDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReportUpdateNotifierTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private ReportUpdateNotifier reportUpdateNotifier;

    @Test
    @DisplayName("should send inventory summary to reports summary topic")
    void shouldSendSummaryToTopic() {
        InventoryReportDTO summary = InventoryReportDTO.builder()
                .totalProducts(100)
                .totalStockUnits(5000)
                .lowStockProducts(5)
                .outOfStockProducts(2)
                .build();

        reportUpdateNotifier.notifySummaryUpdate(summary);

        verify(messagingTemplate).convertAndSend("/topic/reports/summary", summary);
    }

    @Test
    @DisplayName("should send stock level to both global and SKU-specific topics")
    void shouldSendStockLevelToBothTopics() {
        StockLevelDTO stockLevel = StockLevelDTO.builder()
                .productId(2L)
                .sku("ELEC-042")
                .quantity(8)
                .lowStock(true)
                .build();

        reportUpdateNotifier.notifyStockLevelChange(stockLevel);

        verify(messagingTemplate).convertAndSend("/topic/reports/stock-levels", stockLevel);
        verify(messagingTemplate).convertAndSend("/topic/reports/stock-levels/ELEC-042", stockLevel);
    }
}
