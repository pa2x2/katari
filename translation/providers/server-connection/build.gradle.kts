plugins {
    alias(mihonx.plugins.android.library)
    alias(mihonx.plugins.compose)
    alias(mihonx.plugins.spotless)
}

android {
    namespace = "mihon.translation.provider.server"
}

dependencies {
    implementation(projects.featureRuntime)
    implementation(projects.presentationCore)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.materialIcons)
    implementation(libs.androidx.compose.uiToolingPreview)
    api(projects.translation.api)
    api(projects.translation.spi)
    api(libs.kotlinx.serialization.json)
    api(libs.okhttp.core)

    testImplementation(libs.bundles.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver3)
    testRuntimeOnly(libs.junit.platform.launcher)

    debugImplementation(libs.androidx.compose.uiTooling)
}
