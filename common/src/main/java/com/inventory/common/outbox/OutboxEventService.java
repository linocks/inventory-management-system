package com.inventory.common.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.common.entity.OutboxEvent;
import com.inventory.common.event.BaseEvent;
import com.inventory.common.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "outbox.enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
@Slf4j
public class OutboxEventService {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    /**
     * Saves an event to the outbox table. This method should be called within the same
     * transaction as the business data write, ensuring atomicity. The event will be
     * picked up by the OutboxEventPublisher and sent to Kafka.
     *
     * @param topic the Kafka topic to publish to
     * @param key   the Kafka message key (e.g., SKU for ordering)
     * @param event the event to publish
     */
    public void saveEvent(String topic, String key, BaseEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .eventId(event.getEventId())
                    .eventType(event.getEventType().name())
                    .topic(topic)
                    .eventKey(key)
                    .payload(payload)
                    .build();

            outboxRepository.save(outboxEvent);
            log.debug("Event saved to outbox: eventId={}, topic={}", event.getEventId(), topic);
        } catch (JsonProcessingException ex) {
            throw new RuntimeException("Failed to serialize event for outbox", ex);
        }
    }
}
