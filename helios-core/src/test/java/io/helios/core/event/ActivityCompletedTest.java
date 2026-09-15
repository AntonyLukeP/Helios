package io.helios.core.event;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ActivityCompleted")
class ActivityCompletedTest {

    // ── Valid construction ────────────────────────────────────────────────────

    @Test
    @DisplayName("stores activityId and result correctly")
    void storesFields() {
        ActivityCompleted event = new ActivityCompleted("activity-1", "score:750");
        Assertions.assertThat(event.activityId()).isEqualTo("activity-1");
        Assertions.assertThat(event.result()).isEqualTo("score:750");
    }

    @Test
    @DisplayName("result may be null")
    void resultIsNullable() {
        Assertions.assertThatNoException()
                .isThrownBy(() -> new ActivityCompleted("activity-1", null));
        Assertions.assertThat(new ActivityCompleted("activity-1", null).result()).isNull();
    }

    @Test
    @DisplayName("two records with the same fields are equal (value object)")
    void valueEquality() {
        ActivityCompleted a = new ActivityCompleted("activity-1", "score:750");
        ActivityCompleted b = new ActivityCompleted("activity-1", "score:750");
        Assertions.assertThat(a).isEqualTo(b);
    }

    @Test
    @DisplayName("implements Event sealed interface")
    void implementsEvent() {
        Event event = new ActivityCompleted("activity-1", "score:750");
        Assertions.assertThat(event).isInstanceOf(Event.class);
    }

    // ── activityId validation ─────────────────────────────────────────────────

    @Test
    @DisplayName("null activityId is rejected with a clear message")
    void nullActivityIdRejected() {
        Assertions.assertThatIllegalArgumentException()
                .isThrownBy(() -> new ActivityCompleted(null, "score:750"))
                .withMessageContaining("activityId must not be blank");
    }

    @Test
    @DisplayName("blank activityId is rejected with a clear message")
    void blankActivityIdRejected() {
        Assertions.assertThatIllegalArgumentException()
                .isThrownBy(() -> new ActivityCompleted("  ", "score:750"))
                .withMessageContaining("activityId must not be blank");
    }

    // ── Distinct from scheduling ──────────────────────────────────────────────

    @Test
    @DisplayName("ActivityCompleted is a different type from ActivityScheduled")
    void distinctFromActivityScheduled() {
        Event completed = new ActivityCompleted("activity-1", "score:750");
        Event scheduled = new ActivityScheduled("activity-1", "checkCredit", null);
        Assertions.assertThat(completed).isNotInstanceOf(ActivityScheduled.class);
        Assertions.assertThat(scheduled).isNotInstanceOf(ActivityCompleted.class);
        Assertions.assertThat(completed).isNotEqualTo(scheduled);
    }
}
