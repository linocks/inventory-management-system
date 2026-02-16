package com.inventory.common.outbox;

/**
 * Lifecycle states for transactional outbox records.
 */
public enum OutboxStatus {
    PENDING,
    IN_PROGRESS,
    FAILED,
    DEAD,
    PROCESSED
}
