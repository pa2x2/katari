import app.katari.gradle.extensions.desktop

plugins {
    alias(katari.plugins.kotlin.multiplatform)
    alias(katari.plugins.spotless)

    alias(libs.plugins.metro)
}

kotlin {
    android {
        namespace = "app.katari.tts.runtime"

        withHostTest { }
    }
    desktop()

    sourceSets {
        commonMain {
            dependencies {
                api(projects.features.tts.api)

                implementation(projects.core.common)
                implementation(projects.features.language.runtime)
                implementation(projects.features.tts.spi)
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        commonTest {
            dependencies {
                implementation(libs.bundles.test)
                runtimeOnly(libs.junit.platform.launcher)
            }
        }
    }
}
