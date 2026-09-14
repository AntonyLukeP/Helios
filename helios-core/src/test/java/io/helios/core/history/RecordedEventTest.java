package io.helios.core.history;

import io.helios.core.event.WorkflowStarted;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("RecordedEvent")
class RecordedEventTest {

    private final WorkflowStarted event = new WorkflowStarted("loan-approval", null);

    @Test
    @DisplayName("stores sequence and event correctly")
    void storesFields() {
        RecordedEvent re = new RecordedEvent(1L, event);
        Assertions.assertThat(re.sequence()).isEqualTo(1L);
        Assertions.assertThat(re.event()).isSameAs(event);
    }

    @Test
    @DisplayName("two records with same fields are equal (value object)")
    void valueEquality() {
        RecordedEvent a = new RecordedEvent(1L, event);
        RecordedEvent b = new RecordedEvent(1L, event);
        Assertions.assertThat(a).isEqualTo(b);
    }

    @Test
    @DisplayName("sequence zero is rejected with a clear message")
    void rejectsSequenceZero() {
        Assertions.assertThatIllegalArgumentException()
                .isThrownBy(() -> new RecordedEvent(0L, event))
                .withMessageContaining("sequence must be positive");
    }

    @Test
    @DisplayName("negative sequence is rejected with a clear message")
    void rejectsNegativeSequence() {
        Assertions.assertThatIllegalArgumentException()
                .isThrownBy(() -> new RecordedEvent(-1L, event))
                .withMessageContaining("sequence must be positive");
    }

    @Test
    @DisplayName("null event is rejected with a clear message")
    void rejectsNullEvent() {
        Assertions.assertThatNullPointerException()
                .isThrownBy(() -> new RecordedEvent(1L, null))
                .withMessageContaining("event must not be null");
    }
}
