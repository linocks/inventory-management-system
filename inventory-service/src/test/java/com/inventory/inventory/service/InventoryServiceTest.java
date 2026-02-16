package com.inventory.inventory.service;

import com.inventory.common.exception.InsufficientStockException;
import com.inventory.common.exception.ProductNotFoundException;
import com.inventory.common.outbox.OutboxEventService;
import com.inventory.inventory.dto.StockUpdateDTO;
import com.inventory.inventory.entity.Stock;
import com.inventory.inventory.repository.StockRepository;
import com.inventory.inventory.websocket.StockUpdateNotifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.inventory.common.constants.KafkaConstants.TOPIC_STOCK_UPDATED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private StockRepository stockRepository;

    @Mock
    private EventStoreService eventStoreService;

    @Mock
    private OutboxEventService outboxEventService;

    @Mock
    private StockUpdateNotifier stockUpdateNotifier;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    private Stock stock;

    @BeforeEach
    void setUp() {
        stock = Stock.builder()
                .id(1L)
                .productId(100L)
                .sku("PROD-001")
                .quantity(50)
                .minThreshold(10)
                .build();
    }

    @Nested
    @DisplayName("createStock")
    class CreateStock {

        @Test
        @DisplayName("should create new stock record")
        void shouldCreateNewStockRecord() {
            when(stockRepository.existsBySku("PROD-001")).thenReturn(false);
            when(stockRepository.save(any(Stock.class))).thenReturn(stock);

            Stock result = inventoryService.createStock(100L, "PROD-001", 50);

            assertThat(result.getSku()).isEqualTo("PROD-001");
            assertThat(result.getQuantity()).isEqualTo(50);
            verify(stockRepository).save(any(Stock.class));
            verify(eventStoreService).saveEvent(any());
            verify(outboxEventService).saveEvent(eq(TOPIC_STOCK_UPDATED), eq("PROD-001"), any());
            verify(stockUpdateNotifier).notifyStockUpdate(stock);
        }
    }

    @Nested
    @DisplayName("getStockBySku")
    class GetStockBySku {

        @Test
        @DisplayName("should return stock when found")
        void shouldReturnStock() {
            when(stockRepository.findBySku("PROD-001")).thenReturn(Optional.of(stock));

            Stock result = inventoryService.getStockBySku("PROD-001");

            assertThat(result.getSku()).isEqualTo("PROD-001");
        }

        @Test
        @DisplayName("should throw when SKU not found")
        void shouldThrowWhenNotFound() {
            when(stockRepository.findBySku("NONEXISTENT")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> inventoryService.getStockBySku("NONEXISTENT"))
                    .isInstanceOf(ProductNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("restock")
    class Restock {

        @Test
        @DisplayName("should add quantity to existing stock")
        void shouldAddQuantity() {
            StockUpdateDTO dto = StockUpdateDTO.builder().quantity(20).build();
            Stock updatedStock = Stock.builder()
                    .id(1L).productId(100L).sku("PROD-001").quantity(70).build();

            when(stockRepository.findBySku("PROD-001")).thenReturn(Optional.of(stock));
            when(stockRepository.save(any(Stock.class))).thenReturn(updatedStock);

            Stock result = inventoryService.restock("PROD-001", dto);

            assertThat(result.getQuantity()).isEqualTo(70);
            verify(outboxEventService).saveEvent(eq(TOPIC_STOCK_UPDATED), eq("PROD-001"), any());
            verify(stockUpdateNotifier).notifyStockUpdate(updatedStock);
        }
    }

    @Nested
    @DisplayName("sell")
    class Sell {

        @Test
        @DisplayName("should deduct quantity when sufficient stock")
        void shouldDeductQuantity() {
            StockUpdateDTO dto = StockUpdateDTO.builder().quantity(10).build();
            Stock updatedStock = Stock.builder()
                    .id(1L).productId(100L).sku("PROD-001").quantity(40).build();

            when(stockRepository.findBySku("PROD-001")).thenReturn(Optional.of(stock));
            when(stockRepository.save(any(Stock.class))).thenReturn(updatedStock);

            Stock result = inventoryService.sell("PROD-001", dto);

            assertThat(result.getQuantity()).isEqualTo(40);
            verify(outboxEventService).saveEvent(eq(TOPIC_STOCK_UPDATED), eq("PROD-001"), any());
        }

        @Test
        @DisplayName("should throw InsufficientStockException when not enough stock")
        void shouldThrowWhenInsufficientStock() {
            StockUpdateDTO dto = StockUpdateDTO.builder().quantity(100).build();
            when(stockRepository.findBySku("PROD-001")).thenReturn(Optional.of(stock));

            assertThatThrownBy(() -> inventoryService.sell("PROD-001", dto))
                    .isInstanceOf(InsufficientStockException.class)
                    .hasMessageContaining("PROD-001")
                    .hasMessageContaining("100")
                    .hasMessageContaining("50");
        }
    }

    @Nested
    @DisplayName("adjust")
    class Adjust {

        @Test
        @DisplayName("should set stock to exact quantity")
        void shouldSetExactQuantity() {
            StockUpdateDTO dto = StockUpdateDTO.builder().quantity(75).build();
            Stock updatedStock = Stock.builder()
                    .id(1L).productId(100L).sku("PROD-001").quantity(75).build();

            when(stockRepository.findBySku("PROD-001")).thenReturn(Optional.of(stock));
            when(stockRepository.save(any(Stock.class))).thenReturn(updatedStock);

            Stock result = inventoryService.adjust("PROD-001", dto);

            assertThat(result.getQuantity()).isEqualTo(75);
            verify(outboxEventService).saveEvent(eq(TOPIC_STOCK_UPDATED), eq("PROD-001"), any());
        }
    }
}
