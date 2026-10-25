plugins {
    alias(mihonx.plugins.android.library)
    alias(mihonx.plugins.spotless)
}

android {
    namespace = "mihon.video.engine.spi"
}

dependencies {
    api(projects.entrySourceApi)
    api(libs.androidx.media3.common)

    testImplementation(libs.bundles.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}
