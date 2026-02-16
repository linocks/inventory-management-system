package com.inventory.common.outbox.admin;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OutboxAdminResult {
    int requested;
    int affected;
    long deadEvents;
    long failedEvents;
    long inProgressEvents;
}
