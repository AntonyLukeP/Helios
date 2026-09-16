  package io.helios.core.workflow;

/**
 * Thrown when workflow code produces a different decision than what is recorded
 * in the event history for the same replay position.
 *
 * <p>Non-determinism is a programming error in the workflow definition: the same
 * workflow code run against the same history must always produce the same
 * sequence of decisions. Common causes:
 * <ul>
 *   <li>Reading the wall clock ({@code System.currentTimeMillis()}) to branch.
 *   <li>Using a random number to choose an activity type.
 *   <li>Changing the order of {@link WorkflowContext#executeActivity} calls
 *       after history has been recorded.
 *   <li>Changing an activity type or input value for a call that has already
 *       been scheduled in history.
 * </ul>
 *
 * <p>This exception is always fatal for the replay attempt. The engine must not
 * catch it and continue; it indicates a defect in the workflow definition.
 */
public final class NonDeterminismException extends RuntimeException {

    /**
     * Creates a non-determinism exception with a diagnostic message that
     * names the mismatched field and shows both the recorded and the actual value.
     *
     * @param field    the name of the field that differs (e.g. "activityType", "input")
     * @param recorded the value that the event history expects at this position
     * @param actual   the value that the current workflow execution produced
     */
    public NonDeterminismException(String field, Object recorded, Object actual) {
        super(
                "Non-determinism detected in workflow replay: "
                        + field
                        + " changed — "
                        + "history expects '"
                        + recorded
                        + "' but workflow code produced '"
                        + actual
                        + "'");
    }
}
