import app.katari.gradle.extensions.desktop
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(katari.plugins.kotlin.multiplatform)
    alias(katari.plugins.spotless)
}

kotlin {
    android {
        namespace = "app.katari.model.artifacts.api"
    }
    desktop()

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    dependencies {
        api(libs.kotlinx.coroutines.core)
    }
}
