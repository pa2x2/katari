import mihon.gradle.extensions.desktop

plugins {
    alias(mihonx.plugins.kotlin.multiplatform)
    alias(mihonx.plugins.compose)
    alias(mihonx.plugins.spotless)
}

kotlin {
    android {
        namespace = "mihon.entry.viewer.chrome.ui"
        // The return-position bar's labels are Android string resources of this module.
        androidResources {
            enable = true
        }
    }
    desktop()

    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi",
        )
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.i18n)
                implementation(projects.icons.materialSymbols)
                implementation(projects.presentationCore)

                implementation(libs.compose.multiplatform.runtime)
                implementation(libs.compose.multiplatform.animation)
                implementation(libs.compose.multiplatform.foundation)
                implementation(libs.coil.compose)
            }
        }
    }
}
