plugins {
    alias(mihonx.plugins.android.library)
    alias(mihonx.plugins.spotless)

    alias(libs.plugins.metro)
}

android {
    namespace = "mihon.feature.runtime"
}

dependencies {
    api(projects.core.common)
    api(projects.featureGraph)
    api(libs.injekt)
}
