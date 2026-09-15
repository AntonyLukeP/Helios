package io.helios.core.event;

/**
 * Event: an external source has reported the outcome of a previously scheduled activity.
 *
 * <p>This event is the counterpart to {@link ActivityScheduled}. Together they
 * form a pair:
 * <ol>
 *   <li>{@link ActivityScheduled} — the engine committed to requesting the work.
 *   <li>{@code ActivityCompleted} — an external worker (or, in Phase 3, the test
 *       harness) reported the result.
 * </ol>
 *
 * <p>Only a real-world result should ever create an {@code ActivityCompleted}
 * event. The Phase 3 replay engine cannot fabricate completion events; it can
 * only observe them in the history. In tests the harness plays the role of the
 * external world by appending this event manually, exactly as a real worker
 * would via a future API.
 *
 * <p>This event must not appear in {@link io.helios.core.decision.CommandToEventTranslator}.
 * There is no corresponding command for activity completion: it arrives from
 * outside the engine, not from workflow code.
 *
 * @param activityId the ID of the activity that completed; must not be null or blank;
 *                   must match the {@code activityId} of its paired
 *                   {@link ActivityScheduled} event
 * @param result     the serialized result reported by the external source;
 *                   may be null if the activity produces no meaningful output
 */
public record ActivityCompleted(String activityId, String result) implements Event {

    /** Compact constructor — enforces the non-blank identity field. */
    public ActivityCompleted {
        if (activityId == null || activityId.isBlank()) {
            throw new IllegalArgumentException("activityId must not be blank");
        }
        // result is intentionally unrestricted — null is a valid no-output result
    }
}
