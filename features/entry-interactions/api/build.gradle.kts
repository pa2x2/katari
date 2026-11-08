import mihon.gradle.extensions.compileSharedCodeAgainstAndroidApi
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
                api(projects.core.image)
                api(projects.core.platform)
                api(projects.core.domain)
                api(projects.features.entryViewerSettingsApi)
                api(projects.sources.entrySourceApi)
                api(projects.core.i18n)
                api(projects.features.language.api)
                api(projects.features.textRecognition.api)
                api(projects.features.translation.api)
                api(libs.kotlinx.coroutines.core)

                implementation(projects.core.icons.customMaterialSymbols)
                implementation(projects.core.icons.materialSymbols)
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

// The source settings contract hands on the SharedPreferences and preference screens sources build
compileSharedCodeAgainstAndroidApi()
