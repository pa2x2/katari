import app.katari.gradle.extensions.desktop

plugins {
    alias(katari.plugins.kotlin.multiplatform)
    alias(katari.plugins.compose)
    alias(katari.plugins.spotless)
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
