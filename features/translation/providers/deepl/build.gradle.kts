import app.katari.gradle.extensions.desktop

plugins {
    alias(katari.plugins.kotlin.multiplatform)
    alias(katari.plugins.compose)
    alias(katari.plugins.spotless)

    alias(libs.plugins.metro)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "app.katari.translation.provider.deepl"
        // The engine's glyph is a Compose resource, which Android packages as the module's assets.
        androidResources {
            enable = true
        }

        withHostTest { }
    }
    desktop()

    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.core.i18n)
                implementation(projects.core.presentationCore)
                implementation(projects.features.translation.api)
                implementation(projects.features.translation.spi)
                implementation(projects.features.translation.providers.serverConnection)
                implementation(libs.compose.multiplatform.runtime)
                implementation(libs.compose.multiplatform.resources)
                implementation(libs.metrox.viewmodel.compose)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.okhttp.core)
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
    packageOfResClass = "app.katari.translation.provider.deepl.resources"
}
