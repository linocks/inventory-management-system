package com.inventory.common.outbox.admin;

import com.inventory.common.entity.OutboxEvent;
import com.inventory.common.outbox.OutboxStatus;
import com.inventory.common.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "outbox.enabled", havingValue = "true", matchIfMissing = false)
public class OutboxAdminService {

    private final OutboxRepository outboxRepository;

    @Transactional
    public OutboxAdminResult replayDeadEvents(int limit) {
        int boundedLimit = Math.max(1, Math.min(limit, 1000));
        List<Long> deadIds = outboxRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.DEAD, PageRequest.of(0, boundedLimit))
                .map(OutboxEvent::getId)
                .getContent();

        int affected = deadIds.isEmpty()
                ? 0
                : outboxRepository.resetToPending(deadIds, LocalDateTime.now());

        return buildResult(boundedLimit, affected);
    }

    @Transactional
    public OutboxAdminResult reconcileStaleInProgress(int olderThanSeconds, int limit) {
        int boundedLimit = Math.max(1, Math.min(limit, 1000));
        int boundedSeconds = Math.max(5, olderThanSeconds);
        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(boundedSeconds);

        List<Long> staleIds = outboxRepository.findByStatusAndClaimedAtBefore(OutboxStatus.IN_PROGRESS, cutoff).stream()
                .limit(boundedLimit)
                .map(OutboxEvent::getId)
                .toList();

        int affected = staleIds.isEmpty()
                ? 0
                : outboxRepository.resetToPending(staleIds, LocalDateTime.now());

        return buildResult(boundedLimit, affected);
    }

    private OutboxAdminResult buildResult(int requested, int affected) {
        return OutboxAdminResult.builder()
                .requested(requested)
                .affected(affected)
                .deadEvents(outboxRepository.countByStatus(OutboxStatus.DEAD))
                .failedEvents(outboxRepository.countByStatus(OutboxStatus.FAILED))
                .inProgressEvents(outboxRepository.countByStatus(OutboxStatus.IN_PROGRESS))
                .build();
    }
}
