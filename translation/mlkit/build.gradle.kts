plugins {
    alias(mihonx.plugins.android.library)
    alias(mihonx.plugins.spotless)
}

android {
    namespace = "mihon.translation.mlkit"
}

dependencies {
    implementation(projects.featureRuntime)
    implementation(projects.translation.runtime)
    implementation(projects.translation.spi)
    implementation(libs.kotlinx.coroutines.playServices)
    implementation(libs.mlkit.language.id)

    testImplementation(libs.bundles.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}
