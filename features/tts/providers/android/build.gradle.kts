plugins {
    alias(mihonx.plugins.android.library)
    alias(mihonx.plugins.spotless)

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
