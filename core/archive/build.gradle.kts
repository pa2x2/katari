import mihon.gradle.extensions.desktop

plugins {
    alias(mihonx.plugins.kotlin.multiplatform)
    alias(mihonx.plugins.spotless)

    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "mihon.core.archive"
    }
    desktop()

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.jsoup)
            }
        }
        androidMain {
            dependencies {
                implementation(libs.archive)
                implementation(libs.unifile)
            }
        }
        getByName("desktopMain") {
            dependencies {
                // The system's libarchive, which desktop reads archives with as Android does with its own build
                implementation(libs.jna)
            }
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}
