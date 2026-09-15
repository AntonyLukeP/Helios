package io.helios.core.workflow;

/**
 * A Phase 3 workflow definition: deterministic business logic driven by replay.
 *
 * <p>Implementations <strong>must be deterministic</strong>: given the same
 * {@link WorkflowContext} state and the same {@code input}, {@code execute}
 * must always produce the same sequence of context calls and the same return
 * value. The engine will re-execute this method from its first line on every
 * workflow turn.
 *
 * <p>Forbidden inside {@code execute}:
 * <ul>
 *   <li>{@code System.currentTimeMillis()} or any wall-clock read
 *   <li>{@code UUID.randomUUID()} or any random value
 *   <li>Direct I/O, network calls, or database access
 *   <li>Reading mutable global or static state
 * </ul>
 *
 * <p>Because this is a {@code @FunctionalInterface}, callers may supply a
 * lambda expression instead of a named implementing class.
 *
 * <p>Note: this interface is intentionally separate from the Phase 1
 * {@link WorkflowDefinition}. Phase 1 is a learning baseline with a generic
 * {@code <I, O>} signature. Phase 3 uses {@code String} inputs and outputs to
 * keep the replay context simple during this learning phase.
 */
@FunctionalInterface
public interface ReplayableWorkflowDefinition {

    /**
     * Executes the workflow logic for the given input via the supplied context.
     *
     * <p>This method will be called from the first line on every replay turn.
     * Use {@link WorkflowContext#executeActivity(String, String)} to interact
     * with the outside world; do not perform I/O directly.
     *
     * @param context the replay context; use it to schedule activities and
     *                signal workflow completion
     * @param input   the workflow input; treated as opaque data in this phase
     * @return the workflow result; may be null if this workflow produces no output
     */
    String execute(WorkflowContext context, String input);
}
