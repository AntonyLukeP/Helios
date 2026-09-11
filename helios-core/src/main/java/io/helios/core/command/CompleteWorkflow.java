package io.helios.core.command;

/**
 * Command: request that the workflow run be closed as successfully completed.
 *
 * <p>Issuing this command records a {@code WorkflowCompleted} event in the run
 * history. Nothing is persisted to a database in this phase.
 *
 * @param result the optional workflow output; may be null if the workflow
 *               produces no meaningful result
 */
public record CompleteWorkflow(String result) implements Command {}
