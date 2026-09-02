package io.helios.storage.model;

import io.helios.common.event.EventType;

public record NewEvent(
    EventType eventType,
    EncodedPayload Payload,
    EventAttributes attributes,
    Long scheduledEventId,
    Long startedEventId,
    Long initiatedEventId) {

    public NewEvent {
        Objects.requireNonNull(eventType, "eventType");
        if (attributes == null) {
            attributes = EventAttributes.EMPTY;
        }
    }

    /** An event with no payload, attributes, or causal references. */
    public static NewEvent of(EventType type) {
        return new NewEvent(type, null, EventAttributes.EMPTY, null, null, null);
    }

    /** An event carrying an opaque payload. */
    public static NewEvent of(EventType type, EncodedPayload payload) {
        return new NewEvent(type, payload, EventAttributes.EMPTY, null, null, null);
    }

    /** An event carrying engine-visible attributes and an opaque payload. */
    public static NewEvent of(EventType type, EventAttributes attributes, EncodedPayload payload) {
        return new NewEvent(type, payload, attributes, null, null, null);
    }   
    
}
