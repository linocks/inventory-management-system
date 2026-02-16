package com.inventory.inventory.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Tracks consumed Kafka events to provide durable idempotency.
 * A unique eventId guarantees each message is applied at most once.
 */
@Entity
@Table(name = "processed_events", indexes = {
        @Index(name = "idx_processed_events_event_id", columnList = "event_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "processed_event_seq")
    @SequenceGenerator(name = "processed_event_seq", sequenceName = "processed_events_id_seq", allocationSize = 50)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true, length = 255)
    private String eventId;

    @Column(nullable = false, length = 255)
    private String topic;

    @Column(name = "processed_at", nullable = false)
    @Builder.Default
    private LocalDateTime processedAt = LocalDateTime.now();
}

