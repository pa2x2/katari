import mihon.gradle.extensions.desktop
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(mihonx.plugins.kotlin.multiplatform)
    alias(mihonx.plugins.spotless)
}

kotlin {
    android {
        namespace = "mihon.tts.spi"
    }
    desktop()

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    dependencies {
        api(projects.tts.api)
        api(libs.kotlinx.coroutines.core)
        // For @PlatformFacility, which the compiler drops, so core/common isn't needed at run time.
        compileOnly(projects.core.common)
    }
}
