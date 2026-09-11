package io.helios.core.event;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Event sealed interface")
class EventTest {

    // ── WorkflowStarted ───────────────────────────────────────────────────────

    @Test
    @DisplayName("WorkflowStarted stores workflowType and input")
    void workflowStartedStoresFields() {
        WorkflowStarted event = new WorkflowStarted("loan-approval", "{\"amount\":50000}");
        Assertions.assertThat(event.workflowType()).isEqualTo("loan-approval");
        Assertions.assertThat(event.input()).isEqualTo("{\"amount\":50000}");
    }

    @Test
    @DisplayName("WorkflowStarted permits null input")
    void workflowStartedPermitsNullInput() {
        Assertions.assertThatNoException()
                .isThrownBy(() -> new WorkflowStarted("loan-approval", null));
    }

    @Test
    @DisplayName("two WorkflowStarted records with same fields are equal")
    void workflowStartedValueEquality() {
        WorkflowStarted a = new WorkflowStarted("loan-approval", "{}");
        WorkflowStarted b = new WorkflowStarted("loan-approval", "{}");
        Assertions.assertThat(a).isEqualTo(b);
    }

    // ── ActivityScheduled ─────────────────────────────────────────────────────

    @Test
    @DisplayName("ActivityScheduled stores activityId, activityType, and input")
    void activityScheduledStoresFields() {
        ActivityScheduled event = new ActivityScheduled("act-1", "run-credit-check", "{\"id\":42}");
        Assertions.assertThat(event.activityId()).isEqualTo("act-1");
        Assertions.assertThat(event.activityType()).isEqualTo("run-credit-check");
        Assertions.assertThat(event.input()).isEqualTo("{\"id\":42}");
    }

    @Test
    @DisplayName("ActivityScheduled permits null input")
    void activityScheduledPermitsNullInput() {
        Assertions.assertThatNoException()
                .isThrownBy(() -> new ActivityScheduled("act-1", "run-credit-check", null));
    }

    @Test
    @DisplayName("two ActivityScheduled records with same fields are equal")
    void activityScheduledValueEquality() {
        ActivityScheduled a = new ActivityScheduled("act-1", "run-credit-check", null);
        ActivityScheduled b = new ActivityScheduled("act-1", "run-credit-check", null);
        Assertions.assertThat(a).isEqualTo(b);
    }

    // ── WorkflowCompleted ─────────────────────────────────────────────────────

    @Test
    @DisplayName("WorkflowCompleted stores a non-null result")
    void workflowCompletedStoresResult() {
        WorkflowCompleted event = new WorkflowCompleted("APPROVED");
        Assertions.assertThat(event.result()).isEqualTo("APPROVED");
    }

    @Test
    @DisplayName("WorkflowCompleted permits null result")
    void workflowCompletedPermitsNullResult() {
        Assertions.assertThatNoException()
                .isThrownBy(() -> new WorkflowCompleted(null));
    }

    @Test
    @DisplayName("two WorkflowCompleted records with same result are equal")
    void workflowCompletedValueEquality() {
        WorkflowCompleted a = new WorkflowCompleted("APPROVED");
        WorkflowCompleted b = new WorkflowCompleted("APPROVED");
        Assertions.assertThat(a).isEqualTo(b);
    }

    // ── Sealed type check ─────────────────────────────────────────────────────

    @Test
    @DisplayName("all three record types implement Event")
    void allRecordsImplementEvent() {
        Event e1 = new WorkflowStarted("loan-approval", null);
        Event e2 = new ActivityScheduled("act-1", "check", null);
        Event e3 = new WorkflowCompleted("done");
        Assertions.assertThat(e1).isInstanceOf(Event.class);
        Assertions.assertThat(e2).isInstanceOf(Event.class);
        Assertions.assertThat(e3).isInstanceOf(Event.class);
    }
}
