package io.helios.core.history;

import io.helios.core.event.ActivityScheduled;
import io.helios.core.event.Event;
import io.helios.core.event.WorkflowCompleted;
import io.helios.core.event.WorkflowStarted;
import io.helios.core.run.WorkflowRunId;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link InMemoryEventStore}.
 *
 * <p>This store is intentionally single-threaded. No concurrent access is tested
 * or supported in Phase 2. Thread safety is a Phase 3+ concern.
 */
@DisplayName("InMemoryEventStore (single-threaded Phase 2)")
class InMemoryEventStoreTest {

    private InMemoryEventStore store;
    private WorkflowRunId runA;
    private WorkflowRunId runB;

    @BeforeEach
    void setUp() {
        store = new InMemoryEventStore();
        runA = new WorkflowRunId("run-A");
        runB = new WorkflowRunId("run-B");
    }

    // ── Sequence assignment ───────────────────────────────────────────────────

    @Test
    @DisplayName("first append assigns sequences 1 and 2 to two events")
    void firstAppendAssignsSequences1And2() {
        List<Event> events = List.of(
                new WorkflowStarted("loan-approval", null),
                new ActivityScheduled("act-1", "credit-check", null));

        List<RecordedEvent> recorded = store.append(runA, events);

        Assertions.assertThat(recorded).hasSize(2);
        Assertions.assertThat(recorded.get(0).sequence()).isEqualTo(1L);
        Assertions.assertThat(recorded.get(1).sequence()).isEqualTo(2L);
    }

    @Test
    @DisplayName("second append to the same run continues at sequence 3")
    void secondAppendContinuesAtSequence3() {
        store.append(runA, List.of(
                new WorkflowStarted("loan-approval", null),
                new ActivityScheduled("act-1", "credit-check", null)));

        List<RecordedEvent> second = store.append(runA, List.of(new WorkflowCompleted("APPROVED")));

        Assertions.assertThat(second).hasSize(1);
        Assertions.assertThat(second.get(0).sequence()).isEqualTo(3L);
    }

    @Test
    @DisplayName("two run IDs maintain independent sequence counters")
    void runIdsHaveIndependentSequences() {
        store.append(runA, List.of(new WorkflowStarted("loan-approval", null)));
        store.append(runA, List.of(new ActivityScheduled("act-1", "check", null)));

        List<RecordedEvent> runBFirst = store.append(runB,
                List.of(new WorkflowStarted("onboarding", null)));

        Assertions.assertThat(runBFirst.get(0).sequence())
                .as("run B sequence must start at 1, independent of run A")
                .isEqualTo(1L);
    }

    @Test
    @DisplayName("two run IDs maintain independent histories")
    void runIdsHaveIndependentHistories() {
        store.append(runA, List.of(new WorkflowStarted("loan-approval", null)));
        store.append(runB, List.of(new WorkflowStarted("onboarding", null),
                new WorkflowCompleted("done")));

        Assertions.assertThat(store.history(runA).size()).isEqualTo(1);
        Assertions.assertThat(store.history(runB).size()).isEqualTo(2);
    }

    // ── History retrieval ─────────────────────────────────────────────────────

    @Test
    @DisplayName("history for an unknown run returns an empty EventHistory, not null")
    void unknownRunReturnsEmptyHistory() {
        EventHistory history = store.history(new WorkflowRunId("never-seen"));
        Assertions.assertThat(history).isNotNull();
        Assertions.assertThat(history.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("history reflects all appended events in sequence order")
    void historyReflectsAllEvents() {
        Event e1 = new WorkflowStarted("loan-approval", null);
        Event e2 = new ActivityScheduled("act-1", "credit-check", null);
        Event e3 = new WorkflowCompleted("APPROVED");

        store.append(runA, List.of(e1, e2));
        store.append(runA, List.of(e3));

        EventHistory history = store.history(runA);
        Assertions.assertThat(history.size()).isEqualTo(3);
        Assertions.assertThat(history.events().get(0).event()).isEqualTo(e1);
        Assertions.assertThat(history.events().get(1).event()).isEqualTo(e2);
        Assertions.assertThat(history.events().get(2).event()).isEqualTo(e3);
    }

    @Test
    @DisplayName("caller cannot mutate the returned EventHistory to corrupt the store")
    void returnedHistoryIsIsolatedFromInternalState() {
        store.append(runA, List.of(new WorkflowStarted("loan-approval", null)));

        EventHistory snapshot = store.history(runA);
        Assertions.assertThatThrownBy(() -> snapshot.events()
                        .add(new RecordedEvent(99L, new WorkflowCompleted(null))))
                .isInstanceOf(UnsupportedOperationException.class);

        Assertions.assertThat(store.history(runA).size())
                .as("store must have exactly 1 event; caller mutation attempt must not corrupt it")
                .isEqualTo(1);
    }

    // ── Validation ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("appending an empty batch fails with a clear IllegalArgumentException")
    void appendEmptyBatchFails() {
        Assertions.assertThatIllegalArgumentException()
                .isThrownBy(() -> store.append(runA, List.of()))
                .withMessageContaining("events must not be empty");
    }

    @Test
    @DisplayName("null runId is rejected with a clear NullPointerException")
    void nullRunIdRejected() {
        Assertions.assertThatNullPointerException()
                .isThrownBy(() -> store.append(null, List.of(new WorkflowStarted("t", null))))
                .withMessageContaining("runId must not be null");
    }

    @Test
    @DisplayName("null events list is rejected with a clear NullPointerException")
    void nullEventsListRejected() {
        Assertions.assertThatNullPointerException()
                .isThrownBy(() -> store.append(runA, null))
                .withMessageContaining("events must not be null");
    }
}
