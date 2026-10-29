// The desktop extension host: the platform side of the shared host in :extension-runtime, on the Android stand-ins of
// :extension-runtime:android-compat. It reads and verifies apks, converts their dex code to jars and loads them. Paths
// and the app's channels come in from the desktop composition.
plugins {
    alias(mihonx.plugins.kotlin.jvm)
    alias(mihonx.plugins.spotless)
}

dependencies {
    api(projects.extensionRuntime)
    api(projects.extensionRuntime.androidCompat)

    implementation(projects.backup)
    implementation(projects.core.common)
    implementation(projects.domain)
    implementation(projects.entrySourceApi)

    implementation(libs.apksig)
    implementation(libs.asm.commons)
    implementation(libs.dex2jar.translator)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.logcat)

    testImplementation(libs.bundles.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}
