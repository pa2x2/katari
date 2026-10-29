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
        // The reader's strings are Android resources of this module.
        androidResources {
            enable = true
        }

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
                implementation(projects.core.platform)
                implementation(projects.i18n)
                implementation(projects.icons.materialSymbols)
                implementation(projects.language.api)
                // The automatic translation setting asks the translation host whether an engine is ready.
                implementation(projects.translation.api)
                implementation(libs.brotli.dec)
                implementation(libs.bundles.serialization)
                implementation(libs.jsoup)
            }
        }
        androidMain {
            dependencies {
                implementation(projects.core.metro)
                implementation(projects.entryViewerChromeUi)
                implementation(projects.entryViewerSettingsUi)
                implementation(projects.presentationCore)
                implementation(projects.translation.ui)
                implementation(projects.tts.api)
                implementation(projects.tts.ui)

                implementation(libs.androidx.activity.compose)
                implementation(libs.androidx.compose.foundation)
                implementation(libs.androidx.core)
                implementation(libs.androidx.fragment)
                implementation(libs.androidsvg)
                implementation(libs.image.decoder)
                implementation(libs.unifile)
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
                implementation(libs.junit4)
                implementation(libs.robolectric)
                runtimeOnly(libs.junit.vintage)
            }
        }
    }
}

tasks.withType<Test>().configureEach {
    // MockK/Byte Buddy instruments the test JVM, which is incompatible with class-data sharing.
    jvmArgs("-XX:+EnableDynamicAgentLoading", "-Xshare:off")
}
