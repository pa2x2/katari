import app.katari.gradle.extensions.compileSharedCodeAgainstAndroidApi
import app.katari.gradle.extensions.desktop

// The web pages sources reach through the SDK's WebPageSource: the keeper both apps share, and each platform's hidden
// page under it.
plugins {
    alias(katari.plugins.kotlin.multiplatform)
    alias(katari.plugins.spotless)
}

kotlin {
    android {
        namespace = "app.katari.webpages"

        // TODO(antsy): Remove when https://youtrack.jetbrains.com/issue/KT-83319 is resolved
        withHostTest { }
    }
    desktop()

    sourceSets {
        commonMain {
            dependencies {
                api(projects.sources.entrySourceApi)
                implementation(projects.core.common)
            }
        }
        androidMain {
            dependencies {
                implementation(libs.androidx.webkit)
            }
        }
        commonTest {
            dependencies {
                implementation(libs.bundles.test)
                implementation(libs.kotlinx.coroutines.test)
                runtimeOnly(libs.junit.platform.launcher)
            }
        }
    }
}

compileSharedCodeAgainstAndroidApi()
