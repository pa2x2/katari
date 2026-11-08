// The synchronization protocol, defined once for the server and every app client: what the server stores and orders
// (record envelopes, devices, positions, files), what it answers with, and the hybrid clock that orders field edits
// inside payloads. Plain JVM modules, such as the server, consume its desktop target.
import app.katari.gradle.extensions.desktop

plugins {
    alias(katari.plugins.kotlin.multiplatform)
    alias(katari.plugins.spotless)

    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "app.katari.sync.protocol"

        withHostTest { }
    }
    desktop()

    sourceSets {
        commonMain {
            dependencies {
                api(libs.kotlinx.serialization.protobuf)
            }
        }
        commonTest {
            dependencies {
                implementation(libs.bundles.test)
                runtimeOnly(libs.junit.platform.launcher)
            }
        }
    }

    compilerOptions {
        optIn.add("kotlinx.serialization.ExperimentalSerializationApi")
    }
}
