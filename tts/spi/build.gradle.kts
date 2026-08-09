plugins {
    alias(mihonx.plugins.android.library)
    alias(mihonx.plugins.spotless)
}

android {
    namespace = "mihon.tts.spi"
}

dependencies {
    api(projects.tts.api)
    api(libs.kotlinx.coroutines.core)
}
