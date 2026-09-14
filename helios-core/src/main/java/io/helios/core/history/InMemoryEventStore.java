package io.helios.core.history;

import io.helios.core.event.Event;
import io.helios.core.run.WorkflowRunId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Append-only, in-memory store for workflow run event histories.
 *
 * <p>Each {@link WorkflowRunId} has its own independent sequence counter starting
 * at 1. Sequence numbers are gap-free and assigned here, not by callers. This
 * guarantees that every run's history is a contiguous, ordered record regardless
 * of which code appended events or in what order callers construct their event lists.
 *
 * <p><strong>Phase 2 scope:</strong> this store is intentionally single-threaded.
 * Concurrent access is not tested, not supported, and not documented. Durability,
 * optimistic locking, and multi-instance coordination are Phase 3+ concerns.
 */
public final class InMemoryEventStore {

    /**
     * Internal storage: mutable per-run lists are kept private and never exposed.
     * Callers always receive defensive copies.
     */
    private final Map<WorkflowRunId, List<RecordedEvent>> store = new HashMap<>();

    /**
     * Appends a non-empty batch of events to the history for the given run.
     *
     * <p>Sequence numbers are assigned starting from {@code (current size + 1)},
     * ensuring gap-free, monotonically increasing sequences per run.
     *
     * @param runId  the run to append to; must not be null
     * @param events the events to append in supplied order; must not be null or empty
     * @return an unmodifiable list of the newly recorded events with their sequences
     * @throws NullPointerException     if {@code runId} or {@code events} is null
     * @throws IllegalArgumentException if {@code events} is empty
     */
    public List<RecordedEvent> append(WorkflowRunId runId, List<Event> events) {
        Objects.requireNonNull(runId, "runId must not be null");
        Objects.requireNonNull(events, "events must not be null");
        if (events.isEmpty()) {
            throw new IllegalArgumentException("events must not be empty");
        }

        List<RecordedEvent> existing = store.computeIfAbsent(runId, k -> new ArrayList<>());
        long nextSequence = existing.size() + 1L;

        List<RecordedEvent> appended = new ArrayList<>(events.size());
        for (Event event : events) {
            RecordedEvent recorded = new RecordedEvent(nextSequence++, event);
            existing.add(recorded);
            appended.add(recorded);
        }
        return List.copyOf(appended); // unmodifiable; internal list is separate
    }

    /**
     * Returns a snapshot of the event history for the given run.
     *
     * <p>If the run has never been appended to, an empty {@link EventHistory} is
     * returned rather than null. Callers need not null-check the result.
     *
     * <p>The returned history is a snapshot at the moment of the call. Subsequent
     * appends do not retroactively update it.
     *
     * @param runId the run to retrieve history for; must not be null
     * @return an {@link EventHistory} containing all events recorded so far
     * @throws NullPointerException if {@code runId} is null
     */
    public EventHistory history(WorkflowRunId runId) {
        Objects.requireNonNull(runId, "runId must not be null");
        List<RecordedEvent> events = store.getOrDefault(runId, List.of());
        return new EventHistory(runId, events); // constructor makes defensive copy
    }
}
