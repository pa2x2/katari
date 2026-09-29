plugins {
    alias(mihonx.plugins.android.library)
    alias(mihonx.plugins.spotless)
}

android {
    namespace = "mihon.feature.runtime"
}

dependencies {
    api(projects.core.common)
    api(projects.featureGraph)
    api(libs.injekt)
}
