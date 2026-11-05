import mihon.gradle.extensions.desktop

plugins {
    alias(mihonx.plugins.kotlin.multiplatform)
    alias(mihonx.plugins.compose)
    alias(mihonx.plugins.spotless)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.metro)
}

kotlin {
    android {
        namespace = "mihon.entry.interactions.manga"
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
                api(projects.entryInteractions.spi)
                api(projects.domain)
                api(libs.kotlinx.coroutines.core)

                implementation(projects.core.archive)
                implementation(projects.core.common)
                implementation(projects.core.metro)
                implementation(projects.core.platform)
                implementation(projects.coreMetadata)
                implementation(projects.entryViewerChromeUi)
                implementation(projects.entryViewerSettingsUi)
                implementation(projects.i18n)
                implementation(projects.icons.customMaterialSymbols)
                implementation(projects.icons.materialSymbols)
                implementation(projects.language.api)
                implementation(projects.modelArtifacts.api)
                implementation(projects.modelArtifacts.ui)
                implementation(projects.presentationCore)
                implementation(projects.sourceApi)
                implementation(projects.sourceLocal)
                implementation(projects.textRecognition.api)
                implementation(projects.textRecognition.ui)
                implementation(projects.translation.api)
                implementation(projects.translation.ui)
                implementation(projects.tts.api)
                implementation(projects.tts.ui)
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
                implementation(projects.sourceCompat)

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
                implementation(libs.androidx.recyclerView)
                implementation(libs.androidx.viewPager)
                implementation(libs.bundles.coil)
                implementation(libs.material)
                implementation(libs.photoView)
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

// A source set's dependency block cannot configure a catalog dependency, so these take their exclusions and artifact
// type (the decoder is an AAR, which Gradle can't tell without metadata) here.
dependencies {
    "androidMainImplementation"(libs.directionalViewPager) {
        exclude(group = "androidx.viewpager", module = "viewpager")
    }
    "androidMainImplementation"(libs.subsamplingScaleImageView) {
        exclude(module = "image-decoder")
    }
    "androidMainImplementation"(libs.image.decoder) {
        artifact { type = "aar" }
    }
}
