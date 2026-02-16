package com.inventory.common.outbox;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.inventory.common.entity.OutboxEvent;
import com.inventory.common.event.ProductCreatedEvent;
import com.inventory.common.repository.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OutboxEventServiceTest {

    @Mock
    private OutboxRepository outboxRepository;

    private ObjectMapper objectMapper;
    private OutboxEventService outboxEventService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        outboxEventService = new OutboxEventService(outboxRepository, objectMapper);
    }

    @Test
    @DisplayName("should serialize event and save to outbox table")
    void shouldSerializeAndSave() {
        ProductCreatedEvent event = ProductCreatedEvent.builder()
                .productId(1L)
                .sku("PROD-001")
                .name("Test Product")
                .category("Electronics")
                .price(new BigDecimal("29.99"))
                .initialStock(50)
                .build();

        outboxEventService.saveEvent("inventory.product.created", "PROD-001", event);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxRepository).save(captor.capture());

        OutboxEvent saved = captor.getValue();
        assertThat(saved.getEventId()).isEqualTo(event.getEventId());
        assertThat(saved.getEventType()).isEqualTo("PRODUCT_CREATED");
        assertThat(saved.getTopic()).isEqualTo("inventory.product.created");
        assertThat(saved.getEventKey()).isEqualTo("PROD-001");
        assertThat(saved.getPayload()).contains("PROD-001");
        assertThat(saved.getPayload()).contains("29.99");
        assertThat(saved.isProcessed()).isFalse();
    }
}
