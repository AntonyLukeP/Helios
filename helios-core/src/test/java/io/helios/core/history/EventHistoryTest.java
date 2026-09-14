package io.helios.core.history;

import io.helios.core.event.WorkflowStarted;
import io.helios.core.event.ActivityScheduled;
import io.helios.core.run.WorkflowRunId;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("EventHistory")
class EventHistoryTest {

    private final WorkflowRunId runId = new WorkflowRunId("run-test");
    private final RecordedEvent re1 = new RecordedEvent(1L, new WorkflowStarted("loan-approval", null));
    private final RecordedEvent re2 = new RecordedEvent(2L, new ActivityScheduled("act-1", "credit-check", null));

    @Test
    @DisplayName("empty history has size 0 and isEmpty true")
    void emptyHistoryIsEmpty() {
        EventHistory history = new EventHistory(runId, List.of());
        Assertions.assertThat(history.isEmpty()).isTrue();
        Assertions.assertThat(history.size()).isZero();
    }

    @Test
    @DisplayName("history with two events has size 2")
    void historyWithEventsHasCorrectSize() {
        EventHistory history = new EventHistory(runId, List.of(re1, re2));
        Assertions.assertThat(history.size()).isEqualTo(2);
    }

    @Test
    @DisplayName("events are returned in the order they were supplied")
    void eventsReturnedInOrder() {
        EventHistory history = new EventHistory(runId, List.of(re1, re2));
        Assertions.assertThat(history.events()).containsExactly(re1, re2);
    }

    @Test
    @DisplayName("runId matches the run the history belongs to")
    void runIdIsPreserved() {
        EventHistory history = new EventHistory(runId, List.of(re1));
        Assertions.assertThat(history.runId()).isEqualTo(runId);
    }

    @Test
    @DisplayName("returned events list is unmodifiable")
    void eventsListIsUnmodifiable() {
        EventHistory history = new EventHistory(runId, List.of(re1));
        Assertions.assertThatThrownBy(() -> history.events().add(re2))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
