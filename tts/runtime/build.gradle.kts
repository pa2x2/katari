plugins {
    alias(mihonx.plugins.android.library)
    alias(mihonx.plugins.spotless)
}

android {
    namespace = "mihon.tts.runtime"
}

dependencies {
    api(projects.tts.api)

    implementation(projects.featureGraph)
    implementation(projects.tts.spi)
}
