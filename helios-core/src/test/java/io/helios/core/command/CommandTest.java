package io.helios.core.command;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Command sealed interface")
class CommandTest {

    // ── ScheduleActivity ──────────────────────────────────────────────────────

    @Test
    @DisplayName("ScheduleActivity stores all three fields correctly")
    void scheduleActivityStoresFields() {
        ScheduleActivity cmd = new ScheduleActivity("act-1", "run-credit-check", "{\"id\":1}");
        Assertions.assertThat(cmd.activityId()).isEqualTo("act-1");
        Assertions.assertThat(cmd.activityType()).isEqualTo("run-credit-check");
        Assertions.assertThat(cmd.input()).isEqualTo("{\"id\":1}");
    }

    @Test
    @DisplayName("ScheduleActivity permits null input")
    void scheduleActivityPermitsNullInput() {
        Assertions.assertThatNoException()
                .isThrownBy(() -> new ScheduleActivity("act-1", "run-credit-check", null));
    }

    @Test
    @DisplayName("ScheduleActivity rejects null activityId")
    void scheduleActivityRejectsNullActivityId() {
        Assertions.assertThatIllegalArgumentException()
                .isThrownBy(() -> new ScheduleActivity(null, "run-credit-check", null))
                .withMessageContaining("activityId must not be blank");
    }

    @Test
    @DisplayName("ScheduleActivity rejects blank activityId")
    void scheduleActivityRejectsBlankActivityId() {
        Assertions.assertThatIllegalArgumentException()
                .isThrownBy(() -> new ScheduleActivity("  ", "run-credit-check", null))
                .withMessageContaining("activityId must not be blank");
    }

    @Test
    @DisplayName("ScheduleActivity rejects null activityType")
    void scheduleActivityRejectsNullActivityType() {
        Assertions.assertThatIllegalArgumentException()
                .isThrownBy(() -> new ScheduleActivity("act-1", null, null))
                .withMessageContaining("activityType must not be blank");
    }

    @Test
    @DisplayName("ScheduleActivity rejects blank activityType")
    void scheduleActivityRejectsBlankActivityType() {
        Assertions.assertThatIllegalArgumentException()
                .isThrownBy(() -> new ScheduleActivity("act-1", "   ", null))
                .withMessageContaining("activityType must not be blank");
    }

    @Test
    @DisplayName("two ScheduleActivity records with same fields are equal")
    void scheduleActivityValueEquality() {
        ScheduleActivity a = new ScheduleActivity("act-1", "run-credit-check", "{}");
        ScheduleActivity b = new ScheduleActivity("act-1", "run-credit-check", "{}");
        Assertions.assertThat(a).isEqualTo(b);
    }

    // ── CompleteWorkflow ──────────────────────────────────────────────────────

    @Test
    @DisplayName("CompleteWorkflow stores a non-null result")
    void completeWorkflowStoresResult() {
        CompleteWorkflow cmd = new CompleteWorkflow("APPROVED");
        Assertions.assertThat(cmd.result()).isEqualTo("APPROVED");
    }

    @Test
    @DisplayName("CompleteWorkflow permits null result")
    void completeWorkflowPermitsNullResult() {
        CompleteWorkflow cmd = new CompleteWorkflow(null);
        Assertions.assertThat(cmd.result()).isNull();
    }

    @Test
    @DisplayName("two CompleteWorkflow records with same result are equal")
    void completeWorkflowValueEquality() {
        CompleteWorkflow a = new CompleteWorkflow("APPROVED");
        CompleteWorkflow b = new CompleteWorkflow("APPROVED");
        Assertions.assertThat(a).isEqualTo(b);
    }

    // ── Sealed type check ─────────────────────────────────────────────────────

    @Test
    @DisplayName("ScheduleActivity and CompleteWorkflow are both Commands")
    void bothRecordsImplementCommand() {
        Command scheduleCmd = new ScheduleActivity("act-1", "check", null);
        Command completeCmd = new CompleteWorkflow("done");
        Assertions.assertThat(scheduleCmd).isInstanceOf(Command.class);
        Assertions.assertThat(completeCmd).isInstanceOf(Command.class);
    }
}
