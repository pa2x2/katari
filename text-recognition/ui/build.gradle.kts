import mihon.gradle.extensions.desktop

plugins {
    alias(mihonx.plugins.kotlin.multiplatform)
    alias(mihonx.plugins.compose)
    alias(mihonx.plugins.spotless)
}

kotlin {
    android {
        namespace = "mihon.text.recognition.ui"
    }
    desktop()

    sourceSets {
        commonMain {
            dependencies {
                api(projects.textRecognition.api)
                api(projects.modelArtifacts.api)

                implementation(projects.icons.materialSymbols)
                implementation(projects.modelArtifacts.ui)
                implementation(projects.presentationCore)
                implementation(libs.compose.multiplatform.runtime)
                implementation(libs.compose.multiplatform.foundation)
                implementation(libs.kotlinx.coroutines.core)
            }
        }
    }
}
