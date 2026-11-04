import mihon.gradle.extensions.desktop

plugins {
    alias(mihonx.plugins.kotlin.multiplatform)
    alias(mihonx.plugins.spotless)
}

kotlin {
    android {
        namespace = "mihon.video.engine.spi"

        // TODO(antsy): Remove when https://youtrack.jetbrains.com/issue/KT-83319 is resolved
        withHostTest { }
    }
    desktop()

    sourceSets {
        commonMain {
            dependencies {
                api(projects.entrySourceApi)
                api(libs.kotlinx.coroutines.core)
            }
        }
        androidMain {
            dependencies {
                api(libs.androidx.media3.common)
                implementation(libs.kotlinx.coroutines.android)
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
