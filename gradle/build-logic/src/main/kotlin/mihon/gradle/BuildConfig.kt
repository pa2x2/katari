package mihon.gradle

import org.gradle.api.Project

interface BuildConfig {
    val includeTelemetry: Boolean
    val enableUpdater: Boolean
    val includeDependencyInfo: Boolean
}

val Project.Config: BuildConfig get() = object : BuildConfig {
    override val includeTelemetry: Boolean = providers.gradleProperty("include-telemetry").isPresent
    override val enableUpdater: Boolean = providers.gradleProperty("enable-updater").isPresent
    override val includeDependencyInfo: Boolean = providers.gradleProperty("include-dependency-info").isPresent
}
