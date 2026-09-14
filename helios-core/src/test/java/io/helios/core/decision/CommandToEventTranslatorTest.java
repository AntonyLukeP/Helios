package io.helios.core.decision;

import io.helios.core.command.CompleteWorkflow;
import io.helios.core.command.ScheduleActivity;
import io.helios.core.event.ActivityScheduled;
import io.helios.core.event.Event;
import io.helios.core.event.WorkflowCompleted;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CommandToEventTranslator")
class CommandToEventTranslatorTest {

    private CommandToEventTranslator translator;

    @BeforeEach
    void setUp() {
        translator = new CommandToEventTranslator();
    }

    // ── Field preservation ────────────────────────────────────────────────────

    @Test
    @DisplayName("ScheduleActivity fields are preserved in the translated ActivityScheduled")
    void scheduleActivityFieldsPreserved() {
        List<Event> events = translator.translate(
                List.of(new ScheduleActivity("act-42", "run-credit-check", "{\"id\":1}")));

        Assertions.assertThat(events).hasSize(1);
        ActivityScheduled event = (ActivityScheduled) events.get(0);
        Assertions.assertThat(event.activityId()).isEqualTo("act-42");
        Assertions.assertThat(event.activityType()).isEqualTo("run-credit-check");
        Assertions.assertThat(event.input()).isEqualTo("{\"id\":1}");
    }

    @Test
    @DisplayName("CompleteWorkflow result is preserved in the translated WorkflowCompleted")
    void completeWorkflowResultPreserved() {
        List<Event> events = translator.translate(List.of(new CompleteWorkflow("APPROVED")));

        Assertions.assertThat(events).hasSize(1);
        WorkflowCompleted event = (WorkflowCompleted) events.get(0);
        Assertions.assertThat(event.result()).isEqualTo("APPROVED");
    }

    @Test
    @DisplayName("CompleteWorkflow with null result translates to WorkflowCompleted with null result")
    void completeWorkflowNullResultPreserved() {
        List<Event> events = translator.translate(List.of(new CompleteWorkflow(null)));

        WorkflowCompleted event = (WorkflowCompleted) events.get(0);
        Assertions.assertThat(event.result()).isNull();
    }

    // ── Order preservation ────────────────────────────────────────────────────

    @Test
    @DisplayName("command order is preserved in the translated event order")
    void commandOrderPreservedInEvents() {
        List<Event> events = translator.translate(List.of(
                new ScheduleActivity("act-1", "credit-check", null),
                new ScheduleActivity("act-2", "id-verify", null),
                new CompleteWorkflow("APPROVED")));

        Assertions.assertThat(events).hasSize(3);
        Assertions.assertThat(events.get(0)).isInstanceOf(ActivityScheduled.class);
        Assertions.assertThat(events.get(1)).isInstanceOf(ActivityScheduled.class);
        Assertions.assertThat(events.get(2)).isInstanceOf(WorkflowCompleted.class);

        Assertions.assertThat(((ActivityScheduled) events.get(0)).activityId()).isEqualTo("act-1");
        Assertions.assertThat(((ActivityScheduled) events.get(1)).activityId()).isEqualTo("act-2");
    }

    // ── End-to-end valid decision ─────────────────────────────────────────────

    @Test
    @DisplayName("schedule-then-complete translates to ActivityScheduled then WorkflowCompleted")
    void scheduleThenCompleteTranslatesCorrectly() {
        List<Event> events = translator.translate(List.of(
                new ScheduleActivity("act-1", "credit-check", null),
                new CompleteWorkflow("APPROVED")));

        Assertions.assertThat(events).hasSize(2);
        Assertions.assertThat(events.get(0)).isInstanceOf(ActivityScheduled.class);
        Assertions.assertThat(events.get(1)).isInstanceOf(WorkflowCompleted.class);
    }

    // ── Null guard ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("null command list is rejected")
    void nullCommandListIsRejected() {
        Assertions.assertThatNullPointerException()
                .isThrownBy(() -> translator.translate(null))
                .withMessageContaining("commands must not be null");
    }
}
