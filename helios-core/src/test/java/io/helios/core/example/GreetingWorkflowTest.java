package io.helios.core.example;

import io.helios.core.engine.HeliosEngine;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link GreetingWorkflow}.
 *
 * <p>Tests 1 and 2 verify the happy path: once directly and once through the engine.
 * Tests 3 and 4 verify that the workflow enforces its own business input contract.
 */
@DisplayName("GreetingWorkflow")
class GreetingWorkflowTest {

    private final GreetingWorkflow workflow = new GreetingWorkflow();
    private final HeliosEngine engine = new HeliosEngine();

    @Test
    @DisplayName("produces 'Hello, Ada!' for the input 'Ada'")
    void greetsByName() {
        Assertions.assertThat(workflow.execute("Ada")).isEqualTo("Hello, Ada!");
    }

    @Test
    @DisplayName("invoked through HeliosEngine produces the same result")
    void throughEngineProducesSameResult() {
        String result = engine.run(workflow, "Ada");
        Assertions.assertThat(result)
                .as("engine must not alter the result the workflow produces")
                .isEqualTo("Hello, Ada!");
    }

    @Test
    @DisplayName("rejects null input with a clear IllegalArgumentException")
    void rejectsNullName() {
        Assertions.assertThatIllegalArgumentException()
                .isThrownBy(() -> workflow.execute(null))
                .withMessage("name must not be null");
    }

    @Test
    @DisplayName("rejects blank input with a clear IllegalArgumentException")
    void rejectsBlankName() {
        Assertions.assertThatIllegalArgumentException()
                .isThrownBy(() -> workflow.execute("   "))
                .withMessage("name must not be blank");
    }
}
