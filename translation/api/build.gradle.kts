plugins {
    alias(mihonx.plugins.android.library)
    alias(mihonx.plugins.spotless)
}

android {
    namespace = "mihon.translation.api"
}

dependencies {
    testImplementation(libs.bundles.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}
