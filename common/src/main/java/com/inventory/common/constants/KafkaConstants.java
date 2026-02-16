package com.inventory.common.constants;

/**
 * Kafka topic names and consumer group IDs shared across all services.
 */
public final class KafkaConstants {

    private KafkaConstants() {}

    // Topics
    public static final String TOPIC_PRODUCT_CREATED = "inventory.product.created";
    public static final String TOPIC_PRODUCT_UPDATED = "inventory.product.updated";
    public static final String TOPIC_PRODUCT_DELETED = "inventory.product.deleted";
    public static final String TOPIC_STOCK_UPDATED = "inventory.stock.updated";

    // Consumer Groups
    public static final String GROUP_INVENTORY_SERVICE = "inventory-service-group";
    public static final String GROUP_REPORTING_SERVICE = "reporting-service-group";
}
