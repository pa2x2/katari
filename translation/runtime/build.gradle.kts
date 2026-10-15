plugins {
    alias(mihonx.plugins.android.library)
    alias(mihonx.plugins.spotless)

    alias(libs.plugins.metro)
}

android {
    namespace = "mihon.translation.runtime"
}

dependencies {
    api(projects.translation.api)

    implementation(projects.language.runtime)
    implementation(projects.featureGraph)
    implementation(projects.featureRuntime)
    implementation(projects.core.common)
    implementation(projects.translation.spi)
    implementation(libs.androidx.appCompat)
    implementation(libs.diskLruCache)

    testImplementation(libs.bundles.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}
