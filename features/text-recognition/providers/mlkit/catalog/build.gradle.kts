plugins {
    alias(mihonx.plugins.android.library)
    alias(mihonx.plugins.spotless)
}

android {
    namespace = "app.katari.text.recognition.provider.mlkit.catalog"
}

dependencies {
    implementation(projects.features.textRecognition.api)
}
