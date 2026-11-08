import app.katari.gradle.extensions.desktop

plugins {
    alias(katari.plugins.kotlin.multiplatform)
    alias(katari.plugins.compose)
    alias(katari.plugins.spotless)
    alias(libs.plugins.metro)
}

kotlin {
    android {
        namespace = "app.katari.ui"
        // The screens' drawables are Compose resources, which Android packages as the module's assets; values the app's
        // build types override are Android resources.
        androidResources {
            enable = true
        }

        // The recap story test names its types' units through i18n's strings, which load from its Android resources.
        withHostTest {
            isIncludeAndroidResources = true
        }
    }
    desktop()

    sourceSets {
        commonMain {
            dependencies {
                api(projects.core.metro)
                api(projects.core.presentationCore)
                // Screen and Tab, which every app screen extends, are Voyager's.
                api(libs.bundles.voyager)

                api(projects.features.appUpdate)
                implementation(projects.core.common)
                implementation(projects.core.image)
                implementation(projects.core.platform)
                // The debug info shows the backup format's schema.
                implementation(projects.features.backup)
                // The profile manager reads and writes profiles through the database directly.
                implementation(projects.core.data)
                implementation(projects.core.domain)
                implementation(projects.features.entryInteractions.api)
                implementation(projects.features.entryViewerChromeUi)
                implementation(projects.features.entryViewerSettingsUi)
                // Extension and source icons load through the extension host.
                implementation(projects.sources.extensionRuntime)
                implementation(projects.core.i18n)
                implementation(projects.features.libraryUpdate)
                // Source errors from script sources, such as LNReader plugins, are worded by their kind.
                implementation(projects.sources.scriptRuntime)
                implementation(projects.core.icons.customMaterialSymbols)
                implementation(projects.core.icons.materialSymbols)
                implementation(projects.core.icons.simpleIcons)
                implementation(projects.features.modelArtifacts.ui)
                // A source's settings screen shows the preference tree the source settings host reads.
                implementation(projects.sources.sourceCompat)
                implementation(projects.features.sync.client)
                // The sync device token is kept in the store for secrets that translation servers' keys use.
                implementation(projects.features.translation.providers.serverConnection)
                implementation(projects.features.textRecognition.ui)
                implementation(projects.features.translation.ui)
                implementation(projects.features.tts.ui)

                implementation(libs.compose.multiplatform.runtime)
                implementation(libs.compose.multiplatform.foundation)
                implementation(libs.compose.multiplatform.animation)
                implementation(libs.compose.multiplatform.uiUtil)
                implementation(libs.compose.multiplatform.navigationEvent)
                implementation(libs.compose.multiplatform.resources)
                implementation(libs.androidx.lifecycle.runtimeCompose)
                implementation(libs.metrox.viewmodel.compose)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.collections.immutable)
                implementation(libs.composeMaterialMotion)
                implementation(libs.reorderable)
                // The upcoming calendar's month grid.
                implementation(libs.composeGrid)
                implementation(libs.coil.compose)
                // Source images, such as an entry's background art, load through their source's HTTP client.
                implementation(libs.coil.network.okhttp)
                // Entry descriptions are Markdown.
                implementation(libs.bundles.markdown)
                // Entry notes are edited as rich text.
                implementation(libs.composeRichEditor)
                // Merge targets are ranked by how closely their titles match the search.
                implementation(libs.stringSimilarity)
                implementation(libs.kotlinx.datetime)
                implementation(libs.materialKolor)
                implementation(libs.aboutLibraries.core)
                implementation(libs.bundles.serialization)
                // Catalogues, feeds and paged filters load their items page by page.
                implementation(libs.androidx.paging.compose)
            }
        }
        commonTest {
            dependencies {
                // ComicInfo is read back through the app's XML configuration.
                implementation(projects.core.coreMetadata)
                implementation(projects.core.testFixtures)
                implementation(libs.bundles.test)
                implementation(libs.kotlinx.coroutines.test)
                runtimeOnly(libs.junit.platform.launcher)
            }
        }
        androidMain {
            dependencies {
                implementation(libs.androidx.activity.compose)
                implementation(libs.androidx.core)
                // The home tabs' animated icons.
                implementation(libs.compose.multiplatform.animationGraphics)
                // The cover viewer zooms with Android's photo view.
                implementation(libs.photoView)
                // A new device scans a linked device's sync code with the camera, which ZXing reads.
                implementation(libs.androidx.camera.camera2)
                implementation(libs.androidx.camera.compose)
                implementation(libs.androidx.camera.lifecycle)
                implementation(libs.zxing.core)
            }
        }
        getByName("desktopMain") {
            dependencies {
                // The ICU that Android's date and list formatting is built on.
                implementation(libs.icu4j)
            }
        }
    }
}

compose.resources {
    packageOfResClass = "app.katari.ui.resources"
}
