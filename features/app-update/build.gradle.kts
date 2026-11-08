import app.katari.gradle.extensions.desktop

// How the apps update themselves from their GitHub releases, as both platforms do it: finding the newest release of
// the running app, ordering versions, cleaning up release notes and downloading the release's file. Each platform picks
// the file meant for its build and installs it.
plugins {
    alias(katari.plugins.kotlin.multiplatform)
    alias(katari.plugins.spotless)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "app.katari.appupdate"

        withHostTest { }
    }
    desktop()

    sourceSets {
        commonMain {
            dependencies {
                api(projects.core.common)
            }
        }
        commonTest {
            dependencies {
                implementation(libs.bundles.test)
                runtimeOnly(libs.junit.platform.launcher)
            }
        }
    }
}
