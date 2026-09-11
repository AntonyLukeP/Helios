package io.helios.core.run;

import java.util.Objects;
import java.util.UUID;

/**
 * A stable, unique identifier for one workflow run.
 *
 * <p>{@code WorkflowRunId} is a value object: two instances with the same string
 * value are considered identical, regardless of which object reference holds them.
 * Java records provide this automatically through their generated {@code equals}
 * and {@code hashCode} implementations.
 *
 * <p>The identifier is intentionally opaque to callers. Its internal format (UUID)
 * is an implementation detail; callers must not parse or construct values manually
 * except in tests that verify equality.
 *
 * @param value the underlying string identifier; never null, never blank
 */
public record WorkflowRunId(String value) {

    /** Compact constructor — validates the value on every construction path. */
    public WorkflowRunId {
        Objects.requireNonNull(value, "WorkflowRunId value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("WorkflowRunId value must not be blank");
        }
    }

    /**
     * Creates a new {@code WorkflowRunId} backed by a random UUID.
     *
     * <p>Each call returns a distinct value with overwhelming probability.
     *
     * @return a fresh, unique run identifier
     */
    public static WorkflowRunId newId() {
        return new WorkflowRunId(UUID.randomUUID().toString());
    }
}
