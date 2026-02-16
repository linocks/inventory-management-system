package com.inventory.inventory.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * MongoDB document representing a single inventory event in the audit trail.
 * Stored in the {@code inventory_events} collection. Indexed by {@code eventId} (unique)
 * and {@code sku} for efficient querying.
 */
@Document(collection = "inventory_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryEvent {

    @Id
    private String id;

    @Indexed(unique = true)
    private String eventId;

    private String eventType;

    @Indexed
    private String sku;

    private Long productId;
    private int previousQuantity;
    private int newQuantity;
    private int changeAmount;
    private String reason;
    private LocalDateTime timestamp;

    @Builder.Default
    private LocalDateTime processedAt = LocalDateTime.now();
}
