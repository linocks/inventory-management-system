package com.inventory.inventory.service;

import com.inventory.inventory.entity.ProcessedEvent;
import com.inventory.inventory.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Durable inbox for consumed Kafka events.
 */
@Service
@RequiredArgsConstructor
public class EventInboxService {

    private final ProcessedEventRepository processedEventRepository;

    /**
     * Returns true when this event is seen for the first time.
     * Returns false when another transaction already processed it.
     */
    public boolean registerIfFirstSeen(String eventId, String topic) {
        try {
            processedEventRepository.save(ProcessedEvent.builder()
                    .eventId(eventId)
                    .topic(topic)
                    .build());
            return true;
        } catch (DataIntegrityViolationException ex) {
            return false;
        }
    }
}

