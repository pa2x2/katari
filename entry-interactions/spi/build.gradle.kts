plugins {
    alias(mihonx.plugins.android.library)
    alias(mihonx.plugins.compose)
    alias(mihonx.plugins.spotless)
}

android {
    namespace = "mihon.entry.interactions.spi"
}

dependencies {
    api(projects.entryInteractions.api)
    api(libs.androidx.appCompat)

    implementation(libs.injekt)
}
