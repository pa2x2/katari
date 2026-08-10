plugins {
    alias(mihonx.plugins.android.library)
    alias(mihonx.plugins.spotless)
}

android {
    namespace = "mihon.tts.provider.android"
}

dependencies {
    implementation(projects.featureRuntime)
    implementation(projects.tts.api)
    implementation(projects.tts.runtime)
    implementation(projects.tts.spi)
    implementation(libs.kotlinx.coroutines.android)
}
