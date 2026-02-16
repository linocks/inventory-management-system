package com.inventory.reporting.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.common.event.EventContractValidator;
import com.inventory.common.event.EventType;
import com.inventory.common.event.StockUpdatedEvent;
import com.inventory.reporting.dto.InventoryReportDTO;
import com.inventory.reporting.dto.StockLevelDTO;
import com.inventory.reporting.service.InventorySummaryProjectionService;
import com.inventory.reporting.websocket.ReportUpdateNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import static com.inventory.common.constants.KafkaConstants.*;

/**
 * Consumes stock update events from Kafka.
 * Exceptions propagate to the DefaultErrorHandler which retries 3 times
 * then publishes to a dead letter topic ({topic}.DLT).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReportEventConsumer {

    private final InventorySummaryProjectionService summaryProjectionService;
    private final ReportUpdateNotifier reportUpdateNotifier;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = TOPIC_STOCK_UPDATED, groupId = GROUP_REPORTING_SERVICE)
    public void handleStockUpdated(String message) throws Exception {
        StockUpdatedEvent event = objectMapper.readValue(message, StockUpdatedEvent.class);
        EventContractValidator.validate(event, EventType.STOCK_UPDATED, TOPIC_STOCK_UPDATED);
        log.info("Received StockUpdatedEvent: sku={}, {} -> {}, reason={}",
                event.getSku(), event.getPreviousQuantity(),
                event.getNewQuantity(), event.getReason());

        int threshold = event.getMinThreshold() > 0 ? event.getMinThreshold() : 10;

        // Push the individual stock level change
        StockLevelDTO stockLevel = StockLevelDTO.builder()
                .productId(event.getProductId())
                .sku(event.getSku())
                .quantity(event.getNewQuantity())
                .minThreshold(threshold)
                .lowStock(event.getNewQuantity() <= threshold)
                .build();
        reportUpdateNotifier.notifyStockLevelChange(stockLevel);

        // Apply event delta to summary projection, then push updated summary
        InventoryReportDTO summary = summaryProjectionService.applyStockUpdate(event);
        reportUpdateNotifier.notifySummaryUpdate(summary);
    }
}
