import app.katari.gradle.extensions.desktop

plugins {
    alias(katari.plugins.kotlin.multiplatform)
    alias(katari.plugins.compose)
    alias(katari.plugins.spotless)

    alias(libs.plugins.metro)
}

kotlin {
    android {
        namespace = "app.katari.translation.provider.server"
        // The providers' setup activities share this module's theme, an Android resource.
        androidResources {
            enable = true
        }

        withHostTest { }
    }
    desktop()

    sourceSets {
        commonMain {
            dependencies {
                api(projects.core.common)
                api(projects.core.i18n)
                api(projects.features.translation.api)
                api(projects.features.translation.spi)
                api(libs.kotlinx.serialization.json)
                api(libs.okhttp.core)
                // A provider's setup is a screen of its own.
                api(libs.voyager.navigator)

                implementation(projects.core.icons.materialSymbols)
                implementation(projects.core.presentationCore)
                implementation(libs.compose.multiplatform.runtime)
                implementation(libs.compose.multiplatform.foundation)
            }
        }
        androidMain {
            dependencies {
                implementation(projects.core.metro)
                implementation(libs.androidx.activity.compose)
                implementation(libs.androidx.appCompat)
                implementation(libs.metrox.viewmodel.compose)
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
