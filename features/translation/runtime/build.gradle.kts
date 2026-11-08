import mihon.gradle.extensions.desktop

plugins {
    alias(mihonx.plugins.kotlin.multiplatform)
    alias(mihonx.plugins.spotless)

    alias(libs.plugins.metro)
}

kotlin {
    android {
        namespace = "mihon.translation.runtime"
        // The Android robot of the system engine is an Android resource.
        androidResources {
            enable = true
        }

        withHostTest { }
    }
    desktop()

    sourceSets {
        commonMain {
            dependencies {
                api(projects.features.translation.api)

                implementation(projects.core.common)
                implementation(projects.core.platform)
                implementation(projects.features.language.runtime)
                implementation(projects.features.translation.spi)
                implementation(libs.diskLruCache)
            }
        }
        androidMain {
            dependencies {
                implementation(libs.androidx.annotation)
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
