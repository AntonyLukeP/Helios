package io.helios.core.workflow;

import io.helios.core.command.Command;
import io.helios.core.command.CompleteWorkflow;
import io.helios.core.command.ScheduleActivity;
import io.helios.core.event.ActivityCompleted;
import io.helios.core.event.ActivityScheduled;
import io.helios.core.event.Event;
import io.helios.core.event.WorkflowCompleted;
import io.helios.core.event.WorkflowStarted;
import io.helios.core.history.EventHistory;
import io.helios.core.history.InMemoryEventStore;
import io.helios.core.run.WorkflowRunId;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("WorkflowExecutor")
class WorkflowExecutorTest {

    private final WorkflowExecutor executor = new WorkflowExecutor();
    private static final WorkflowRunId RUN = new WorkflowRunId("test-run");

    /**
     * Builds an {@link EventHistory} by appending the given events to a fresh
     * {@link InMemoryEventStore}. An empty array produces an empty history.
     */
    private static EventHistory historyOf(Event... events) {
        InMemoryEventStore store = new InMemoryEventStore();
        if (events.length > 0) {
            store.append(RUN, List.of(events));
        }
        return store.history(RUN);
    }

    /** Simple one-activity workflow: schedules credit check and returns its result. */
    private static final ReplayableWorkflowDefinition ONE_ACTIVITY =
            (ctx, input) -> ctx.executeActivity("checkCredit", input);

    // ── null guards ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("null workflow is rejected with a clear NullPointerException")
    void nullWorkflowRejected() {
        EventHistory h = historyOf(new WorkflowStarted("T", "i"));
        Assertions.assertThatNullPointerException()
                .isThrownBy(() -> executor.execute(null, h))
                .withMessageContaining("workflow");
    }

    @Test
    @DisplayName("null history is rejected with a clear NullPointerException")
    void nullHistoryRejected() {
        Assertions.assertThatNullPointerException()
                .isThrownBy(() -> executor.execute(ONE_ACTIVITY, null))
                .withMessageContaining("history");
    }

    // ── WorkflowStarted validation ────────────────────────────────────────────

    @Test
    @DisplayName("missing WorkflowStarted event fails clearly")
    void missingWorkflowStartedFails() {
        EventHistory h = historyOf(); // empty — no WorkflowStarted
        Assertions.assertThatIllegalArgumentException()
                .isThrownBy(() -> executor.execute(ONE_ACTIVITY, h))
                .withMessageContaining("WorkflowStarted");
    }

    @Test
    @DisplayName("multiple WorkflowStarted events fail clearly")
    void multipleWorkflowStartedFails() {
        EventHistory h = historyOf(
                new WorkflowStarted("T", "i"),
                new WorkflowStarted("T2", "i2"));
        Assertions.assertThatIllegalArgumentException()
                .isThrownBy(() -> executor.execute(ONE_ACTIVITY, h))
                .withMessageContaining("WorkflowStarted");
    }

    // ── WorkflowCompleted validation ──────────────────────────────────────────

    @Test
    @DisplayName("multiple WorkflowCompleted events fail clearly")
    void multipleWorkflowCompletedFails() {
        EventHistory h = historyOf(
                new WorkflowStarted("T", "i"),
                new WorkflowCompleted("r"),
                new WorkflowCompleted("r2"));
        Assertions.assertThatIllegalArgumentException()
                .isThrownBy(() -> executor.execute(ONE_ACTIVITY, h))
                .withMessageContaining("WorkflowCompleted");
    }

    // ── initial run: suspend emitting ScheduleActivity ────────────────────────

