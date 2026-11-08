import mihon.gradle.extensions.desktop

// The database side of the Entry features: the hosts they reach the library, merges, migrations, removals and profile
// moves through. Kept apart from the umbrella entry-interactions module, which has no database access, and from the
// screens in app-ui, so that screens cannot reach them.
plugins {
    alias(mihonx.plugins.kotlin.multiplatform)
    alias(mihonx.plugins.spotless)
    alias(libs.plugins.metro)
}

kotlin {
    android {
        namespace = "app.katari.entry.interactions.host"

        withHostTest { }
    }
    desktop()

    sourceSets {
        commonMain {
            dependencies {
                api(projects.features.entryInteractions.api)

                implementation(projects.core.common)
                implementation(projects.core.platform)
                implementation(projects.core.data)
                implementation(projects.core.domain)
                implementation(libs.kotlinx.coroutines.core)
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
