import app.katari.gradle.extensions.desktop

plugins {
    alias(katari.plugins.kotlin.multiplatform)
    alias(katari.plugins.spotless)

    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "tachiyomi.core.metadata"

        // TODO(antsy): Remove when https://youtrack.jetbrains.com/issue/KT-83319 is resolved
        withHostTest { }
    }
    desktop()

    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.sources.entrySourceApi)
                implementation(projects.sources.sourceApi)

                implementation(libs.bundles.serialization)
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
