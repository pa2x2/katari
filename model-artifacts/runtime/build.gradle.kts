import mihon.gradle.extensions.desktop

plugins {
    alias(mihonx.plugins.kotlin.multiplatform)
    alias(mihonx.plugins.spotless)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.metro)
}

kotlin {
    android {
        namespace = "mihon.model.artifacts.runtime"

        withHostTest { }
    }
    desktop()

    sourceSets {
        commonMain {
            dependencies {
                api(projects.modelArtifacts.api)

                implementation(projects.core.common)
                implementation(projects.core.platform)
                implementation(libs.kotlinx.coroutines.core)
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
