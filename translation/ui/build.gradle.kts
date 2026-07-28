plugins {
    alias(mihonx.plugins.android.library)
    alias(mihonx.plugins.compose)
    alias(mihonx.plugins.spotless)
}

android {
    namespace = "mihon.translation.ui"
}

dependencies {
    implementation(projects.translation.api)
}
