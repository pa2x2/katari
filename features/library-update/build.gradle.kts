import mihon.gradle.extensions.desktop

// Library updates as both platforms run them: which entries an update checks, the pass that checks them, the queue
// that runs every profile's updates one after another, and the metadata refresh. Each platform supplies the work chain
// the queue runs on and the notifications; the screens that start and show updates stay in app-ui.
plugins {
    alias(mihonx.plugins.kotlin.multiplatform)
    alias(mihonx.plugins.spotless)
    alias(libs.plugins.metro)
}

kotlin {
    android {
        namespace = "app.katari.library.update"

        withHostTest { }
    }
    desktop()

    sourceSets {
        commonMain {
            dependencies {
                api(projects.core.domain)
                api(projects.features.entryInteractions.api)

                implementation(projects.core.common)
                implementation(projects.core.platform)
                // Profiles and the active profile, which every update runs for.
                implementation(projects.core.data)
                implementation(projects.core.i18n)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.datetime)
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
