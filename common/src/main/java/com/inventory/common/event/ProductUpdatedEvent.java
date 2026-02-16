package com.inventory.common.event;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * Published to {@code inventory.product.updated} when a product is modified.
 * Contains the updated product fields.
 */
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class ProductUpdatedEvent extends BaseEvent {

    private Long productId;
    private String sku;
    private String name;
    private String category;
    private BigDecimal price;

    @Override
    public EventType getEventType() {
        return EventType.PRODUCT_UPDATED;
    }
}
