import app.katari.gradle.extensions.desktop
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(katari.plugins.kotlin.multiplatform)
    alias(katari.plugins.spotless)
}

kotlin {
    android {
        namespace = "app.katari.translation.api"
    }
    desktop()

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    dependencies {
        api(projects.features.language.api)
        api(projects.core.common)
        api(projects.core.platform)
        // An engine's glyph is a Compose resource of the provider's module.
        api(libs.compose.multiplatform.resources)
        implementation(libs.androidx.annotation)
    }
}
