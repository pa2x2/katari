plugins {
    alias(katari.plugins.android.library)
    alias(katari.plugins.spotless)
}

android {
    namespace = "app.katari.text.recognition.provider.mlkit.catalog"
}

dependencies {
    implementation(projects.features.textRecognition.api)
}
