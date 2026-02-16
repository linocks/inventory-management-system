package com.inventory.common.event;

import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Published to {@code inventory.stock.updated} on every stock change
 * (sale, restock, adjustment, initial). The reporting service consumes
 * this to update real-time reports and push WebSocket notifications.
 */
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class StockUpdatedEvent extends BaseEvent {

    private Long productId;
    private String sku;
    private int previousQuantity;
    private int newQuantity;
    private int minThreshold;
    private int changeAmount;
    private StockChangeReason reason;

    @Override
    public EventType getEventType() {
        return EventType.STOCK_UPDATED;
    }

    public enum StockChangeReason {
        SALE,
        RESTOCK,
        ADJUSTMENT,
        RETURN,
        INITIAL
    }
}
