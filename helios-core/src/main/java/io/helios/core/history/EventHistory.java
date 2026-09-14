package io.helios.core.history;

import io.helios.core.run.WorkflowRunId;
import java.util.List;
import java.util.Objects;

/**
 * An immutable, ordered snapshot of all events recorded for one workflow run.
 *
 * <p>Instances are created exclusively by {@link InMemoryEventStore}. The constructor
 * is package-private to enforce that invariant.
 *
 * <p>All list accessors return unmodifiable views. Callers cannot add, remove, or
 * replace entries through this class.
 *
 * <p>This is a Phase 2, in-memory learning component. It has no persistence, no
 * concurrency protection, and no serialization support.
 */
public final class EventHistory {

    private final WorkflowRunId runId;
    private final List<RecordedEvent> events;

    /**
     * Package-private: only {@link InMemoryEventStore} creates histories.
     *
     * @param runId  the run this history belongs to
     * @param events the ordered events to snapshot; a defensive copy is taken
     */
    EventHistory(WorkflowRunId runId, List<RecordedEvent> events) {
        this.runId = Objects.requireNonNull(runId, "runId must not be null");
        this.events = List.copyOf(events); // unmodifiable defensive copy
    }

    /** Returns the run this history belongs to. */
    public WorkflowRunId runId() {
        return runId;
    }

    /**
     * Returns all recorded events for this run in sequence order.
     *
     * <p>The returned list is unmodifiable. Mutations throw
     * {@link UnsupportedOperationException}.
     */
    public List<RecordedEvent> events() {
        return events;
    }

    /** Returns the number of events in this history. */
    public int size() {
        return events.size();
    }

    /** Returns {@code true} if no events have been recorded for this run. */
    public boolean isEmpty() {
        return events.isEmpty();
    }

    @Override
    public String toString() {
        return "EventHistory{runId=" + runId.value() + ", size=" + events.size() + "}";
    }
}
