plugins {
    alias(mihonx.plugins.android.library)
    alias(mihonx.plugins.spotless)

    alias(libs.plugins.metro)
}

android {
    namespace = "app.katari.text.recognition.provider.mlkit.notincluded"
}

dependencies {
    implementation(projects.features.textRecognition.api)
    implementation(projects.features.textRecognition.providers.mlkit.catalog)
    implementation(projects.features.textRecognition.spi)
}
