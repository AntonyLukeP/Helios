package io.helios.core.workflow;

/**
 * Internal control-flow signal thrown by {@link WorkflowContext} to unwind the
 * workflow call stack when execution must pause at the history frontier.
 *
 * <p>This is <strong>not a business exception</strong>. It is never caused by
 * workflow logic, never surfaced to callers of the engine, and never logged as
 * an error. It is the mechanism by which the replay executor stops a workflow
 * mid-execution cleanly, without returning a value.
 *
 * <p>Construction is controlled through the {@link #INSTANCE} singleton.
 * Throwing the same object every time avoids allocating a new exception and,
 * more importantly, avoids filling in a stack trace — stack-trace fill-in is
 * the most expensive part of throwing an exception in the JVM and is completely
 * unnecessary for a control-flow signal.
 *
 * <p>The replay executor catches {@code WorkflowSuspended} and interprets it
 * as: "workflow reached the frontier; no further commands this turn." Any other
 * exception escaping from workflow code is a genuine workflow or programming
 * error and must not be caught alongside {@code WorkflowSuspended}.
 */
final class WorkflowSuspended extends RuntimeException {

    /**
     * The single instance to throw.
     *
     * <p>Usage inside the replay context:
     * <pre>{@code
     * throw WorkflowSuspended.INSTANCE;
     * }</pre>
     */
    static final WorkflowSuspended INSTANCE = new WorkflowSuspended();

    /**
     * Private constructor.
     *
     * <p>Passes {@code writableStackTrace = false} to the superclass so the JVM
     * never fills in a stack trace for this instance. This makes throw/catch of
     * {@code WorkflowSuspended} nearly as cheap as a normal method return.
     */
    private WorkflowSuspended() {
        super(
                "workflow suspended at history frontier — this is control flow, not an error",
                /*cause*/ null,
                /*enableSuppression*/ false,
                /*writableStackTrace*/ false);
    }
}
