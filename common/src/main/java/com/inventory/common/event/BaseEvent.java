package com.inventory.common.event;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Abstract base class for all domain events published to Kafka.
 * Provides a unique event ID for idempotency and a timestamp.
 */
@Getter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public abstract class BaseEvent {

    @EqualsAndHashCode.Include
    @Builder.Default
    private final String eventId = UUID.randomUUID().toString();

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    @Builder.Default
    private Integer contractVersion = 1;

    public abstract EventType getEventType();
}
