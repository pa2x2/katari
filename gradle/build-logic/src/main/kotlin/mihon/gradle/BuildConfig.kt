package mihon.gradle

import org.gradle.api.Project

interface BuildConfig {
    val includeTelemetry: Boolean
    val includeDependencyInfo: Boolean

    /** Whether the desktop app is built as the release, rather than as the development build every other build is. */
    val desktopRelease: Boolean
}

val Project.Config: BuildConfig get() = object : BuildConfig {
    override val includeTelemetry: Boolean = providers.gradleProperty("include-telemetry").isPresent
    override val includeDependencyInfo: Boolean = providers.gradleProperty("include-dependency-info").isPresent
    override val desktopRelease: Boolean = providers.gradleProperty("desktop-release").isPresent
}
