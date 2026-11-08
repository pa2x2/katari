import mihon.gradle.extensions.desktop

// The app's half of sync, the same on every platform: record keys, capture of local edits to rows and preferences into
// the outbox while sync is on, the outbox drain that stamps them with hybrid timestamps, the payload of every synced
// kind, merging and applying other devices' records, and talking to the server: the connection, the runs and when
// they happen. Platforms supply only their facilities (background work, the device's name). The tables it keeps its
// state in are part of the app database in :data.
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
                api(projects.features.sync.protocol)

                implementation(projects.core.common)
                // Sync waits for networks and Wi-Fi as the platform reports them.
                implementation(projects.core.platform)
                implementation(projects.core.data)
                implementation(projects.core.domain)
                implementation(projects.sources.entrySourceApi)
                implementation(projects.sources.extensionRuntime)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.serialization.protobuf)
                implementation(libs.okio)
            }
        }
        getByName("desktopMain") {
            dependencies {
                // Source settings are Android's shared preferences, which the desktop gets as stand-ins
                implementation(projects.sources.extensionRuntime.androidCompat)
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
                implementation(projects.sources.extensionRuntime.jvmHost)
                // Transport is tested against the real server, started in the test's process.
                implementation(projects.features.sync.server)
            }
        }
    }

    compilerOptions {
        optIn.add("kotlinx.serialization.ExperimentalSerializationApi")
        optIn.add("kotlin.uuid.ExperimentalUuidApi")
    }
}
