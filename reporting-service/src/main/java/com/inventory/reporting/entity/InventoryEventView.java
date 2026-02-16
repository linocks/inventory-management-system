package com.inventory.reporting.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "inventory_events")
@Getter
@NoArgsConstructor
public class InventoryEventView {

    @Id
    private String id;

    private String eventId;
    private String eventType;
    private String sku;
    private Long productId;
    private int previousQuantity;
    private int newQuantity;
    private int changeAmount;
    private String reason;
    private LocalDateTime timestamp;
    private LocalDateTime processedAt;
}
