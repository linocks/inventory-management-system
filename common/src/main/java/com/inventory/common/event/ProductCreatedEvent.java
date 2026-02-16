package com.inventory.common.event;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * Published to {@code inventory.product.created} when a new product is created.
 * The inventory service consumes this to auto-create a stock record.
 */
@Getter
@ToString(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class ProductCreatedEvent extends BaseEvent {

    private Long productId;
    private String sku;
    private String name;
    private String category;
    private BigDecimal price;
    private int initialStock;

    @Override
    public EventType getEventType() {
        return EventType.PRODUCT_CREATED;
    }
}
