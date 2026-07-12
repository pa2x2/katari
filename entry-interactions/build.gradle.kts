plugins {
    alias(mihonx.plugins.android.library)
    alias(mihonx.plugins.spotless)
}

android {
    namespace = "mihon.entry.interactions"
}

dependencies {
    api(projects.entryInteractions.api)

    implementation(projects.core.common)
    implementation(projects.entryInteractions.spi)
    implementation(projects.entryInteractions.manga)
    implementation(projects.entryInteractions.anime)
    implementation(libs.androidx.core)
    implementation(libs.injekt)
    implementation(libs.bundles.coil)

    testImplementation(libs.bundles.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}
