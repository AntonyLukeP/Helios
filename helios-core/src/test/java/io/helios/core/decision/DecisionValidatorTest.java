package io.helios.core.decision;

import io.helios.core.command.CompleteWorkflow;
import io.helios.core.command.ScheduleActivity;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DecisionValidator")
class DecisionValidatorTest {

    private DecisionValidator validator;

    @BeforeEach
    void setUp() {
        validator = new DecisionValidator();
    }

    // ── Valid decisions ───────────────────────────────────────────────────────

    @Test
    @DisplayName("a single ScheduleActivity command is valid")
    void singleScheduleActivityIsValid() {
        Assertions.assertThatNoException()
                .isThrownBy(() -> validator.validate(
                        List.of(new ScheduleActivity("act-1", "credit-check", null))));
    }

    @Test
    @DisplayName("a single CompleteWorkflow command is valid")
    void singleCompleteWorkflowIsValid() {
        Assertions.assertThatNoException()
                .isThrownBy(() -> validator.validate(List.of(new CompleteWorkflow("APPROVED"))));
    }

    @Test
    @DisplayName("ScheduleActivity followed by CompleteWorkflow is valid")
    void scheduleActivityThenCompleteWorkflowIsValid() {
        Assertions.assertThatNoException()
                .isThrownBy(() -> validator.validate(List.of(
                        new ScheduleActivity("act-1", "credit-check", null),
                        new CompleteWorkflow("APPROVED"))));
    }

    @Test
    @DisplayName("multiple distinct ScheduleActivity commands are valid")
    void multipleDistinctScheduleActivitiesAreValid() {
        Assertions.assertThatNoException()
                .isThrownBy(() -> validator.validate(List.of(
                        new ScheduleActivity("act-1", "credit-check", null),
                        new ScheduleActivity("act-2", "id-verify", null))));
    }

    // ── Terminal command violations ───────────────────────────────────────────

    @Test
    @DisplayName("CompleteWorkflow not-last is rejected with a clear message")
    void completeWorkflowNotLastIsRejected() {
        Assertions.assertThatIllegalArgumentException()
                .isThrownBy(() -> validator.validate(List.of(
                        new CompleteWorkflow("APPROVED"),
                        new ScheduleActivity("act-1", "credit-check", null))))
                .withMessageContaining("CompleteWorkflow")
                .withMessageContaining("terminal");
    }

    @Test
    @DisplayName("two CompleteWorkflow commands are rejected (first is not last)")
    void twoCompleteWorkflowCommandsAreRejected() {
        Assertions.assertThatIllegalArgumentException()
                .isThrownBy(() -> validator.validate(List.of(
                        new CompleteWorkflow("APPROVED"),
                        new CompleteWorkflow("DENIED"))))
                .withMessageContaining("terminal");
    }

    // ── Duplicate activity ID ─────────────────────────────────────────────────

    @Test
    @DisplayName("duplicate activityId in one decision is rejected")
    void duplicateActivityIdIsRejected() {
        Assertions.assertThatIllegalArgumentException()
                .isThrownBy(() -> validator.validate(List.of(
                        new ScheduleActivity("act-1", "credit-check", null),
                        new ScheduleActivity("act-1", "id-verify", null))))
                .withMessageContaining("act-1");
    }

    // ── Null / empty guard ────────────────────────────────────────────────────

    @Test
    @DisplayName("null command list is rejected")
    void nullCommandListIsRejected() {
        Assertions.assertThatNullPointerException()
                .isThrownBy(() -> validator.validate(null))
                .withMessageContaining("commands must not be null");
    }

    @Test
    @DisplayName("empty command list is rejected")
    void emptyCommandListIsRejected() {
        Assertions.assertThatIllegalArgumentException()
                .isThrownBy(() -> validator.validate(List.of()))
                .withMessageContaining("at least one command");
    }
}
