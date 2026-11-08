import app.katari.gradle.extensions.desktop

plugins {
    alias(katari.plugins.kotlin.multiplatform)
    alias(katari.plugins.compose)
    alias(katari.plugins.spotless)
}

kotlin {
    android {
        namespace = "app.katari.core.image"
    }
    desktop()

    sourceSets {
        // The fork publishes each platform's decoder on its own, without metadata a common source set could compile
        // against, but under the same names on both; the code translating its types is compiled into each platform.
        val forkMapping = "src/forkMapping/kotlin"
        androidMain { kotlin.srcDir(forkMapping) }
        getByName("desktopMain") { kotlin.srcDir(forkMapping) }
        commonMain {
            dependencies {
                implementation(projects.core.common)
                api(libs.kotlinx.coroutines.core)
                // Decoded pixels are handed to Compose as its ImageBitmap, and regions are Compose's IntRect.
                api(libs.compose.multiplatform.ui)
                implementation(libs.compose.multiplatform.runtime)
            }
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}

// A source set's dependency block cannot configure a catalog dependency. The Android decoder is named as an AAR, which
// Gradle can't tell without metadata.
dependencies {
    "androidMainImplementation"(libs.image.decoder) {
        artifact { type = "aar" }
    }
    "desktopMainImplementation"(libs.image.decoder.jvm)
}
