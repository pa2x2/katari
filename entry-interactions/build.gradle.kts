import mihon.gradle.extensions.compileSharedCodeAgainstAndroidApi
import mihon.gradle.extensions.desktop

plugins {
    alias(mihonx.plugins.kotlin.multiplatform)
    alias(mihonx.plugins.spotless)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.metro)
}

kotlin {
    android {
        namespace = "mihon.entry.interactions"

        withHostTest {
            isIncludeAndroidResources = true
        }
    }
    desktop()

    sourceSets {
        commonMain {
            dependencies {
                api(projects.entryInteractions.api)
                api(projects.entryViewerSettingsApi)
                // Metro aggregates contributions from the app graph's compile classpath, so the type modules have to
                // reach it. The boundary check still keeps application sources from importing them.
                api(projects.entryInteractions.manga)
                api(projects.entryInteractions.anime)
                api(projects.entryInteractions.book)

                implementation(projects.core.common)
                implementation(projects.core.metro)
                implementation(projects.entryInteractions.spi)
                implementation(projects.sourceLocal)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.bundles.serialization)
            }
        }
        androidMain {
            dependencies {
                implementation(projects.entryInteractions.downloadNotification)
                implementation(projects.sourceCompat)
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
                implementation(projects.tts.api)
                runtimeOnly(libs.junit.platform.launcher)
            }
        }
        getByName("androidHostTest") {
            dependencies {
                implementation(libs.unifile)
                implementation(libs.androidx.work.testing)
                implementation(libs.junit4)
                implementation(libs.robolectric)
                runtimeOnly(libs.junit.vintage)
            }
        }
    }
}

// The source settings and tracker adapter features read sources' SharedPreferences
compileSharedCodeAgainstAndroidApi()
