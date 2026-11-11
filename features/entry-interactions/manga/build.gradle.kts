import app.katari.gradle.extensions.desktop

plugins {
    alias(katari.plugins.kotlin.multiplatform)
    alias(katari.plugins.compose)
    alias(katari.plugins.spotless)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.metro)
}

kotlin {
    android {
        namespace = "app.katari.entry.interactions.manga"
        // The reader's layouts, animations and S Pen actions are Android resources of this module.
        androidResources {
            enable = true
        }

        withHostTest { }
    }
    desktop()

    compilerOptions {
        optIn.addAll("kotlinx.coroutines.ExperimentalCoroutinesApi", "kotlinx.coroutines.FlowPreview")
    }

    sourceSets {
        commonMain {
            dependencies {
                api(projects.features.entryInteractions.spi)
                api(projects.core.domain)
                api(libs.kotlinx.coroutines.core)

                implementation(projects.core.archive)
                implementation(projects.core.common)
                implementation(projects.core.image)
                implementation(projects.core.metro)
                implementation(projects.core.platform)
                implementation(projects.core.coreMetadata)
                implementation(projects.features.entryViewerChromeUi)
                implementation(projects.features.entryViewerSettingsUi)
                implementation(projects.core.i18n)
                implementation(projects.core.platformPresentation)
                implementation(projects.core.icons.customMaterialSymbols)
                implementation(projects.core.icons.materialSymbols)
                implementation(projects.features.language.api)
                implementation(projects.features.modelArtifacts.api)
                implementation(projects.features.modelArtifacts.ui)
                implementation(projects.core.presentationCore)
                implementation(projects.sources.sourceApi)
                implementation(projects.sources.sourceLocal)
                implementation(projects.features.textRecognition.api)
                implementation(projects.features.textRecognition.ui)
                implementation(projects.features.translation.api)
                implementation(projects.features.translation.ui)
                implementation(projects.features.tts.api)
                implementation(projects.features.tts.ui)
                implementation(libs.bundles.serialization)
                implementation(libs.androidx.annotation)
                implementation(libs.androidx.lifecycle.viewmodel)
                implementation(libs.androidx.lifecycle.viewmodelSavedstate)
                implementation(libs.compose.multiplatform.animation)
                implementation(libs.compose.multiplatform.foundation)
                implementation(libs.compose.multiplatform.navigationEvent)
                implementation(libs.compose.multiplatform.runtime)
                implementation(libs.compose.multiplatform.ui)
                implementation(libs.diskLruCache)
                api(libs.metrox.viewmodel)
            }
        }
        androidMain {
            // The reader's Compose UI and Coil APIs, which only the Android viewers use.
            languageSettings {
                optIn("androidx.compose.foundation.ExperimentalFoundationApi")
                optIn("androidx.compose.foundation.layout.ExperimentalLayoutApi")
                optIn("androidx.compose.ui.ExperimentalComposeUiApi")
                optIn("coil3.annotation.ExperimentalCoilApi")
            }
            dependencies {
                implementation(projects.sources.sourceCompat)

                implementation(libs.androidx.activity.compose)
                implementation(libs.androidx.appCompat)
                implementation(libs.androidx.compose.foundation)
                implementation(libs.androidx.compose.animation)
                implementation(libs.androidx.compose.animationGraphics)
                implementation(libs.androidx.compose.uiToolingPreview)
                implementation(libs.androidx.compose.uiUtil)
                implementation(libs.androidx.core)
                implementation(libs.androidx.interpolator)
                implementation(libs.bundles.androidx.lifecycle)
                implementation(libs.bundles.coil)
                implementation(libs.material)
            }
        }
        getByName("desktopMain") {
            dependencies {
                // The reader's ViewModel, made with its arguments in the screen that shows it
                implementation(libs.metrox.viewmodel.compose)
            }
        }
        getByName("androidHostTest") {
            dependencies {
                implementation(libs.bundles.test)
                implementation(libs.kotlinx.coroutines.test)
                runtimeOnly(libs.junit.platform.launcher)
            }
        }
    }
}

// A source set's dependency block cannot configure a catalog dependency, so the decoder takes its artifact type here:
// it is an AAR, which Gradle can't tell without metadata.
dependencies {
    "androidMainImplementation"(libs.image.decoder) {
        artifact { type = "aar" }
    }
}
