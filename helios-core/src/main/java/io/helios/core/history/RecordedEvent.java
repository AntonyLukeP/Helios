package io.helios.core.history;

import io.helios.core.event.Event;
import java.util.Objects;

/**
 * An event as it exists in a workflow run history: the raw event fact paired with its
 * position in the run's ordered sequence.
 *
 * <p>{@code RecordedEvent} is a value object. Two instances with the same sequence and
 * event are considered identical. Java records provide this automatically.
 *
 * <p>The sequence number is assigned by {@link InMemoryEventStore}, not by the caller
 * or the event itself. This keeps event types free of ordering concerns.
 *
 * @param sequence the 1-based, gap-free position of this event in the run history;
 *                 must be positive
 * @param event    the event fact; must not be null
 */
public record RecordedEvent(long sequence, Event event) {

    /** Compact constructor — validates invariants on every construction path. */
    public RecordedEvent {
        if (sequence <= 0) {
            throw new IllegalArgumentException(
                    "sequence must be positive, got: " + sequence);
        }
        Objects.requireNonNull(event, "event must not be null");
    }
}
