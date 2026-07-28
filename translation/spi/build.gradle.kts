plugins {
    alias(mihonx.plugins.android.library)
    alias(mihonx.plugins.spotless)
}

android {
    namespace = "mihon.translation.spi"
}

dependencies {
    api(projects.translation.api)
    api(libs.kotlinx.coroutines.core)
}
