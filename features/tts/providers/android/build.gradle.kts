plugins {
    alias(katari.plugins.android.library)
    alias(katari.plugins.spotless)

    alias(libs.plugins.metro)
}

android {
    namespace = "app.katari.tts.provider.android"
}

dependencies {
    implementation(projects.features.tts.api)
    implementation(projects.features.tts.spi)
    implementation(libs.kotlinx.coroutines.android)
}
