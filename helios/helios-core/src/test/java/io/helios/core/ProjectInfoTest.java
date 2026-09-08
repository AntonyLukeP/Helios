package io.helios.core;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ProjectInfo}.
 *
 * <p>This test class has two roles:
 *
 * <ol>
 *   <li>Prove that the JUnit 5 platform, AssertJ, and the Java 21 toolchain are wired correctly.
 *   <li>Document the expected values of the project's compile-time constants.
 * </ol>
 *
 * <p>No workflow, storage, or framework logic is tested here.
 */
@DisplayName("ProjectInfo constants")
class ProjectInfoTest {

    @Test
    @DisplayName("NAME equals the canonical project identifier 'helios'")
    void projectNameIsHelios() {
        Assertions.assertThat(ProjectInfo.NAME)
                .as("ProjectInfo.NAME must equal the canonical project name")
                .isEqualTo("helios");
    }

    @Test
    @DisplayName("VERSION_MAJOR is 0 — no stable release has been cut yet")
    void majorVersionIsZero() {
        Assertions.assertThat(ProjectInfo.VERSION_MAJOR)
                .as("Major version stays 0 until the first public stable release")
                .isZero();
    }

    @Test
    @DisplayName("VERSION_MINOR is 1 — initial development phase has begun")
    void minorVersionIsOne() {
        // This test documents intent: minor == 1 means 'in active early development'.
        // AssertJ's failure message will print the actual value, making regressions obvious.
        Assertions.assertThat(ProjectInfo.VERSION_MINOR)
                .as("Minor version should be 1 while initial features are being built")
                .isEqualTo(1);
    }
}
