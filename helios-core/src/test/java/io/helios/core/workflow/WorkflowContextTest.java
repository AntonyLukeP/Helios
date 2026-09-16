package io.helios.core.workflow;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link WorkflowContext} API contract and the
 * {@link WorkflowSuspended} control-flow signal.
 *
 * <p>Detailed replay behaviour is covered in {@link WorkflowContextReplayTest}.
 */
@DisplayName("WorkflowContext — API contract")
class WorkflowContextTest {

    @Test
    @DisplayName("no-arg constructor is a valid construction seam (empty-history context)")
    void noArgConstructorIsValidSeam() {
        Assertions.assertThatNoException().isThrownBy(WorkflowContext::new);
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
