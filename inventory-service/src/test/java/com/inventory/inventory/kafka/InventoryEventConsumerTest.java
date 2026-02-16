package com.inventory.inventory.kafka;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.inventory.common.event.ProductCreatedEvent;
import com.inventory.common.event.ProductDeletedEvent;
import com.inventory.inventory.service.EventInboxService;
import com.inventory.inventory.service.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryEventConsumerTest {

    @Mock
    private InventoryService inventoryService;

    @Mock
    private EventInboxService eventInboxService;

    private ObjectMapper objectMapper;
    private InventoryEventConsumer consumer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        consumer = new InventoryEventConsumer(inventoryService, eventInboxService, objectMapper);
    }

    @Nested
    @DisplayName("handleProductCreated")
    class HandleProductCreated {

        private ProductCreatedEvent event;
        private String message;

        @BeforeEach
        void setUp() throws Exception {
            event = ProductCreatedEvent.builder()
                    .productId(1L)
                    .sku("PROD-001")
                    .name("Test Product")
                    .category("Electronics")
                    .price(new BigDecimal("29.99"))
                    .initialStock(50)
                    .build();
            message = objectMapper.writeValueAsString(event);
        }

        @Test
        @DisplayName("should create stock for new event")
        void shouldCreateStock() throws Exception {
            when(eventInboxService.registerIfFirstSeen(event.getEventId(), "inventory.product.created")).thenReturn(true);

            consumer.handleProductCreated(message);

            verify(inventoryService).createStock(1L, "PROD-001", 50);
        }

        @Test
        @DisplayName("should skip duplicate event")
        void shouldSkipDuplicate() throws Exception {
            when(eventInboxService.registerIfFirstSeen(event.getEventId(), "inventory.product.created")).thenReturn(false);

            consumer.handleProductCreated(message);

            verify(inventoryService, never()).createStock(anyLong(), anyString(), anyInt());
        }
    }

    @Nested
    @DisplayName("handleProductDeleted")
    class HandleProductDeleted {

        @Test
        @DisplayName("should remove stock for deleted product")
        void shouldRemoveStock() throws Exception {
            ProductDeletedEvent event = ProductDeletedEvent.builder()
                    .productId(1L)
                    .sku("PROD-001")
                    .build();
            String message = objectMapper.writeValueAsString(event);
            when(eventInboxService.registerIfFirstSeen(event.getEventId(), "inventory.product.deleted")).thenReturn(true);

            consumer.handleProductDeleted(message);

            verify(inventoryService).removeStock("PROD-001");
        }
    }
}
