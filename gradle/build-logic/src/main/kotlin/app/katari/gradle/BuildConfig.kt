package app.katari.gradle

import org.gradle.api.Project

interface BuildConfig {
    val includeTelemetry: Boolean

    /**
     * The token of Katari's PostHog project, which only the release workflow passes. A build that carries telemetry but
     * no token sends nothing.
     */
    val postHogProjectToken: String?

    val includeDependencyInfo: Boolean

    /** Whether the desktop app is built as the release, rather than as the development build every other build is. */
    val desktopRelease: Boolean
}

val Project.Config: BuildConfig get() = object : BuildConfig {
    override val includeTelemetry: Boolean = providers.gradleProperty("include-telemetry").isPresent
    override val postHogProjectToken: String? = providers.gradleProperty("posthog-project-token").orNull
    override val includeDependencyInfo: Boolean = providers.gradleProperty("include-dependency-info").isPresent
    override val desktopRelease: Boolean = providers.gradleProperty("desktop-release").isPresent
}
