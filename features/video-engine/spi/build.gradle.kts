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
                api(projects.sources.entrySourceApi)
                api(libs.kotlinx.coroutines.core)
            }
        }
        getByName("desktopMain") {
            dependencies {
                // Skia's image type, which a Compose window's video output hands its frames over as
                api(libs.compose.multiplatform.ui)
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
