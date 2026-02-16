package com.inventory.common.event;

/**
 * Shared guardrails for event contract compatibility.
 */
public final class EventContractValidator {

    private static final int SUPPORTED_VERSION = 1;

    private EventContractValidator() {
    }

    public static void validate(BaseEvent event, EventType expectedType, String topic) {
        if (event == null) {
            throw new IllegalArgumentException("Event payload is required");
        }
        if (event.getEventId() == null || event.getEventId().isBlank()) {
            throw new IllegalArgumentException("eventId is required");
        }
        if (event.getTimestamp() == null) {
            throw new IllegalArgumentException("timestamp is required");
        }
        if (event.getEventType() != expectedType) {
            throw new IllegalArgumentException("Unexpected event type for topic " + topic
                    + ": expected=" + expectedType + ", actual=" + event.getEventType());
        }
        Integer version = event.getContractVersion();
        if (version == null || version != SUPPORTED_VERSION) {
            throw new IllegalArgumentException("Unsupported contractVersion for topic " + topic
                    + ": " + version + " (supported: " + SUPPORTED_VERSION + ")");
        }
    }
}
