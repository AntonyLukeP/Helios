package io.helios.core.engine;

import io.helios.core.workflow.WorkflowDefinition;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link HeliosEngine}.
 *
 * <p>Each test targets one precise behaviour of the Phase 1 engine contract.
 * No mocking framework is needed: lambdas with atomic captures are sufficient
 * to observe exactly what the engine passes in and what it does with the result.
 */
@DisplayName("HeliosEngine")
class HeliosEngineTest {

    private final HeliosEngine engine = new HeliosEngine();

    @Test
    @DisplayName("invokes the workflow definition exactly once per run call")
    void invokesWorkflowExactlyOnce() {
        AtomicInteger callCount = new AtomicInteger(0);
        WorkflowDefinition<String, String> workflow = input -> {
            callCount.incrementAndGet();
            return "result";
        };
        engine.run(workflow, "any-input");
        Assertions.assertThat(callCount.get())
                .as("engine must delegate to the workflow exactly once")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("passes the original input to the workflow without modification")
    void passesInputUnchangedToWorkflow() {
        AtomicReference<String> received = new AtomicReference<>();
        WorkflowDefinition<String, String> workflow = input -> {
            received.set(input);
            return "result";
        };
        engine.run(workflow, "exact-value");
        Assertions.assertThat(received.get())
                .as("engine must not transform the input before handing it to the workflow")
                .isEqualTo("exact-value");
    }

    @Test
    @DisplayName("returns the workflow result to the caller without alteration")
    void returnsWorkflowResultUnchanged() {
        WorkflowDefinition<String, String> workflow = input -> "computed-" + input;
        String result = engine.run(workflow, "X");
        Assertions.assertThat(result)
                .as("engine must not transform the workflow result before returning it")
                .isEqualTo("computed-X");
    }

    @Test
    @DisplayName("rejects a null workflow definition with a clear NullPointerException")
    void rejectsNullWorkflowDefinition() {
        Assertions.assertThatNullPointerException()
                .isThrownBy(() -> engine.run(null, "any-input"))
                .withMessage("workflow must not be null");
    }

    @Test
    @DisplayName("propagates a workflow exception to the caller as-is without wrapping")
    void propagatesWorkflowExceptionUnchanged() {
        RuntimeException deliberate = new IllegalStateException("workflow failed deliberately");
        WorkflowDefinition<String, String> failingWorkflow = input -> { throw deliberate; };
        Assertions.assertThatThrownBy(() -> engine.run(failingWorkflow, "any-input"))
                .isSameAs(deliberate);
    }
}
