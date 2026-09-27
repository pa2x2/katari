plugins {
    alias(mihonx.plugins.android.library)
    alias(mihonx.plugins.spotless)
}

android {
    namespace = "mihon.text.recognition.spi"
}

dependencies {
    api(projects.textRecognition.api)
    api(libs.kotlinx.coroutines.core)
}
