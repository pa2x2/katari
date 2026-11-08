import app.katari.gradle.extensions.desktop

plugins {
    alias(katari.plugins.kotlin.multiplatform)
    alias(katari.plugins.spotless)

    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "app.katari.core.archive"
    }
    desktop()

    sourceSets {
        commonMain {
            dependencies {
                api(projects.core.common)
                implementation(libs.jsoup)
            }
        }
        androidMain {
            dependencies {
                implementation(libs.archive)
            }
        }
        getByName("desktopMain") {
            dependencies {
                // libarchive itself comes with the app, which picks the build for the system it is packaged for
                implementation(libs.jna)
            }
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}
