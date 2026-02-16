package com.inventory.reporting.websocket;

import com.inventory.reporting.dto.InventoryReportDTO;
import com.inventory.reporting.dto.StockLevelDTO;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReportUpdateNotifier {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Pushes an updated stock level to WebSocket subscribers when a stock change event
     * is received from Kafka.
     *
     * @param stockLevel the updated stock level data
     */
    @CircuitBreaker(name = "websocket", fallbackMethod = "fallbackNotifyStockLevel")
    public void notifyStockLevelChange(StockLevelDTO stockLevel) {
        messagingTemplate.convertAndSend("/topic/reports/stock-levels", stockLevel);
        messagingTemplate.convertAndSend("/topic/reports/stock-levels/" + stockLevel.getSku(), stockLevel);
        log.debug("Report WebSocket notification sent for SKU: {}", stockLevel.getSku());
    }

    /**
     * Pushes an updated inventory summary to WebSocket subscribers.
     *
     * @param summary the recalculated inventory summary
     */
    @CircuitBreaker(name = "websocket", fallbackMethod = "fallbackNotifySummary")
    public void notifySummaryUpdate(InventoryReportDTO summary) {
        messagingTemplate.convertAndSend("/topic/reports/summary", summary);
        log.debug("Report summary WebSocket notification sent");
    }

    private void fallbackNotifyStockLevel(StockLevelDTO stockLevel, Throwable t) {
        log.warn("Report WebSocket notification failed for SKU: {}. Cause: {}", stockLevel.getSku(), t.getMessage());
    }

    private void fallbackNotifySummary(InventoryReportDTO summary, Throwable t) {
        log.warn("Report summary WebSocket notification failed. Cause: {}", t.getMessage());
    }
}
