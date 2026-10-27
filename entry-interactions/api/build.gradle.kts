import mihon.gradle.extensions.desktop
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(mihonx.plugins.kotlin.multiplatform)
    alias(mihonx.plugins.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(mihonx.plugins.spotless)
}

kotlin {
    android {
        namespace = "mihon.entry.interactions.api"

        withHostTest { }
    }
    desktop()

    sourceSets {
        commonMain {
            dependencies {
                api(projects.core.common)
                api(projects.core.platform)
                api(projects.domain)
                api(projects.entryViewerSettingsApi)
                api(projects.entrySourceApi)
                api(projects.i18n)
                api(projects.language.api)
                api(projects.textRecognition.api)
                api(projects.translation.api)
                api(libs.kotlinx.coroutines.core)
                // Tracking services carry their logo as a Compose resource.
                api(libs.compose.multiplatform.resources)

                implementation(projects.icons.customMaterialSymbols)
                implementation(projects.icons.materialSymbols)
            }
        }
        commonTest {
            dependencies {
                implementation(libs.bundles.test)
                implementation(libs.kotlinx.coroutines.test)
                runtimeOnly(libs.junit.platform.launcher)
            }
        }
    }
}
