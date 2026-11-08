plugins {
    alias(katari.plugins.android.library)
    alias(katari.plugins.spotless)

    alias(libs.plugins.metro)
}

android {
    namespace = "app.katari.text.recognition.provider.mlkit"

    defaultConfig {
        consumerProguardFiles("consumer-proguard.pro")
    }
}

dependencies {
    implementation(projects.features.textRecognition.api)
    implementation(projects.features.textRecognition.providers.mlkit.catalog)
    implementation(projects.features.textRecognition.spi)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.mlkit.text.recognition.chinese)
    implementation(libs.mlkit.text.recognition.devanagari)
    implementation(libs.mlkit.text.recognition.japanese)
    implementation(libs.mlkit.text.recognition.korean)
    implementation(libs.mlkit.text.recognition.latin)
}
