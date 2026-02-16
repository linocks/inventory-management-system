package com.inventory.common.outbox;

import com.inventory.common.entity.OutboxEvent;
import com.inventory.common.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@ConditionalOnProperty(name = "outbox.enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
public class OutboxEventPublisher implements ApplicationListener<ContextClosedEvent> {

    private static final Logger log = LoggerFactory.getLogger(OutboxEventPublisher.class);

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);
    private final AtomicInteger inFlightPublishes = new AtomicInteger(0);

    @Value("${outbox.publisher.enabled:true}")
    private boolean publisherEnabled = true;

    @Value("${outbox.batch-size:100}")
    private int batchSize = 100;

    @Value("${outbox.max-in-flight:200}")
    private int maxInFlight = 200;

    @Value("${outbox.max-retries:10}")
    private int maxRetries = 10;

    @Value("${outbox.base-retry-delay-ms:1000}")
    private long baseRetryDelayMs = 1000L;

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        shuttingDown.set(true);
    }

    /**
     * Polls the outbox table for unprocessed events, publishes them to Kafka,
     * and marks them as processed. If Kafka send fails, the event stays in
     * the outbox and is retried on the next poll.
     *
     * <p>The polling interval is configurable via {@code outbox.poll-interval-ms}
     * in application.yml. Defaults to 2000ms.</p>
     */
    @Scheduled(fixedDelayString = "${outbox.poll-interval-ms:2000}")
    public void publishOutboxEvents() {
        if (!publisherEnabled || shuttingDown.get()) {
            return;
        }

        int availableSlots = Math.max(0, maxInFlight - inFlightPublishes.get());
        if (availableSlots == 0) {
            return;
        }

        try {
            List<OutboxEvent> events = outboxRepository
                    .findClaimableEvents(LocalDateTime.now(), PageRequest.of(0, Math.min(batchSize, availableSlots)))
                    .getContent();

            for (OutboxEvent event : events) {
                if (shuttingDown.get()) {
                    return;
                }

                if (inFlightPublishes.get() >= maxInFlight) {
                    break;
                }

                if (outboxRepository.claimEvent(event.getId(), LocalDateTime.now()) == 0) {
                    continue;
                }

                inFlightPublishes.incrementAndGet();

                kafkaTemplate.send(event.getTopic(), event.getEventKey(), event.getPayload())
                        .whenComplete((result, ex) -> {
                            try {
                                if (ex == null) {
                                    outboxRepository.markProcessed(event.getId(), LocalDateTime.now());
                                    log.info("Outbox event published: eventId={}, topic={}",
                                            event.getEventId(), event.getTopic());
                                    return;
                                }

                                String error = truncateError(ex.getMessage());
                                int nextRetryCount = event.getRetryCount() + 1;

                                if (nextRetryCount >= maxRetries) {
                                    outboxRepository.markDead(event.getId(), nextRetryCount, error);
                                    log.error("Outbox event moved to DEAD state: eventId={}, topic={}, retries={}",
                                            event.getEventId(), event.getTopic(), nextRetryCount);
                                } else {
                                    LocalDateTime nextAttemptAt = LocalDateTime.now()
                                            .plusNanos(computeBackoffDelayMs(nextRetryCount) * 1_000_000);
                                    outboxRepository.markFailed(event.getId(), nextRetryCount, nextAttemptAt, error);
                                    log.warn("Outbox publish failed: eventId={}, topic={}, retry={}, nextAttemptAt={}, error={}",
                                            event.getEventId(), event.getTopic(), nextRetryCount, nextAttemptAt, error);
                                }
                            } catch (Exception callbackEx) {
                                log.error("Failed to update outbox publish state for eventId={}: {}",
                                        event.getEventId(), callbackEx.getMessage(), callbackEx);
                            } finally {
                                inFlightPublishes.decrementAndGet();
                            }
                        });
            }
        } catch (DataAccessResourceFailureException | CannotAcquireLockException ex) {
            if (shuttingDown.get()) {
                log.debug("Skipping outbox poll during shutdown: {}", ex.getMessage());
                return;
            }
            log.warn("Outbox poll failed due to transient data access issue: {}", ex.getMessage());
        }
    }

    private long computeBackoffDelayMs(int retryCount) {
        long multiplier = 1L << Math.min(retryCount - 1, 6);
        return baseRetryDelayMs * multiplier;
    }

    private String truncateError(String error) {
        if (error == null || error.isBlank()) {
            return "Unknown publish error";
        }
        if (error.length() <= 1000) {
            return error;
        }
        return error.substring(0, 1000);
    }
}
