plugins {
    alias(mihonx.plugins.android.library)
    alias(mihonx.plugins.compose)
    alias(mihonx.plugins.spotless)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "mihon.translation.provider.libretranslate"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.featureRuntime)
    implementation(projects.presentationCore)
    implementation(projects.translation.api)
    implementation(projects.translation.runtime)
    implementation(projects.translation.spi)
    implementation(libs.androidx.appCompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.materialIcons)
    implementation(libs.androidx.compose.uiToolingPreview)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.bundles.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver3)
    testRuntimeOnly(libs.junit.platform.launcher)

    debugImplementation(libs.androidx.compose.uiTooling)
}
