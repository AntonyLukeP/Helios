package io.helios.core.workflow;

/**
 * The authoring surface available to {@link ReplayableWorkflowDefinition} code.
 *
 * <p>Workflow code must interact with the outside world exclusively through
 * this context. Direct I/O, clock reads, random values, or network calls inside
 * workflow code break determinism and therefore break replay.
 *
 * <p>Construction is package-private. Only the Phase 3 replay executor (in the
 * same package) will create {@code WorkflowContext} instances and wire them to
 * the event history. Workflow authors receive a fully configured context; they
 * must not construct one themselves.
 *
 * <p>This class intentionally exposes no public setters, no mutable state
 * accessors, no clock, no random source, no sleep, no signal mechanism, no
 * cancellation handle, no activity registry, and no serialization support.
 * Those concerns belong to later phases.
 */
public final class WorkflowContext {

    /**
     * Package-private construction seam.
     *
     * <p>The Phase 3 replay executor will call this constructor. Workflow
     * authors must not call it; receive the context as a parameter instead.
     */
    WorkflowContext() {}

    /**
     * Schedules an external activity and returns its recorded result.
     *
     * <p><strong>Phase 3 wiring is not yet complete.</strong> This method
     * currently throws {@link UnsupportedOperationException}. Once the replay
     * executor is implemented it will:
     * <ol>
     *   <li>Check whether a matching {@code ActivityScheduled} and
     *       {@code ActivityCompleted} pair already exists in history (fast-forward).
     *   <li>Return the recorded result if the activity is complete.
     *   <li>Emit a {@code ScheduleActivity} command and throw
     *       {@link WorkflowSuspended#INSTANCE} to unwind the call stack if the
     *       activity is new or still pending.
     * </ol>
     *
     * @param activityType the name of the activity to execute
     * @param input        the serialized input for the activity; may be null
     * @return the serialized result reported by the external worker
     * @throws UnsupportedOperationException until Phase 3 replay wiring is complete
     */
    public String executeActivity(String activityType, String input) {
        throw new UnsupportedOperationException(
                "Phase 3 replay wiring is not complete:"
                        + " executeActivity cannot be called until the replay executor is implemented");
    }
}
