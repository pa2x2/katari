import mihon.gradle.extensions.desktop

plugins {
    alias(mihonx.plugins.kotlin.multiplatform)
    alias(mihonx.plugins.compose)
    alias(mihonx.plugins.spotless)

    alias(libs.plugins.metro)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "mihon.translation.provider.libretranslate"
        // The server engine's glyph is a Compose resource, which Android packages as the module's assets; the Offline
        // Translator's strings and glyph are Android resources.
        androidResources {
            enable = true
        }

        withHostTest { }
    }
    desktop()

    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.i18n)
                implementation(projects.presentationCore)
                implementation(projects.translation.api)
                implementation(projects.translation.spi)
                implementation(projects.translation.providers.serverConnection)
                implementation(libs.compose.multiplatform.runtime)
                implementation(libs.compose.multiplatform.foundation)
                implementation(libs.compose.multiplatform.resources)
                implementation(libs.metrox.viewmodel.compose)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.okhttp.core)
            }
        }
        androidMain {
            dependencies {
                implementation(libs.androidx.appCompat)
                implementation(libs.androidx.activity.compose)
                implementation(libs.androidx.lifecycle.runtime)
            }
        }
        commonTest {
            dependencies {
                implementation(libs.bundles.test)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.okhttp.mockwebserver3)
                runtimeOnly(libs.junit.platform.launcher)
            }
        }
    }
}

compose.resources {
    packageOfResClass = "mihon.translation.provider.libretranslate.resources"
}