    @Test
    @DisplayName("initial one-activity workflow emits ScheduleActivity, not CompleteWorkflow")
    void initialWorkflow_emitsScheduleActivity_notCompleteWorkflow() {
        EventHistory h = historyOf(new WorkflowStarted("LoanApproval", "app-100"));

        List<Command> cmds = executor.execute(ONE_ACTIVITY, h);

        Assertions.assertThat(cmds).hasSize(1);
        Assertions.assertThat(cmds.get(0)).isInstanceOf(ScheduleActivity.class);
        ScheduleActivity cmd = (ScheduleActivity) cmds.get(0);
        Assertions.assertThat(cmd.activityId()).isEqualTo("activity-1");
        Assertions.assertThat(cmd.activityType()).isEqualTo("checkCredit");
        Assertions.assertThat(cmd.input()).isEqualTo("app-100");
    }

    @Test
    @DisplayName("normal suspension does not get reported as an error")
    void normalSuspension_returnsCommandsWithoutThrowing() {
        EventHistory h = historyOf(new WorkflowStarted("T", "i"));
        Assertions.assertThatNoException().isThrownBy(() -> executor.execute(ONE_ACTIVITY, h));
    }

    // ── after activity completes: emits CompleteWorkflow ──────────────────────

    @Test
    @DisplayName("after matching completion is recorded, replay returns result and emits CompleteWorkflow")
    void afterActivityCompletes_emitsCompleteWorkflow() {
        EventHistory h = historyOf(
                new WorkflowStarted("LoanApproval", "app-100"),
                new ActivityScheduled("activity-1", "checkCredit", "app-100"),
                new ActivityCompleted("activity-1", "score:750"));

        List<Command> cmds = executor.execute(ONE_ACTIVITY, h);

        Assertions.assertThat(cmds).hasSize(1);
        Assertions.assertThat(cmds.get(0)).isInstanceOf(CompleteWorkflow.class);
        Assertions.assertThat(((CompleteWorkflow) cmds.get(0)).result()).isEqualTo("score:750");
    }

    // ── final replay: WorkflowCompleted already in history ────────────────────

    @Test
    @DisplayName("final replay against recorded WorkflowCompleted emits no commands")
    void finalReplay_emitsNoCommands() {
        EventHistory h = historyOf(
                new WorkflowStarted("LoanApproval", "app-100"),
                new ActivityScheduled("activity-1", "checkCredit", "app-100"),
                new ActivityCompleted("activity-1", "score:750"),
                new WorkflowCompleted("score:750"));

        List<Command> cmds = executor.execute(ONE_ACTIVITY, h);

        Assertions.assertThat(cmds).isEmpty();
    }

    // ── non-determinism ────────────────────────────────────────────────────────

    @Test
    @DisplayName("changed workflow activity type causes NonDeterminismException")
    void changedActivityType_causesNonDeterminism() {
        EventHistory h = historyOf(
                new WorkflowStarted("T", "app-100"),
                new ActivityScheduled("activity-1", "checkCredit", "app-100"));

        ReplayableWorkflowDefinition wrongType =
                (ctx, input) -> ctx.executeActivity("WRONG_TYPE", input);

        Assertions.assertThatThrownBy(() -> executor.execute(wrongType, h))
                .isInstanceOf(NonDeterminismException.class)
                .hasMessageContaining("activityType");
    }

    @Test
    @DisplayName("workflow returning while history expects an activity causes NonDeterminismException")
    void workflowReturnsTooEarly_causesNonDeterminism() {
        // activity-1 complete; activity-2 scheduled but not complete
        EventHistory h = historyOf(
                new WorkflowStarted("T", "app-100"),
                new ActivityScheduled("activity-1", "checkCredit", "app-100"),
                new ActivityCompleted("activity-1", "score:750"),
                new ActivityScheduled("activity-2", "idVerify", "id-doc"));

        // Workflow fast-forwards activity-1 then returns without consuming activity-2
        ReplayableWorkflowDefinition returnsEarly = (ctx, input) -> {
            ctx.executeActivity("checkCredit", input); // fast-forwards activity-1
            return "done"; // returns — activity-2 in history is unmatched
        };

        Assertions.assertThatThrownBy(() -> executor.execute(returnsEarly, h))
                .isInstanceOf(NonDeterminismException.class);
    }
}
