import mihon.gradle.extensions.desktop
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(mihonx.plugins.kotlin.multiplatform)
    alias(mihonx.plugins.spotless)
}

kotlin {
    android {
        namespace = "mihon.entry.viewer.settings.api"
    }
    desktop()

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    dependencies {
        api(projects.core.common)
        api(libs.kotlinx.coroutines.core)
    }
}
