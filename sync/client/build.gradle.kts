import mihon.gradle.extensions.desktop

// The app's half of sync, the same on every platform: record keys, capture of local edits into the outbox while sync
// is on, the outbox drain that stamps them with hybrid timestamps, and the payload of every synced kind. The tables
// it keeps its state in are part of the app database in :data.
plugins {
    alias(mihonx.plugins.kotlin.multiplatform)
    alias(mihonx.plugins.spotless)

    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.metro)
}

kotlin {
    android {
        namespace = "app.katari.sync.client"

        withHostTest { }
    }
    desktop()

    sourceSets {
        commonMain {
            dependencies {
                api(projects.sync.protocol)

                implementation(projects.core.common)
                implementation(projects.data)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.serialization.protobuf)
                implementation(libs.okio)
            }
        }
        commonTest {
            dependencies {
                implementation(libs.bundles.test)
                implementation(libs.kotlinx.coroutines.test)
                runtimeOnly(libs.junit.platform.launcher)
            }
        }
        getByName("desktopTest") {
            dependencies {
                implementation(projects.domain)
                implementation(projects.entrySourceApi)
            }
        }
    }

    compilerOptions {
        optIn.add("kotlinx.serialization.ExperimentalSerializationApi")
        optIn.add("kotlin.uuid.ExperimentalUuidApi")
    }
}
