plugins {
    alias(mihonx.plugins.android.library)
    alias(mihonx.plugins.spotless)
}

android {
    namespace = "mihon.translation.mlkit"
}

dependencies {
    implementation(projects.translation.spi)
}
