package com.inventory.common.event;

import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Published to {@code inventory.product.deleted} when a product is removed.
 * The inventory service consumes this to delete the corresponding stock record.
 */
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class ProductDeletedEvent extends BaseEvent {

    private Long productId;
    private String sku;

    @Override
    public EventType getEventType() {
        return EventType.PRODUCT_DELETED;
    }
}
