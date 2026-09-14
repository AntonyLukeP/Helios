package io.helios.core.engine;

import io.helios.core.command.Command;
import io.helios.core.command.CompleteWorkflow;
import io.helios.core.command.ScheduleActivity;
import io.helios.core.decision.CommandToEventTranslator;
import io.helios.core.decision.DecisionValidator;
import io.helios.core.event.ActivityScheduled;
import io.helios.core.event.WorkflowCompleted;
import io.helios.core.event.WorkflowStarted;
import io.helios.core.history.EventHistory;
import io.helios.core.history.InMemoryEventStore;
import io.helios.core.history.RecordedEvent;
import io.helios.core.run.WorkflowRunId;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("InMemoryWorkflowHistoryService")
class InMemoryWorkflowHistoryServiceTest {

    private InMemoryWorkflowHistoryService service;
    private WorkflowRunId loanRun;

    @BeforeEach
    void setUp() {
        service = new InMemoryWorkflowHistoryService(
                new InMemoryEventStore(),
                new DecisionValidator(),
                new CommandToEventTranslator());
        loanRun = new WorkflowRunId("loan-100");
    }

    // ── end-to-end story ──────────────────────────────────────────────────────

    @Test
    @DisplayName("loan-approval story: start → schedule activity → complete → three ordered events")
    void loanApprovalEndToEndStory() {
        // start the run
        RecordedEvent startEvent = service.start(loanRun, "LoanApproval", "application-100");
        Assertions.assertThat(startEvent.sequence()).isEqualTo(1L);
        Assertions.assertThat(startEvent.event()).isInstanceOf(WorkflowStarted.class);
        WorkflowStarted started = (WorkflowStarted) startEvent.event();
        Assertions.assertThat(started.workflowType()).isEqualTo("LoanApproval");
        Assertions.assertThat(started.input()).isEqualTo("application-100");

        // apply first decision: schedule a credit check
        List<RecordedEvent> decision1 = service.applyDecision(
                loanRun,
                List.of(new ScheduleActivity("credit-check-1", "checkCredit", "application-100")));
        Assertions.assertThat(decision1).hasSize(1);
        Assertions.assertThat(decision1.get(0).sequence()).isEqualTo(2L);
        Assertions.assertThat(decision1.get(0).event()).isInstanceOf(ActivityScheduled.class);
        ActivityScheduled scheduled = (ActivityScheduled) decision1.get(0).event();
        Assertions.assertThat(scheduled.activityId()).isEqualTo("credit-check-1");
        Assertions.assertThat(scheduled.activityType()).isEqualTo("checkCredit");
        Assertions.assertThat(scheduled.input()).isEqualTo("application-100");

        // inspect full history after two appends
        EventHistory historyAfterSchedule = service.history(loanRun);
        Assertions.assertThat(historyAfterSchedule.size()).isEqualTo(2);
        Assertions.assertThat(historyAfterSchedule.events().get(0).sequence()).isEqualTo(1L);
        Assertions.assertThat(historyAfterSchedule.events().get(0).event())
                .isInstanceOf(WorkflowStarted.class);
        Assertions.assertThat(historyAfterSchedule.events().get(1).sequence()).isEqualTo(2L);
        Assertions.assertThat(historyAfterSchedule.events().get(1).event())
                .isInstanceOf(ActivityScheduled.class);

        // apply second decision: complete the workflow
        List<RecordedEvent> decision2 =
                service.applyDecision(loanRun, List.of(new CompleteWorkflow("approved")));
        Assertions.assertThat(decision2).hasSize(1);
        Assertions.assertThat(decision2.get(0).sequence()).isEqualTo(3L);
        Assertions.assertThat(decision2.get(0).event()).isInstanceOf(WorkflowCompleted.class);
        WorkflowCompleted completed = (WorkflowCompleted) decision2.get(0).event();
        Assertions.assertThat(completed.result()).isEqualTo("approved");

        // assert full history of 3 events
        EventHistory finalHistory = service.history(loanRun);
        Assertions.assertThat(finalHistory.size()).isEqualTo(3);
        Assertions.assertThat(finalHistory.events().get(2).sequence()).isEqualTo(3L);
        Assertions.assertThat(finalHistory.events().get(2).event())
                .isInstanceOf(WorkflowCompleted.class);
    }

    // ── start ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("start returns a RecordedEvent with sequence 1 and WorkflowStarted")
    void startReturnsRecordedEventSequence1() {
        RecordedEvent event = service.start(loanRun, "LoanApproval", null);
        Assertions.assertThat(event.sequence()).isEqualTo(1L);
        Assertions.assertThat(event.event()).isInstanceOf(WorkflowStarted.class);
    }

    @Test
    @DisplayName("starting the same run ID twice is rejected with a clear message")
    void duplicateStartIsRejected() {
        service.start(loanRun, "LoanApproval", "application-100");
        Assertions.assertThatIllegalArgumentException()
                .isThrownBy(() -> service.start(loanRun, "LoanApproval", "application-100"))
                .withMessageContaining("loan-100")
                .withMessageContaining("already started");
    }

    @Test
    @DisplayName("two different run IDs can be started independently")
    void twoRunIdsStartIndependently() {
        WorkflowRunId runB = new WorkflowRunId("loan-200");
        service.start(loanRun, "LoanApproval", null);
        Assertions.assertThatNoException()
                .isThrownBy(() -> service.start(runB, "LoanApproval", null));
    }

    // ── applyDecision ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("applyDecision on an unknown run is rejected with a clear message")
    void applyDecisionUnknownRunRejected() {
        Assertions.assertThatIllegalArgumentException()
                .isThrownBy(() -> service.applyDecision(
                        loanRun, List.of(new CompleteWorkflow("done"))))
                .withMessageContaining("loan-100")
                .withMessageContaining("unknown");
    }

    @Test
    @DisplayName("applyDecision delegates validation: invalid commands are rejected")
    void applyDecisionDelegatesValidation() {
        service.start(loanRun, "LoanApproval", null);
        // empty command list is invalid per DecisionValidator
        Assertions.assertThatIllegalArgumentException()
                .isThrownBy(() -> service.applyDecision(loanRun, List.of()))
                .withMessageContaining("at least one command");
    }

    @Test
    @DisplayName("applyDecision with two commands returns two recorded events")
    void applyDecisionMultipleCommandsRecorded() {
        service.start(loanRun, "LoanApproval", null);
        List<Command> commands = List.of(
                new ScheduleActivity("act-1", "checkCredit", null),
                new ScheduleActivity("act-2", "verifyId", null));

        List<RecordedEvent> recorded = service.applyDecision(loanRun, commands);

        Assertions.assertThat(recorded).hasSize(2);
        Assertions.assertThat(recorded.get(0).sequence()).isEqualTo(2L);
        Assertions.assertThat(recorded.get(1).sequence()).isEqualTo(3L);
    }

    // ── history ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("history for an unknown run returns empty, not null")
    void historyUnknownRunReturnsEmpty() {
        EventHistory history = service.history(loanRun);
        Assertions.assertThat(history).isNotNull();
        Assertions.assertThat(history.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("history after start contains exactly one WorkflowStarted event")
    void historyAfterStartHasOneEvent() {
        service.start(loanRun, "LoanApproval", "application-100");
        EventHistory history = service.history(loanRun);
        Assertions.assertThat(history.size()).isEqualTo(1);
        Assertions.assertThat(history.events().get(0).event()).isInstanceOf(WorkflowStarted.class);
    }
}
