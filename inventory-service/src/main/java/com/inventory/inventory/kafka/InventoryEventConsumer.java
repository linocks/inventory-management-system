package com.inventory.inventory.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.common.event.ProductCreatedEvent;
import com.inventory.common.event.ProductDeletedEvent;
import com.inventory.common.event.ProductUpdatedEvent;
import com.inventory.common.event.EventContractValidator;
import com.inventory.common.event.EventType;
import com.inventory.inventory.service.EventInboxService;
import com.inventory.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static com.inventory.common.constants.KafkaConstants.*;

/**
 * Consumes product lifecycle events from Kafka.
 * Exceptions propagate to the DefaultErrorHandler which retries 3 times
 * then publishes to a dead letter topic ({topic}.DLT).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryEventConsumer {

    private final InventoryService inventoryService;
    private final EventInboxService eventInboxService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = TOPIC_PRODUCT_CREATED, groupId = GROUP_INVENTORY_SERVICE)
    @Transactional
    public void handleProductCreated(String message) throws Exception {
        ProductCreatedEvent event = objectMapper.readValue(message, ProductCreatedEvent.class);
        EventContractValidator.validate(event, EventType.PRODUCT_CREATED, TOPIC_PRODUCT_CREATED);
        log.info("Received ProductCreatedEvent: sku={}, eventId={}", event.getSku(), event.getEventId());

        if (!eventInboxService.registerIfFirstSeen(event.getEventId(), TOPIC_PRODUCT_CREATED)) {
            log.warn("Duplicate event detected, skipping: eventId={}", event.getEventId());
            return;
        }

        inventoryService.createStock(event.getProductId(), event.getSku(), event.getInitialStock());
    }

    @KafkaListener(topics = TOPIC_PRODUCT_UPDATED, groupId = GROUP_INVENTORY_SERVICE)
    @Transactional
    public void handleProductUpdated(String message) throws Exception {
        ProductUpdatedEvent event = objectMapper.readValue(message, ProductUpdatedEvent.class);
        EventContractValidator.validate(event, EventType.PRODUCT_UPDATED, TOPIC_PRODUCT_UPDATED);
        log.info("Received ProductUpdatedEvent: sku={}, eventId={}", event.getSku(), event.getEventId());

        if (!eventInboxService.registerIfFirstSeen(event.getEventId(), TOPIC_PRODUCT_UPDATED)) {
            log.warn("Duplicate event detected, skipping: eventId={}", event.getEventId());
            return;
        }

        log.info("Product updated acknowledged: sku={}", event.getSku());
    }

    @KafkaListener(topics = TOPIC_PRODUCT_DELETED, groupId = GROUP_INVENTORY_SERVICE)
    @Transactional
    public void handleProductDeleted(String message) throws Exception {
        ProductDeletedEvent event = objectMapper.readValue(message, ProductDeletedEvent.class);
        EventContractValidator.validate(event, EventType.PRODUCT_DELETED, TOPIC_PRODUCT_DELETED);
        log.info("Received ProductDeletedEvent: sku={}, eventId={}", event.getSku(), event.getEventId());

        if (!eventInboxService.registerIfFirstSeen(event.getEventId(), TOPIC_PRODUCT_DELETED)) {
            log.warn("Duplicate event detected, skipping: eventId={}", event.getEventId());
            return;
        }

        inventoryService.removeStock(event.getSku());
    }
}
