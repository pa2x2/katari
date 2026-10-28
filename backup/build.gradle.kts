import mihon.gradle.extensions.desktop

plugins {
    alias(mihonx.plugins.kotlin.multiplatform)
    alias(mihonx.plugins.spotless)

    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.metro)
}

kotlin {
    android {
        namespace = "mihon.backup"

        // Old backups' reader settings convert to reading modes, which load their labels from i18n's Android resources.
        withHostTest {
            isIncludeAndroidResources = true
        }
    }
    desktop()

    sourceSets {
        commonMain {
            dependencies {
                api(projects.core.common)
                api(projects.core.platform)
                api(projects.domain)
                api(projects.entryInteractions.api)
                api(projects.i18n)
                // Creators and restorers read and write the database directly, as backups always have.
                api(projects.data)

                implementation(projects.entrySourceApi)

                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.bundles.serialization)
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

    compilerOptions {
        optIn.add("kotlinx.serialization.ExperimentalSerializationApi")
    }
}
