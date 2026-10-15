plugins {
    alias(mihonx.plugins.android.library)
    alias(mihonx.plugins.spotless)

    alias(libs.plugins.metro)
}

android {
    namespace = "mihon.text.recognition.provider.mlkit.catalog"
}

dependencies {
    implementation(projects.textRecognition.api)
    implementation(projects.textRecognition.spi)
}
