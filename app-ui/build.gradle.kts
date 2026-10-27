import mihon.gradle.extensions.desktop

plugins {
    alias(mihonx.plugins.kotlin.multiplatform)
    alias(mihonx.plugins.compose)
    alias(mihonx.plugins.spotless)
    alias(libs.plugins.metro)
}

kotlin {
    android {
        namespace = "mihon.app.ui"
    }
    desktop()

    sourceSets {
        commonMain {
            dependencies {
                api(projects.core.metro)
                api(projects.presentationCore)

                implementation(libs.compose.multiplatform.runtime)
            }
        }
    }
}
