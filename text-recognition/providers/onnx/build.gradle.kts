plugins {
    alias(mihonx.plugins.android.library)
    alias(mihonx.plugins.spotless)
}

android {
    namespace = "mihon.text.recognition.provider.onnx"
}

dependencies {
    implementation(projects.featureRuntime)
    implementation(projects.textRecognition.api)
    implementation(projects.textRecognition.runtime)
    implementation(projects.textRecognition.spi)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.onnxruntime.android)

    testImplementation(libs.bundles.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}
