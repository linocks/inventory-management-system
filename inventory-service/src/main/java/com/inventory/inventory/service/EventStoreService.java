package com.inventory.inventory.service;

import com.inventory.inventory.entity.InventoryEvent;
import com.inventory.inventory.repository.InventoryEventRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Circuit-broken wrapper for MongoDB event storage. If MongoDB is unavailable,
 * the fallback logs a warning instead of failing the stock operation.
 * Events are also captured by the transactional outbox, so no data is lost.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EventStoreService {

    private final InventoryEventRepository eventRepository;

    @CircuitBreaker(name = "mongoEventStore", fallbackMethod = "fallbackSaveEvent")
    public void saveEvent(InventoryEvent event) {
        eventRepository.save(event);
    }

    private void fallbackSaveEvent(InventoryEvent event, Throwable t) {
        log.warn("MongoDB event store unavailable, event not persisted: eventId={}, sku={}. Cause: {}",
                event.getEventId(), event.getSku(), t.getMessage());
    }
}
