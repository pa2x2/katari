plugins {
    alias(mihonx.plugins.android.library)
    alias(mihonx.plugins.spotless)
}

android {
    namespace = "mihon.tts.runtime"
}

dependencies {
    api(projects.tts.api)

    implementation(projects.core.common)
    implementation(projects.featureGraph)
    implementation(projects.featureRuntime)
    implementation(projects.language.runtime)
    implementation(projects.tts.spi)
    implementation(libs.injekt)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.bundles.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}
