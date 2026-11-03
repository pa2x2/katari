import mihon.gradle.extensions.desktop

plugins {
    alias(mihonx.plugins.kotlin.multiplatform)
    alias(mihonx.plugins.compose)
    alias(mihonx.plugins.spotless)
}

kotlin {
    android {
        namespace = "mihon.entry.viewer.settings.ui"
    }
    desktop()

    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi",
            "-opt-in=androidx.compose.ui.ExperimentalComposeUiApi",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.coroutines.FlowPreview",
        )
    }

    sourceSets {
        commonMain {
            dependencies {
                api(projects.entryViewerSettingsApi)

                implementation(projects.i18n)
                implementation(projects.icons.materialSymbols)
                implementation(projects.presentationCore)

                implementation(libs.compose.multiplatform.runtime)
                implementation(libs.compose.multiplatform.foundation)
                implementation(libs.compose.multiplatform.uiUtil)
                implementation(libs.androidx.lifecycle.runtimeCompose)
                implementation(libs.kotlinx.coroutines.core)
            }
        }
    }
}
