package io.helios.core;

/**
 * Compile-time constants describing this project.
 *
 * <p>This is a minimal, non-product class added in Phase 0 to:
 * <ul>
 *   <li>establish the {@code io.helios.core} package namespace, and</li>
 *   <li>give the test suite something concrete to assert against.</li>
 * </ul>
 *
 * <p>It carries no workflow, storage, or framework logic.
 */
public final class ProjectInfo {

    /** The canonical name of this project. */
    public static final String NAME = "helios";

    /** Major version of the Helios engine — incremented on breaking API changes. */
    public static final int VERSION_MAJOR = 0;

    /** Minor version — incremented when new non-breaking features are added. */
    public static final int VERSION_MINOR = 1;

    // Prevent instantiation — this class is a namespace for constants only.
    private ProjectInfo() {
        throw new UnsupportedOperationException("ProjectInfo is a utility class.");
    }
}
