package com.inventory.inventory.websocket;

import com.inventory.inventory.dto.StockResponseDTO;
import com.inventory.inventory.entity.Stock;
import com.inventory.inventory.mapper.StockMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockUpdateNotifierTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private StockMapper stockMapper;

    @InjectMocks
    private StockUpdateNotifier stockUpdateNotifier;

    @Test
    @DisplayName("should send to both global and SKU-specific topics in one call")
    void shouldSendToBothTopics() {
        Stock stock = Stock.builder()
                .id(2L)
                .productId(200L)
                .sku("ELEC-042")
                .quantity(0)
                .build();
        StockResponseDTO dto = StockResponseDTO.builder()
                .id(2L).productId(200L).sku("ELEC-042").quantity(0).build();
        when(stockMapper.toDTO(stock)).thenReturn(dto);

        stockUpdateNotifier.notifyStockUpdate(stock);

        verify(messagingTemplate).convertAndSend("/topic/inventory", dto);
        verify(messagingTemplate).convertAndSend("/topic/inventory/ELEC-042", dto);
    }
}
