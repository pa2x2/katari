plugins {
    alias(mihonx.plugins.android.library)
    alias(mihonx.plugins.spotless)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "mihon.text.recognition.runtime"
}

dependencies {
    api(projects.textRecognition.api)

    implementation(projects.core.common)
    implementation(projects.featureGraph)
    implementation(projects.featureRuntime)
    implementation(projects.textRecognition.spi)
    implementation(libs.diskLruCache)
    implementation(libs.injekt)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.bundles.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}
