package com.inventory.inventory.websocket;

import com.inventory.inventory.dto.StockResponseDTO;
import com.inventory.inventory.entity.Stock;
import com.inventory.inventory.mapper.StockMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockUpdateNotifier {

    private final SimpMessagingTemplate messagingTemplate;
    private final StockMapper stockMapper;

    /**
     * Broadcasts a stock update to all WebSocket subscribers.
     * Sends to two destinations:
     * - /topic/inventory       (all stock updates)
     * - /topic/inventory/{sku} (updates for a specific product)
     *
     * @param stock the updated stock record
     */
    @CircuitBreaker(name = "websocket", fallbackMethod = "fallbackNotify")
    public void notifyStockUpdate(Stock stock) {
        StockResponseDTO dto = stockMapper.toDTO(stock);
        messagingTemplate.convertAndSend("/topic/inventory", dto);
        messagingTemplate.convertAndSend("/topic/inventory/" + stock.getSku(), dto);
        log.debug("WebSocket notification sent for SKU: {}", stock.getSku());
    }

    private void fallbackNotify(Stock stock, Throwable t) {
        log.warn("WebSocket notification failed for SKU: {}. Cause: {}", stock.getSku(), t.getMessage());
    }
}
