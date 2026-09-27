plugins {
    alias(mihonx.plugins.android.library)
    alias(mihonx.plugins.spotless)
}

android {
    namespace = "mihon.text.recognition.provider.mlkit.catalog"
}

dependencies {
    implementation(projects.featureRuntime)
    implementation(projects.textRecognition.api)
    implementation(projects.textRecognition.runtime)
    implementation(projects.textRecognition.spi)
}
