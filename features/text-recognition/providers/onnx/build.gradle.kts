import app.katari.gradle.extensions.desktop

plugins {
    alias(katari.plugins.kotlin.multiplatform)
    alias(katari.plugins.spotless)

    alias(libs.plugins.metro)
}

kotlin {
    android {
        namespace = "app.katari.text.recognition.provider.onnx"
        optimization {
            consumerKeepRules.apply {
                publish = true
                file("consumer-proguard.pro")
            }
        }

        withHostTest { }
    }
    desktop()

    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.features.textRecognition.api)
                implementation(projects.features.textRecognition.spi)
                implementation(libs.kotlinx.coroutines.core)
                // The shared code calls ONNX Runtime's Java API, which the Android AAR and the Linux jar both carry
                // from the same build; each platform runs the libraries of its own.
                compileOnly(libs.onnxruntime.linux.x64)
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

// A source set's dependency block cannot configure a catalog dependency. The AAR's GitHub release has no metadata
// naming the artifact's type.
dependencies {
    "androidMainImplementation"(libs.onnxruntime.android) {
        artifact { type = "aar" }
    }
    "desktopMainImplementation"(libs.onnxruntime.linux.x64)
}
