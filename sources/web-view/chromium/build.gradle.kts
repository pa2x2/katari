// The Chromium the desktop web view downloads instead of carrying: which one, its download, and its installation in
// the app's data folder.
plugins {
    alias(katari.plugins.kotlin.jvm)
    alias(katari.plugins.spotless)
}

dependencies {
    // The download itself: a pinned checksum and size, resuming, typed failures
    api(projects.features.modelArtifacts.api)

    implementation(projects.core.archive)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.logcat)

    testImplementation(libs.bundles.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}
