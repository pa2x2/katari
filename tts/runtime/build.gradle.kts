plugins {
    alias(mihonx.plugins.android.library)
    alias(mihonx.plugins.spotless)

    alias(libs.plugins.metro)
}

android {
    namespace = "mihon.tts.runtime"
}

dependencies {
    api(projects.tts.api)

    implementation(projects.core.common)
    implementation(projects.language.runtime)
    implementation(projects.tts.spi)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.bundles.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}
