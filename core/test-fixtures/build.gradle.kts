import mihon.gradle.extensions.desktop

// Test doubles shared by more than one module's tests. Only test source sets depend on this module.
plugins {
    alias(mihonx.plugins.kotlin.multiplatform)
    alias(mihonx.plugins.spotless)
}

kotlin {
    android {
        namespace = "mihon.core.testfixtures"
    }
    desktop()

    sourceSets {
        commonMain {
            dependencies {
                api(projects.core.common)
                implementation(libs.kotlinx.coroutines.core)
            }
        }
    }
}
