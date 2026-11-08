import app.katari.gradle.extensions.desktop
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(katari.plugins.kotlin.multiplatform)
    alias(katari.plugins.spotless)
}

kotlin {
    android {
        namespace = "app.katari.text.recognition.spi"
    }
    desktop()

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    dependencies {
        api(projects.features.textRecognition.api)
        api(libs.kotlinx.coroutines.core)
    }
}
