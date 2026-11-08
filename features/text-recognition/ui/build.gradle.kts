import mihon.gradle.extensions.desktop

plugins {
    alias(mihonx.plugins.kotlin.multiplatform)
    alias(mihonx.plugins.compose)
    alias(mihonx.plugins.spotless)
}

kotlin {
    android {
        namespace = "app.katari.text.recognition.ui"
    }
    desktop()

    sourceSets {
        commonMain {
            dependencies {
                api(projects.features.textRecognition.api)
                api(projects.features.modelArtifacts.api)

                implementation(projects.core.icons.materialSymbols)
                implementation(projects.features.modelArtifacts.ui)
                implementation(projects.core.presentationCore)
                implementation(libs.compose.multiplatform.runtime)
                implementation(libs.compose.multiplatform.foundation)
                implementation(libs.kotlinx.coroutines.core)
            }
        }
    }
}
