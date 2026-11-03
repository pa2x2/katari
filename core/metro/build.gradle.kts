import mihon.gradle.extensions.desktop
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(mihonx.plugins.kotlin.multiplatform)
    alias(mihonx.plugins.spotless)

    alias(libs.plugins.metro)
}

kotlin {
    android {
        namespace = "mihon.core.metro"
    }
    desktop()

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    dependencies {
        api(libs.metrox.viewmodel)
    }
}
