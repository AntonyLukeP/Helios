package io.helios.core;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Smoke-test for the helios-core module.
 *
 * <p>Purpose: prove that the JUnit 5 platform, AssertJ, and the Java 21
 * toolchain are all wired up correctly. This test contains no workflow logic.
 */
@DisplayName("ProjectInfo")
class ProjectInfoTest {

    @Test
    @DisplayName("project name constant equals 'helios'")
    void projectNameIsHelios() {
        Assertions.assertThat(ProjectInfo.NAME)
                .as("ProjectInfo.NAME should be the canonical project name")
                .isEqualTo("helios");
    }

    @Test
    @DisplayName("major version is 0 during initial development")
    void majorVersionIsZero() {
        Assertions.assertThat(ProjectInfo.VERSION_MAJOR)
                .as("Major version must be 0 until the first stable release")
                .isZero();
    }
}
