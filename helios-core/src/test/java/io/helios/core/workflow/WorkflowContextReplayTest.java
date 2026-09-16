package io.helios.core.workflow;

import io.helios.core.command.Command;
import io.helios.core.command.ScheduleActivity;
import io.helios.core.event.ActivityCompleted;
import io.helios.core.event.ActivityScheduled;
import io.helios.core.event.WorkflowStarted;
import io.helios.core.history.RecordedEvent;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for the deterministic replay behaviour of {@link WorkflowContext}.
 *
 * <p>All tests operate on in-memory {@link RecordedEvent} lists. No executor,
 * database, worker, or thread is involved.
 */
@DisplayName("WorkflowContext — deterministic replay")
class WorkflowContextReplayTest {

    // ── helpers ───────────────────────────────────────────────────────────────

    private static RecordedEvent re(long seq, io.helios.core.event.Event event) {
        return new RecordedEvent(seq, event);
    }

    /** History: WorkflowStarted only — activity frontier is empty. */
    private static List<RecordedEvent> justStarted() {
        return List.of(re(1, new WorkflowStarted("LoanApproval", "app-100")));
    }

    /** History: WorkflowStarted + ActivityScheduled for activity-1, no completion. */
    private static List<RecordedEvent> scheduledOnly() {
        return List.of(
                re(1, new WorkflowStarted("LoanApproval", "app-100")),
                re(2, new ActivityScheduled("activity-1", "checkCredit", "app-100")));
    }

    /** History: WorkflowStarted + ActivityScheduled + ActivityCompleted for activity-1. */
    private static List<RecordedEvent> scheduledAndCompleted() {
        return List.of(
                re(1, new WorkflowStarted("LoanApproval", "app-100")),
                re(2, new ActivityScheduled("activity-1", "checkCredit", "app-100")),
                re(3, new ActivityCompleted("activity-1", "score:750")));
    }

    // ── frontier path ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("empty history: first call emits ScheduleActivity(activity-1) then suspends")
    void emptyHistory_emitsScheduleAndSuspends() {
        WorkflowContext ctx = new WorkflowContext(justStarted());

        Assertions.assertThatThrownBy(() -> ctx.executeActivity("checkCredit", "app-100"))
                .isSameAs(WorkflowSuspended.INSTANCE);

        List<Command> cmds = ctx.newCommands();
        Assertions.assertThat(cmds).hasSize(1);
        ScheduleActivity cmd = (ScheduleActivity) cmds.get(0);
        Assertions.assertThat(cmd.activityId()).isEqualTo("activity-1");
        Assertions.assertThat(cmd.activityType()).isEqualTo("checkCredit");
        Assertions.assertThat(cmd.input()).isEqualTo("app-100");
    }

    // ── replay path — incomplete activity ─────────────────────────────────────

    @Test
    @DisplayName("matching scheduled but incomplete: suspends with no new command emitted")
    void matchingScheduled_incomplete_suspendWithNoNewCommand() {
        WorkflowContext ctx = new WorkflowContext(scheduledOnly());

        Assertions.assertThatThrownBy(() -> ctx.executeActivity("checkCredit", "app-100"))
                .isSameAs(WorkflowSuspended.INSTANCE);

        Assertions.assertThat(ctx.newCommands()).isEmpty();
    }

    // ── replay path — completed activity ──────────────────────────────────────

    @Test
    @DisplayName("matching scheduled and completed: returns stored result, emits no command")
    void matchingScheduledAndCompleted_returnsResult() {
        WorkflowContext ctx = new WorkflowContext(scheduledAndCompleted());

        String result = ctx.executeActivity("checkCredit", "app-100");

        Assertions.assertThat(result).isEqualTo("score:750");
        Assertions.assertThat(ctx.newCommands()).isEmpty();
    }

