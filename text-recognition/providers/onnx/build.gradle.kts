plugins {
    alias(mihonx.plugins.android.library)
    alias(mihonx.plugins.spotless)

    alias(libs.plugins.metro)
}

android {
    namespace = "mihon.text.recognition.provider.onnx"

    defaultConfig {
        consumerProguardFiles("consumer-proguard.pro")
    }
}

dependencies {
    implementation(projects.textRecognition.api)
    implementation(projects.textRecognition.spi)
    implementation(libs.kotlinx.coroutines.core)
    // Its GitHub release has no metadata naming the artifact's type.
    implementation(libs.onnxruntime.android) {
        artifact { type = "aar" }
    }

    testImplementation(libs.bundles.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}
