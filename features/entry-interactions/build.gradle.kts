import app.katari.gradle.extensions.compileSharedCodeAgainstAndroidApi
import app.katari.gradle.extensions.desktop

plugins {
    alias(katari.plugins.kotlin.multiplatform)
    alias(katari.plugins.spotless)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.metro)
}

kotlin {
    android {
        namespace = "app.katari.entry.interactions"

        withHostTest {
            isIncludeAndroidResources = true
        }
    }
    desktop()

    sourceSets {
        commonMain {
            dependencies {
                api(projects.features.entryInteractions.api)
                api(projects.features.entryViewerSettingsApi)
                // Metro aggregates contributions from the app graph's compile classpath, so the type modules have to
                // reach it. The boundary check still keeps application sources from importing them.
                api(projects.features.entryInteractions.manga)
                api(projects.features.entryInteractions.anime)
                api(projects.features.entryInteractions.book)

                implementation(projects.core.common)
                implementation(projects.core.metro)
                implementation(projects.features.entryInteractions.spi)
                implementation(projects.sources.sourceLocal)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.bundles.serialization)
            }
        }
        androidMain {
            dependencies {
                implementation(projects.features.entryInteractions.downloadNotification)
                implementation(projects.sources.sourceCompat)
                implementation(libs.androidx.core)
                implementation(libs.androidx.work)
                implementation(libs.bundles.coil)
            }
        }
        commonTest {
            dependencies {
                implementation(libs.bundles.test)
                implementation(libs.bundles.serialization)
                implementation(libs.kotlinx.coroutines.test)
                implementation(projects.features.tts.api)
                runtimeOnly(libs.junit.platform.launcher)
            }
        }
        getByName("androidHostTest") {
            dependencies {
                implementation(libs.androidx.work.testing)
                implementation(libs.junit4)
                implementation(libs.robolectric)
                runtimeOnly(libs.junit.vintage)
            }
        }
    }
}

// The source settings feature reads sources' SharedPreferences
compileSharedCodeAgainstAndroidApi()
