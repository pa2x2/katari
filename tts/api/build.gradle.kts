plugins {
    alias(mihonx.plugins.android.library)
    alias(mihonx.plugins.spotless)
}

android {
    namespace = "mihon.tts.api"
}

dependencies {
    api(projects.core.common)
    api(projects.language.api)
    api(libs.kotlinx.coroutines.core)
}
