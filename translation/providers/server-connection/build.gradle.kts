import mihon.gradle.extensions.desktop

plugins {
    alias(mihonx.plugins.kotlin.multiplatform)
    alias(mihonx.plugins.compose)
    alias(mihonx.plugins.spotless)

    alias(libs.plugins.metro)
}

kotlin {
    android {
        namespace = "mihon.translation.provider.server"
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
                api(projects.i18n)
                api(projects.translation.api)
                api(projects.translation.spi)
                api(libs.kotlinx.serialization.json)
                api(libs.okhttp.core)
                // A provider's setup is a screen of its own.
                api(libs.voyager.navigator)

                implementation(projects.icons.materialSymbols)
                implementation(projects.presentationCore)
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