    @Test
    @DisplayName("second call after completed first receives deterministic ID activity-2")
    void secondCallAfterCompletedFirst_getsActivity2() {
        WorkflowContext ctx = new WorkflowContext(scheduledAndCompleted());

        ctx.executeActivity("checkCredit", "app-100"); // fast-forward activity-1

        // activity-1 is complete; now at frontier → emits activity-2
        Assertions.assertThatThrownBy(() -> ctx.executeActivity("idVerify", "id-doc"))
                .isSameAs(WorkflowSuspended.INSTANCE);

        List<Command> cmds = ctx.newCommands();
        Assertions.assertThat(cmds).hasSize(1);
        ScheduleActivity cmd = (ScheduleActivity) cmds.get(0);
        Assertions.assertThat(cmd.activityId()).isEqualTo("activity-2");
        Assertions.assertThat(cmd.activityType()).isEqualTo("idVerify");
    }

    @Test
    @DisplayName("null input in history matches null input in call (Objects.equals semantics)")
    void nullInputMatchesNullInput() {
        List<RecordedEvent> history = List.of(
                re(1, new WorkflowStarted("T", null)),
                re(2, new ActivityScheduled("activity-1", "checkCredit", null)),
                re(3, new ActivityCompleted("activity-1", "done")));
        WorkflowContext ctx = new WorkflowContext(history);

        Assertions.assertThat(ctx.executeActivity("checkCredit", null)).isEqualTo("done");
    }

    // ── non-determinism detection ─────────────────────────────────────────────

    @Test
    @DisplayName("changed activity type causes NonDeterminismException with field/expected/actual")
    void changedActivityType_causesNonDeterminism() {
        WorkflowContext ctx = new WorkflowContext(scheduledOnly()); // expects checkCredit

        Assertions.assertThatThrownBy(() -> ctx.executeActivity("WRONG_TYPE", "app-100"))
                .isInstanceOf(NonDeterminismException.class)
                .hasMessageContaining("activityType")
                .hasMessageContaining("checkCredit")
                .hasMessageContaining("WRONG_TYPE");
    }

    @Test
    @DisplayName("changed input causes NonDeterminismException with field/expected/actual")
    void changedInput_causesNonDeterminism() {
        WorkflowContext ctx = new WorkflowContext(scheduledOnly()); // expects input=app-100

        Assertions.assertThatThrownBy(() -> ctx.executeActivity("checkCredit", "WRONG_INPUT"))
                .isInstanceOf(NonDeterminismException.class)
                .hasMessageContaining("input")
                .hasMessageContaining("app-100")
                .hasMessageContaining("WRONG_INPUT");
    }

    @Test
    @DisplayName("unexpected activity order causes NonDeterminismException")
    void unexpectedActivityOrder_causesNonDeterminism() {
        // History: activity-1=checkCredit (complete), activity-2=idVerify (incomplete)
        List<RecordedEvent> twoActivities = List.of(
                re(1, new WorkflowStarted("LoanApproval", "app-100")),
                re(2, new ActivityScheduled("activity-1", "checkCredit", "app-100")),
                re(3, new ActivityCompleted("activity-1", "score:750")),
                re(4, new ActivityScheduled("activity-2", "idVerify", "id-doc")));
        WorkflowContext ctx = new WorkflowContext(twoActivities);

        ctx.executeActivity("checkCredit", "app-100"); // passes activity-1

        // Workflow now calls wrong activity type at position 2
        Assertions.assertThatThrownBy(() -> ctx.executeActivity("REORDERED", "id-doc"))
                .isInstanceOf(NonDeterminismException.class)
                .hasMessageContaining("activityType")
                .hasMessageContaining("idVerify")
                .hasMessageContaining("REORDERED");
    }

    // ── immutability of command view ──────────────────────────────────────────

    @Test
    @DisplayName("returned new-command view cannot be mutated by test code")
    void newCommandsViewIsUnmodifiable() {
        WorkflowContext ctx = new WorkflowContext(justStarted());

        Assertions.assertThatThrownBy(() -> ctx.executeActivity("checkCredit", "app-100"))
                .isSameAs(WorkflowSuspended.INSTANCE);

        List<Command> view = ctx.newCommands();
        Assertions.assertThatThrownBy(() -> view.add(new ScheduleActivity("x", "y", null)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    // ── validation ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("blank activityType is rejected with a clear IllegalArgumentException")
    void blankActivityType_rejected() {
        WorkflowContext ctx = new WorkflowContext(justStarted());

        Assertions.assertThatIllegalArgumentException()
                .isThrownBy(() -> ctx.executeActivity("  ", "app-100"))
                .withMessageContaining("activityType must not be blank");
    }
}
