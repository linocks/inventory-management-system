package com.inventory.common.repository;

import com.inventory.common.entity.OutboxEvent;
import com.inventory.common.outbox.OutboxStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Spring Data JPA repository for {@link OutboxEvent} entities.
 * Used by the outbox poller to fetch and publish unprocessed events.
 */
@Repository
public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {

    @Query("""
            SELECT e FROM OutboxEvent e
            WHERE e.status = com.inventory.common.outbox.OutboxStatus.PENDING
               OR (e.status = com.inventory.common.outbox.OutboxStatus.FAILED
                   AND (e.nextAttemptAt IS NULL OR e.nextAttemptAt <= :now))
            ORDER BY e.createdAt ASC
            """)
    Page<OutboxEvent> findClaimableEvents(@Param("now") LocalDateTime now, Pageable pageable);

    Page<OutboxEvent> findByStatusOrderByCreatedAtAsc(OutboxStatus status, Pageable pageable);

    List<OutboxEvent> findByStatusAndClaimedAtBefore(OutboxStatus status, LocalDateTime cutoff);

    long countByProcessedFalse();

    long countByStatus(OutboxStatus status);

    @Modifying
    @Transactional
    @Query("""
            UPDATE OutboxEvent e
            SET e.status = com.inventory.common.outbox.OutboxStatus.IN_PROGRESS,
                e.claimedAt = :claimedAt
            WHERE e.id = :id
              AND (e.status = com.inventory.common.outbox.OutboxStatus.PENDING
                   OR (e.status = com.inventory.common.outbox.OutboxStatus.FAILED
                       AND (e.nextAttemptAt IS NULL OR e.nextAttemptAt <= :claimedAt)))
            """)
    int claimEvent(@Param("id") Long id, @Param("claimedAt") LocalDateTime claimedAt);

    @Modifying
    @Transactional
    @Query("""
            UPDATE OutboxEvent e
            SET e.status = com.inventory.common.outbox.OutboxStatus.PROCESSED,
                e.processed = true,
                e.processedAt = :processedAt,
                e.claimedAt = NULL,
                e.lastError = NULL
            WHERE e.id = :id
            """)
    int markProcessed(@Param("id") Long id, @Param("processedAt") LocalDateTime processedAt);

    @Modifying
    @Transactional
    @Query("""
            UPDATE OutboxEvent e
            SET e.status = com.inventory.common.outbox.OutboxStatus.FAILED,
                e.processed = false,
                e.retryCount = :retryCount,
                e.nextAttemptAt = :nextAttemptAt,
                e.claimedAt = NULL,
                e.lastError = :lastError
            WHERE e.id = :id
            """)
    int markFailed(@Param("id") Long id,
                   @Param("retryCount") int retryCount,
                   @Param("nextAttemptAt") LocalDateTime nextAttemptAt,
                   @Param("lastError") String lastError);

    @Modifying
    @Transactional
    @Query("""
            UPDATE OutboxEvent e
            SET e.status = com.inventory.common.outbox.OutboxStatus.DEAD,
                e.processed = false,
                e.retryCount = :retryCount,
                e.claimedAt = NULL,
                e.lastError = :lastError
            WHERE e.id = :id
            """)
    int markDead(@Param("id") Long id,
                 @Param("retryCount") int retryCount,
                 @Param("lastError") String lastError);

    @Modifying
    @Transactional
    @Query("""
            UPDATE OutboxEvent e
            SET e.status = com.inventory.common.outbox.OutboxStatus.PENDING,
                e.retryCount = 0,
                e.nextAttemptAt = :nextAttemptAt,
                e.claimedAt = NULL,
                e.lastError = NULL
            WHERE e.id IN :ids
            """)
    int resetToPending(@Param("ids") List<Long> ids, @Param("nextAttemptAt") LocalDateTime nextAttemptAt);
}
