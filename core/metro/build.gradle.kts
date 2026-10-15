plugins {
    alias(mihonx.plugins.android.library)
    alias(mihonx.plugins.spotless)

    alias(libs.plugins.metro)
}

android {
    namespace = "mihon.core.metro"
}

dependencies {
    api(libs.metrox.viewmodel)
}
