plugins {
    alias(mihonx.plugins.android.library)
    alias(mihonx.plugins.compose)
    alias(mihonx.plugins.spotless)
}

android {
    namespace = "mihon.entry.interactions.api"
}

dependencies {
    api(projects.core.common)
    api(projects.domain)
    api(projects.entrySourceApi)
    api(projects.i18n)
    api(libs.kotlinx.coroutines.core)

    implementation(libs.androidx.compose.materialIcons)
    implementation(libs.androidx.core)

    testImplementation(libs.bundles.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}
