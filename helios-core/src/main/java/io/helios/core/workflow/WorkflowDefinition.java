package io.helios.core.workflow;

/**
 * A workflow definition: pure business logic from typed input {@code I} to typed output {@code O}.
 *
 * <p>The definition owns the business logic only. The engine
 * ({@link io.helios.core.engine.HeliosEngine}) owns the invocation boundary.
 *
 * <p>Because this is a {@code @FunctionalInterface}, callers may supply a lambda expression
 * instead of writing a named implementing class.
 *
 * @param <I> the type of input the workflow accepts
 * @param <O> the type of output the workflow produces
 */
@FunctionalInterface
public interface WorkflowDefinition<I, O> {

    /**
     * Executes the workflow logic for the given input and returns its result.
     *
     * @param input the workflow input; may be {@code null} if this definition accepts null
     * @return the workflow output
     */
    O execute(I input);
}
