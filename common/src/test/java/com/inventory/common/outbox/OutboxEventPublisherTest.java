package com.inventory.common.outbox;

import com.inventory.common.entity.OutboxEvent;
import com.inventory.common.outbox.OutboxStatus;
import com.inventory.common.repository.OutboxRepository;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxEventPublisherTest {

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private OutboxEventPublisher outboxEventPublisher;

    private OutboxEvent createOutboxEvent(String eventId, String topic, String key, String payload) {
        return OutboxEvent.builder()
                .id(1L)
                .eventId(eventId)
                .eventType("PRODUCT_CREATED")
                .topic(topic)
                .eventKey(key)
                .payload(payload)
                .processed(false)
                .status(OutboxStatus.PENDING)
                .build();
    }

    @Test
    @DisplayName("should publish event to Kafka and mark as processed")
    void shouldPublishAndMarkProcessed() {
        OutboxEvent event = createOutboxEvent("evt-1", "inventory.product.created", "PROD-001", "{\"sku\":\"PROD-001\"}");
        when(outboxRepository.findClaimableEvents(any(), any()))
                .thenReturn(new PageImpl<>(List.of(event)));
        when(outboxRepository.claimEvent(eq(1L), any())).thenReturn(1);

        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(
                new SendResult<>(new ProducerRecord<>("inventory.product.created", "PROD-001", "{}"),
                        new RecordMetadata(null, 0, 0, 0, 0, 0)));
        when(kafkaTemplate.send("inventory.product.created", "PROD-001", "{\"sku\":\"PROD-001\"}")).thenReturn(future);

        outboxEventPublisher.publishOutboxEvents();

        verify(outboxRepository).markProcessed(eq(1L), any());
    }

    @Test
    @DisplayName("should mark event failed when Kafka publish fails")
    void shouldMarkFailedWhenPublishFails() {
        OutboxEvent event = createOutboxEvent("evt-1", "topic", "key1", "payload1");
        when(outboxRepository.findClaimableEvents(any(), any()))
                .thenReturn(new PageImpl<>(List.of(event)));
        when(outboxRepository.claimEvent(eq(1L), any())).thenReturn(1);

        CompletableFuture<SendResult<String, String>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Kafka unavailable"));
        when(kafkaTemplate.send("topic", "key1", "payload1")).thenReturn(failedFuture);

        outboxEventPublisher.publishOutboxEvents();

        verify(kafkaTemplate, times(1)).send(any(), any(), any());
        verify(outboxRepository).markFailed(eq(1L), eq(1), any(), any());
        verify(outboxRepository, never()).markProcessed(anyLong(), any());
    }
}
