import app.katari.gradle.extensions.desktop

plugins {
    alias(katari.plugins.kotlin.multiplatform)
    alias(katari.plugins.compose)
    alias(katari.plugins.spotless)
}

kotlin {
    android {
        namespace = "app.katari.entry.viewer.chrome.ui"
    }
    desktop()

    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi",
            "-opt-in=androidx.compose.ui.ExperimentalComposeUiApi",
        )
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.core.i18n)
                implementation(projects.core.icons.materialSymbols)
                implementation(projects.core.presentationCore)

                implementation(libs.compose.multiplatform.runtime)
                implementation(libs.compose.multiplatform.animation)
                implementation(libs.compose.multiplatform.foundation)
                implementation(libs.compose.multiplatform.navigationEvent)
                implementation(libs.coil.compose)
            }
        }
        getByName("desktopMain") {
            dependencies {
                implementation(projects.core.platform)
            }
        }
    }
}
