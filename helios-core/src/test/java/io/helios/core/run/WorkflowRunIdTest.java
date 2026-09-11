package io.helios.core.run;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("WorkflowRunId")
class WorkflowRunIdTest {

    @Test
    @DisplayName("newId() returns a non-blank UUID-based value")
    void newIdReturnsNonBlankValue() {
        WorkflowRunId id = WorkflowRunId.newId();
        Assertions.assertThat(id.value())
                .as("newId() must produce a non-blank value")
                .isNotBlank();
    }

    @Test
    @DisplayName("two newId() calls produce distinct values")
    void newIdProducesDistinctValues() {
        WorkflowRunId a = WorkflowRunId.newId();
        WorkflowRunId b = WorkflowRunId.newId();
        Assertions.assertThat(a).isNotEqualTo(b);
    }

    @Test
    @DisplayName("two instances with the same value are equal")
    void sameValueIsEqual() {
        WorkflowRunId a = new WorkflowRunId("run-123");
        WorkflowRunId b = new WorkflowRunId("run-123");
        Assertions.assertThat(a).isEqualTo(b);
    }

    @Test
    @DisplayName("null value is rejected with a clear NullPointerException")
    void nullValueIsRejected() {
        Assertions.assertThatNullPointerException()
                .isThrownBy(() -> new WorkflowRunId(null))
                .withMessageContaining("value must not be null");
    }

    @Test
    @DisplayName("blank value is rejected with a clear IllegalArgumentException")
    void blankValueIsRejected() {
        Assertions.assertThatIllegalArgumentException()
                .isThrownBy(() -> new WorkflowRunId("   "))
                .withMessageContaining("value must not be blank");
    }
}
