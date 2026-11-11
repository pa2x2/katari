import app.katari.gradle.extensions.desktop

plugins {
    alias(katari.plugins.kotlin.multiplatform)
    alias(katari.plugins.compose)
    alias(katari.plugins.spotless)
}

kotlin {
    android {
        namespace = "app.katari.platform.presentation"
    }
    desktop()

    sourceSets {
        commonMain {
            dependencies {
                api(projects.core.i18n)
                api(libs.compose.multiplatform.ui)

                implementation(projects.core.icons.materialSymbols)
                implementation(projects.core.icons.platformSymbols)
            }
        }
    }
}
