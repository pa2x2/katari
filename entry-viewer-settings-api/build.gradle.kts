plugins {
    alias(mihonx.plugins.android.library)
    alias(mihonx.plugins.spotless)
}

android {
    namespace = "mihon.entry.viewer.settings.api"
}

dependencies {
    api(projects.core.common)
    api(libs.kotlinx.coroutines.core)

    testImplementation(libs.bundles.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}
