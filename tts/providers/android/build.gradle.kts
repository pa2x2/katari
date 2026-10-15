plugins {
    alias(mihonx.plugins.android.library)
    alias(mihonx.plugins.spotless)

    alias(libs.plugins.metro)
}

android {
    namespace = "mihon.tts.provider.android"
}

dependencies {
    implementation(projects.tts.api)
    implementation(projects.tts.spi)
    implementation(libs.kotlinx.coroutines.android)
}
