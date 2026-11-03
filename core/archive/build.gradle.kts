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
                implementation(libs.jna)
                // libarchive with its compression libraries built in, so x86_64 Linux reads archives whatever the
                // system has, as Android does with its own build
                runtimeOnly(libs.libarchive.desktop)
            }
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}
