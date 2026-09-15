package io.helios.core.workflow;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("WorkflowContext")
class WorkflowContextTest {

    @Test
    @DisplayName("executeActivity throws UnsupportedOperationException before Phase 3 wiring is complete")
    void executeActivityThrowsUnsupportedOperation() {
        WorkflowContext ctx = new WorkflowContext();
        Assertions.assertThatThrownBy(() -> ctx.executeActivity("checkCredit", "app-100"))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("Phase 3");
    }

    @Test
    @DisplayName("UnsupportedOperationException message names the unfinished replay wiring")
    void executeActivityMessageDescribesIncompleteWiring() {
        WorkflowContext ctx = new WorkflowContext();
        Assertions.assertThatThrownBy(() -> ctx.executeActivity("checkCredit", "app-100"))
                .hasMessageContaining("replay");
    }

    @Test
    @DisplayName("WorkflowSuspended.INSTANCE is a non-null RuntimeException control-flow signal")
    void workflowSuspendedIsRuntimeException() {
        Assertions.assertThat(WorkflowSuspended.INSTANCE)
                .isNotNull()
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("WorkflowSuspended suppresses stack trace fill-in (it is control flow, not an error)")
    void workflowSuspendedSuppressesStackTrace() {
        Assertions.assertThat(WorkflowSuspended.INSTANCE.getStackTrace()).isEmpty();
    }
}
