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
                api(projects.presentationCore)
                // Screen and Tab, which every app screen extends, are Voyager's.
                api(libs.bundles.voyager)

                api(projects.appUpdate)
                implementation(projects.core.common)
                implementation(projects.core.image)
                implementation(projects.core.platform)
                // The debug info shows the backup format's schema.
                implementation(projects.backup)
                // The profile manager reads and writes profiles through the database directly.
                implementation(projects.data)
                implementation(projects.domain)
                implementation(projects.entryInteractions.api)
                implementation(projects.entryViewerChromeUi)
                implementation(projects.entryViewerSettingsUi)
                // Extension and source icons load through the extension host.
                implementation(projects.extensionRuntime)
                implementation(projects.i18n)
                implementation(projects.libraryUpdate)
                // Source errors from script sources, such as LNReader plugins, are worded by their kind.
                implementation(projects.scriptRuntime)
                implementation(projects.icons.customMaterialSymbols)
                implementation(projects.icons.materialSymbols)
                implementation(projects.icons.simpleIcons)
                implementation(projects.modelArtifacts.ui)
                // A source's settings screen shows the preference tree the source settings host reads.
                implementation(projects.sourceCompat)
                implementation(projects.sync.client)
                // The sync device token is kept in the store for secrets that translation servers' keys use.
                implementation(projects.translation.providers.serverConnection)
                implementation(projects.textRecognition.ui)
                implementation(projects.trackers)
                implementation(projects.translation.ui)
                implementation(projects.tts.ui)

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
                implementation(projects.coreMetadata)
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
    packageOfResClass = "mihon.app.ui.resources"
}
