package io.helios.core.event;

/**
 * Event: the workflow run has completed successfully.
 *
 * <p>This event is produced by translating a
 * {@link io.helios.core.command.CompleteWorkflow} command. Nothing is persisted
 * to a database in this phase.
 *
 * @param result the optional workflow output; may be null
 */
public record WorkflowCompleted(String result) implements Event {}
