package io.helios.core.event;

/**
 * Event: an activity has been scheduled for execution.
 *
 * <p>This event records the engine's intent to execute an activity. It is
 * produced by translating a {@link io.helios.core.command.ScheduleActivity}
 * command. It does <em>not</em> mean the activity ran or that its result is
 * known. Activity completion will be recorded in a separate event in a later
 * phase.
 *
 * @param activityId   the caller-assigned identifier for this activity invocation
 * @param activityType the name of the activity to execute
 * @param input        the serialized activity input; may be null
 */
public record ActivityScheduled(String activityId, String activityType, String input)
        implements Event {}
