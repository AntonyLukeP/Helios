package io.helios.core.engine;

import io.helios.core.workflow.WorkflowDefinition;
import java.util.Objects;

/**
 * Phase 1 engine: a single, minimal invocation boundary for sequential workflow execution.
 *
 * <p>Responsibilities of this class:
 * <ol>
 *   <li>Validate that the workflow definition is not {@code null}.
 *   <li>Invoke the workflow with the provided input.
 *   <li>Return the result to the caller unchanged.
 * </ol>
 *
 * <p>Phase 1 deliberately does not persist state, retry on failure, schedule activities,
 * wait for signals or timers, or record any event history. These are Phase 2+ concerns.
 */
public final class HeliosEngine {

    /**
     * Runs a workflow definition with the given input and returns its result.
     *
     * <p>Call path: {@code run} validates the workflow is non-null, then delegates
     * directly to {@link WorkflowDefinition#execute(Object)} with the unmodified input,
     * and returns whatever {@code execute} returns without further transformation.
     *
     * @param workflow the workflow definition to execute; must not be {@code null}
     * @param input    the input passed to the workflow; may be {@code null} if the
     *                 workflow's own contract permits it
     * @param <I>      the workflow input type
     * @param <O>      the workflow output type
     * @return the result produced by the workflow
     * @throws NullPointerException if {@code workflow} is {@code null}
     */
    public <I, O> O run(WorkflowDefinition<I, O> workflow, I input) {
        Objects.requireNonNull(workflow, "workflow must not be null");
        return workflow.execute(input);
    }
}
