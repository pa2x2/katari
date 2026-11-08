import app.katari.gradle.extensions.desktop
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(katari.plugins.kotlin.multiplatform)
    alias(katari.plugins.spotless)
}

kotlin {
    android {
        namespace = "app.katari.tts.spi"
    }
    desktop()

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    dependencies {
        api(projects.features.tts.api)
        api(libs.kotlinx.coroutines.core)
        // For @PlatformFacility, which the compiler drops, so core/common isn't needed at run time.
        compileOnly(projects.core.common)
    }
}
