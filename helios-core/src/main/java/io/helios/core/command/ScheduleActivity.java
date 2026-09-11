package io.helios.core.command;

/**
 * Command: request that an external activity be scheduled for execution.
 *
 * <p>Issuing this command records an {@code ActivityScheduled} event in the run
 * history. It does not execute the activity, assign a worker, or guarantee any
 * delivery. Those belong to later phases.
 *
 * @param activityId   a caller-assigned identifier for this activity invocation;
 *                     must not be null or blank
 * @param activityType the name of the activity to execute (e.g. "run-credit-check");
 *                     must not be null or blank
 * @param input        the serialized input for the activity; may be null in this
 *                     learning phase
 */
public record ScheduleActivity(String activityId, String activityType, String input)
        implements Command {

    /** Compact constructor — enforces the non-blank contract on identity fields. */
    public ScheduleActivity {
        if (activityId == null || activityId.isBlank()) {
            throw new IllegalArgumentException("activityId must not be blank");
        }
        if (activityType == null || activityType.isBlank()) {
            throw new IllegalArgumentException("activityType must not be blank");
        }
        // input intentionally unrestricted — workflows may pass null in this phase
    }
}
