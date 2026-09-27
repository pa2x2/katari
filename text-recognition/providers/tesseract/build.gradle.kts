plugins {
    alias(mihonx.plugins.android.library)
    alias(mihonx.plugins.spotless)
}

android {
    namespace = "mihon.text.recognition.provider.tesseract"
}

dependencies {
    implementation(projects.featureRuntime)
    implementation(projects.textRecognition.api)
    implementation(projects.textRecognition.runtime)
    implementation(projects.textRecognition.spi)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.tesseract4android)

    testImplementation(libs.bundles.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}
