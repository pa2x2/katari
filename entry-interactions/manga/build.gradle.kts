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

                implementation(projects.core.common)
                implementation(projects.i18n)
                implementation(projects.icons.materialSymbols)
                implementation(libs.bundles.serialization)
            }
        }
        androidMain {
            // The reader's Compose and Coil APIs; the shared code uses neither.
            languageSettings {
                optIn("androidx.compose.foundation.ExperimentalFoundationApi")
                optIn("androidx.compose.foundation.layout.ExperimentalLayoutApi")
                optIn("androidx.compose.ui.ExperimentalComposeUiApi")
                optIn("coil3.annotation.ExperimentalCoilApi")
            }
            dependencies {
                implementation(projects.core.archive)
                implementation(projects.core.metro)
                implementation(projects.coreMetadata)
                implementation(projects.entryViewerSettingsUi)
                implementation(projects.icons.customMaterialSymbols)
                implementation(projects.language.api)
                implementation(projects.modelArtifacts.ui)
                implementation(projects.textRecognition.api)
                implementation(projects.textRecognition.ui)
                implementation(projects.translation.api)
                implementation(projects.translation.ui)
                implementation(projects.tts.api)
                implementation(projects.tts.ui)
                implementation(projects.entryViewerChromeUi)
                implementation(projects.presentationCore)
                implementation(projects.sourceApi)
                implementation(projects.sourceCompat)
                implementation(projects.sourceLocal)

                implementation(libs.androidx.activity.compose)
                implementation(libs.androidx.annotation)
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
                implementation(libs.diskLruCache)
                implementation(libs.image.decoder)
                implementation(libs.material)
                api(libs.metrox.viewmodel)
                implementation(libs.photoView)
                implementation(libs.unifile)
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

// A source set's dependency block cannot configure a catalog dependency, so these two take their exclusions here.
dependencies {
    "androidMainImplementation"(libs.directionalViewPager) {
        exclude(group = "androidx.viewpager", module = "viewpager")
    }
    "androidMainImplementation"(libs.subsamplingScaleImageView) {
        exclude(module = "image-decoder")
    }
}
