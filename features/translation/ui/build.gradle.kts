import app.katari.gradle.extensions.desktop

plugins {
    alias(katari.plugins.kotlin.multiplatform)
    alias(katari.plugins.compose)
    alias(katari.plugins.spotless)
}

kotlin {
    android {
        namespace = "app.katari.translation.ui"

        withHostTest { }
    }
    desktop()

    sourceSets {
        commonMain {
            dependencies {
                api(projects.features.translation.api)

                implementation(projects.core.platform)
                implementation(projects.core.icons.materialSymbols)
                implementation(projects.core.presentationCore)
                implementation(libs.compose.multiplatform.runtime)
                implementation(libs.compose.multiplatform.foundation)
                implementation(libs.compose.multiplatform.navigationEvent)
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        androidMain {
            dependencies {
                implementation(libs.androidx.core)
            }
        }
        getByName("androidHostTest") {
            dependencies {
                implementation(libs.bundles.test)
                implementation(libs.kotlinx.coroutines.test)
                runtimeOnly(libs.junit.platform.launcher)
            }
        }
    }
}
