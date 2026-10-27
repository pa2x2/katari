import mihon.gradle.extensions.desktop

plugins {
    alias(mihonx.plugins.kotlin.multiplatform)
    alias(mihonx.plugins.spotless)

    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.metro)
    alias(libs.plugins.sqldelight)
}

kotlin {
    android {
        namespace = "tachiyomi.data"

        // TODO(antsy): Remove when https://youtrack.jetbrains.com/issue/KT-83319 is resolved
        withHostTest { }
    }
    desktop()

    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.entryViewerSettingsApi)
                implementation(projects.entrySourceApi)
                implementation(projects.sourceApi)
                implementation(projects.domain)
                implementation(projects.core.common)

                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.serialization.jsonOkio)
                implementation(libs.kotlinx.serialization.protobuf)

                implementation(libs.kotlinx.datetime)

                api(libs.bundles.sqldelight)
                implementation(libs.androidx.sqlite.bundled)
            }
        }
        commonTest {
            dependencies {
                implementation(libs.bundles.test)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.sqldelight.sqliteDriver)
                runtimeOnly(libs.junit.platform.launcher)
            }
        }
    }

    compilerOptions {
        optIn.add("kotlinx.serialization.ExperimentalSerializationApi")
    }
}

sqldelight {
    databases {
        create("Database") {
            packageName.set("tachiyomi.data")
            dialect(libs.sqldelight.sqliteDialect338)
            schemaOutputDirectory.set(project.file("./src/commonMain/sqldelight"))
            generateAsync.set(true)
        }
    }
}
