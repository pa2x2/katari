import mihon.gradle.extensions.desktop

plugins {
    alias(mihonx.plugins.kotlin.multiplatform)
    alias(mihonx.plugins.compose)
    alias(mihonx.plugins.spotless)
}

kotlin {
    android {
        namespace = "mihon.model.artifacts.ui"
    }
    desktop()

    sourceSets {
        commonMain {
            dependencies {
                api(projects.modelArtifacts.api)

                implementation(projects.presentationCore)
                implementation(libs.compose.multiplatform.runtime)
                implementation(libs.compose.multiplatform.foundation)
            }
        }
    }
}
