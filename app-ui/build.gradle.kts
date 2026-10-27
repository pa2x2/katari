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
        // The screens' drawables are Compose resources, which Android packages as the module's assets.
        androidResources {
            enable = true
        }
    }
    desktop()

    sourceSets {
        commonMain {
            dependencies {
                api(projects.core.metro)
                api(projects.presentationCore)
                // Screen and Tab, which every app screen extends, are Voyager's.
                api(libs.bundles.voyager)

                implementation(projects.core.common)
                implementation(projects.core.platform)
                implementation(projects.domain)
                implementation(projects.entryInteractions.api)
                implementation(projects.i18n)
                implementation(projects.icons.materialSymbols)
                implementation(projects.modelArtifacts.ui)
                implementation(projects.textRecognition.ui)
                implementation(projects.trackers)
                implementation(projects.translation.ui)

                implementation(libs.compose.multiplatform.runtime)
                implementation(libs.compose.multiplatform.foundation)
                implementation(libs.compose.multiplatform.animation)
                implementation(libs.compose.multiplatform.uiUtil)
                implementation(libs.compose.multiplatform.navigationEvent)
                implementation(libs.compose.multiplatform.resources)
                implementation(libs.androidx.lifecycle.runtimeCompose)
                implementation(libs.metrox.viewmodel.compose)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.composeMaterialMotion)
                implementation(libs.reorderable)
                implementation(libs.coil.compose)
                implementation(libs.kotlinx.datetime)
            }
        }
    }
}

compose.resources {
    packageOfResClass = "mihon.app.ui.resources"
}
