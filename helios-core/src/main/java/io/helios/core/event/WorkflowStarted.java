package io.helios.core.event;

/**
 * Event: a workflow run has been started.
 *
 * <p>This is always the first event in any run history. It records the workflow
 * type and the input that was provided at start time.
 *
 * @param workflowType the name of the workflow definition (e.g. "loan-approval")
 * @param input        the serialized input; may be null
 */
public record WorkflowStarted(String workflowType, String input) implements Event {}
