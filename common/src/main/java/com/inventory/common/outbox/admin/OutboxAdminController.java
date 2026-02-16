package com.inventory.common.outbox.admin;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Operational endpoints for replaying and reconciling outbox records.
 * Active only in services that produce outbox events.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/outbox/admin")
@ConditionalOnProperty(name = "outbox.enabled", havingValue = "true", matchIfMissing = false)
public class OutboxAdminController {

    private final OutboxAdminService outboxAdminService;

    @PostMapping("/replay")
    @Operation(summary = "Replay dead outbox events", description = "Moves DEAD outbox records back to PENDING for republish.")
    public ResponseEntity<OutboxAdminResult> replay(@RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(outboxAdminService.replayDeadEvents(limit));
    }

    @PostMapping("/reconcile")
    @Operation(summary = "Reconcile stale in-progress outbox events",
            description = "Moves stale IN_PROGRESS records back to PENDING when a publish worker died mid-flight.")
    public ResponseEntity<OutboxAdminResult> reconcile(
            @RequestParam(defaultValue = "120") int olderThanSeconds,
            @RequestParam(defaultValue = "500") int limit) {
        return ResponseEntity.ok(outboxAdminService.reconcileStaleInProgress(olderThanSeconds, limit));
    }
}
