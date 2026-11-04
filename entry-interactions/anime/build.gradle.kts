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
        namespace = "mihon.entry.interactions.anime"
        // The player activity's theme and transitions, the benchmark build's paused start and ExoPlayer's audio
        // attribution are Android resources of this module.
        androidResources {
            enable = true
        }

        // The shared tests also run on the Android host, where the player's problems and engine switch reasons load
        // their strings from i18n's Android resources.
        withHostTest {
            isIncludeAndroidResources = true
        }
    }
    desktop()

    compilerOptions {
        optIn.addAll(
            "kotlinx.coroutines.ExperimentalCoroutinesApi",
            "kotlinx.coroutines.FlowPreview",
            "androidx.compose.foundation.ExperimentalFoundationApi",
            "androidx.compose.foundation.layout.ExperimentalLayoutApi",
            "androidx.compose.ui.ExperimentalComposeUiApi",
        )
    }

    sourceSets {
        commonMain {
            dependencies {
                api(projects.entryInteractions.spi)
                api(projects.domain)
                api(libs.kotlinx.coroutines.core)

                implementation(projects.core.common)
                implementation(projects.core.metro)
                implementation(projects.core.platform)
                implementation(projects.entryViewerChromeUi)
                implementation(projects.i18n)
                implementation(projects.icons.materialSymbols)
                implementation(projects.presentationCore)
                implementation(projects.sourceApi)
                implementation(projects.videoEngine.dash)
                implementation(projects.videoEngine.spi)
                implementation(libs.bundles.serialization)
                implementation(libs.coil.compose)
                implementation(libs.compose.multiplatform.animation)
                implementation(libs.compose.multiplatform.foundation)
                implementation(libs.compose.multiplatform.navigationEvent)
                implementation(libs.compose.multiplatform.runtime)
                implementation(libs.compose.multiplatform.ui)
                implementation(libs.androidx.lifecycle.viewmodel)
                implementation(libs.androidx.lifecycle.viewmodelSavedstate)
            }
        }
        androidMain {
            dependencies {
                implementation(projects.videoEngine.mpv)

                implementation(libs.androidx.activity.compose)
                implementation(libs.androidx.annotation)
                implementation(libs.androidx.appCompat)
                implementation(libs.androidx.compose.animation)
                implementation(libs.androidx.compose.foundation)
                implementation(libs.androidx.compose.uiToolingPreview)
                implementation(libs.androidx.core)
                implementation(libs.androidx.media3.datasourceOkhttp)
                implementation(libs.androidx.media3.exoplayer)
                implementation(libs.androidx.media3.exoplayerDash)
                implementation(libs.androidx.media3.exoplayerHls)
                implementation(libs.androidx.media3.session)
                implementation(libs.bundles.androidx.lifecycle)
                api(libs.metrox.viewmodel)
            }
        }
        commonTest {
            dependencies {
                implementation(libs.bundles.test)
                implementation(libs.kotlinx.coroutines.test)
                runtimeOnly(libs.junit.platform.launcher)
            }
        }
        getByName("androidHostTest") {
            dependencies {
                implementation(libs.junit4)
                implementation(libs.robolectric)
                implementation(libs.unifile)
                runtimeOnly(libs.junit.vintage)
            }
        }
    }
}
