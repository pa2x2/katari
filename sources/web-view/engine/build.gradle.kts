// What the desktop web view does with Chromium whichever way Chromium is run: mirroring the app's cookies into it,
// the DevTools protocol messages that set a page's user agent and run its scripts, an extension page's bridges and
// loads, and the data a Chromium profile keeps.
plugins {
    alias(katari.plugins.kotlin.jvm)
    alias(katari.plugins.spotless)
}

dependencies {
    api(projects.core.common)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.logcat)

    testImplementation(libs.bundles.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}
