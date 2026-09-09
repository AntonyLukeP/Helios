package io.helios.core.example;

import io.helios.core.workflow.WorkflowDefinition;

/**
 * Example Phase 1 workflow: greets a person by name.
 *
 * <p>This class demonstrates the minimal shape of a {@link WorkflowDefinition}:
 *
 * <ul>
 *   <li>It accepts a typed input ({@code String} name).
 *   <li>It enforces its own business input contract (non-null, non-blank name).
 *   <li>It performs a pure computation with no I/O, no logging, no clock, no
 *       random values, no global state, and no external calls.
 *   <li>Given the same input it always returns the same output, making it safe
 *       to replay in later phases.
 * </ul>
 *
 * <p>The engine ({@link io.helios.core.engine.HeliosEngine}) validates only that a
 * workflow definition is provided; it has no opinion on what constitutes valid
 * business input. That contract belongs here, close to the logic that depends on it.
 *
 * <p>Usage (unit tests are the executable demonstration in Phase 1):
 *
 * <pre>{@code
 * new HeliosEngine().run(new GreetingWorkflow(), "Ada")  // "Hello, Ada!"
 * }</pre>
 */
public final class GreetingWorkflow implements WorkflowDefinition<String, String> {

    @Override
    public String execute(String name) {
        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        }
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        return "Hello, " + name + "!";
    }
}
