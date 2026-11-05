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
        namespace = "mihon.entry.interactions.book"
        withHostTest { }
    }
    desktop()

    sourceSets {
        commonMain {
            dependencies {
                api(projects.bookApi)
                api(projects.domain)
                api(projects.entryInteractions.spi)
                api(projects.entryViewerSettingsApi)
                api(libs.kotlinx.coroutines.core)

                implementation(projects.core.common)
                implementation(projects.core.metro)
                implementation(projects.core.platform)
                implementation(projects.entryViewerChromeUi)
                implementation(projects.entryViewerSettingsUi)
                implementation(projects.i18n)
                implementation(projects.icons.materialSymbols)
                implementation(projects.language.api)
                implementation(projects.presentationCore)
                // The automatic translation setting asks the translation host whether an engine is ready.
                implementation(projects.translation.api)
                implementation(projects.translation.ui)
                implementation(projects.tts.api)
                implementation(projects.tts.ui)
                implementation(libs.brotli.dec)
                implementation(libs.bundles.serialization)
                implementation(libs.androidx.lifecycle.viewmodel)
                implementation(libs.androidx.lifecycle.viewmodelSavedstate)
                implementation(libs.compose.multiplatform.animation)
                implementation(libs.compose.multiplatform.foundation)
                implementation(libs.compose.multiplatform.navigationEvent)
                implementation(libs.compose.multiplatform.runtime)
                implementation(libs.compose.multiplatform.ui)
                implementation(libs.jsoup)
            }
        }
        androidMain {
            dependencies {
                implementation(libs.androidx.activity.compose)
                implementation(libs.androidx.compose.foundation)
                implementation(libs.androidx.core)
                implementation(libs.androidx.fragment)
                implementation(libs.androidsvg)
            }
        }
        getByName("desktopMain") {
            dependencies {
                // The reader's retained sessions, kept by the screen that shows it
                implementation(libs.metrox.viewmodel.compose)
            }
        }
        commonTest {
            dependencies {
                implementation(libs.bundles.test)
                implementation(libs.kotlinx.coroutines.test)
                implementation(kotlin("test"))
                runtimeOnly(libs.junit.platform.launcher)
            }
        }
        getByName("androidHostTest") {
            dependencies {
                implementation(libs.unifile)
                implementation(libs.junit4)
                implementation(libs.robolectric)
                runtimeOnly(libs.junit.vintage)
            }
        }
    }
}

// A source set's dependency block cannot configure a catalog dependency. Named as an AAR, which Gradle can't tell
// without metadata.
dependencies {
    "androidMainImplementation"(libs.image.decoder) {
        artifact { type = "aar" }
    }
}

tasks.withType<Test>().configureEach {
    // MockK/Byte Buddy instruments the test JVM, which is incompatible with class-data sharing.
    jvmArgs("-XX:+EnableDynamicAgentLoading", "-Xshare:off")
}
