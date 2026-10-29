// The desktop extension compatibility harness, a developer tool that is never part of the app or a package: it installs
// the extensions of the stores in a list through the desktop host, on folders of its own, calls each source and writes
// a report. It has no tests, since all it does needs the network and live stores.
// Run: ./gradlew :extension-runtime:smoke:run --args="--stores <file> --reports <directory> [options]"
import mihon.gradle.extensions.extensionHostJvmArguments

plugins {
    alias(mihonx.plugins.kotlin.jvm)
    alias(mihonx.plugins.spotless)
    alias(libs.plugins.kotlin.serialization)
    application
}

application {
    mainClass = "mihon.extension.smoke.MainKt"
    applicationDefaultJvmArgs = extensionHostJvmArguments + listOf(
        // A few hundred extensions loaded at once, and a few converted at a time
        "-Xmx2g",
        "-Djava.awt.headless=true",
        // QuickJS, for LNReader plugins, loads its native library with System.load
        "--enable-native-access=ALL-UNNAMED",
    )
}

// It runs from the build only and is never packaged, so the application plugin's archives aren't made
tasks.distZip { enabled = false }
tasks.distTar { enabled = false }

dependencies {
    implementation(projects.desktop)
    implementation(projects.extensionRuntime)
    implementation(projects.extensionRuntime.jvmHost)
    implementation(projects.core.common)
    implementation(projects.domain)
    implementation(projects.entrySourceApi)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.logcat)
    implementation(libs.okhttp.core)
}
