// The desktop extension compatibility harness, a developer tool that is never part of the app or a package: it installs
// the extensions of the stores in a list through the desktop host, on folders of its own, calls each source and writes
// a report. It has no tests, since all it does needs the network and live stores.
// Run: ./gradlew :tools:extension-compatibility-harness:run --args="--stores <file> --reports <directory> [options]"
import app.katari.gradle.extensions.extensionHostJvmArguments
import app.katari.gradle.jetbrainsruntime.launchOnJetBrainsRuntime

plugins {
    alias(katari.plugins.kotlin.jvm)
    alias(katari.plugins.spotless)
    alias(libs.plugins.kotlin.serialization)
    application
}

application {
    mainClass = "app.katari.tools.extensioncompatibility.MainKt"
    applicationDefaultJvmArgs = extensionHostJvmArguments + listOf(
        // A few hundred extensions loaded at once, and a few converted at a time
        "-Xmx2g",
        "-Djava.awt.headless=true",
        // QuickJS, for LNReader plugins, loads its native library with System.load
        "--enable-native-access=ALL-UNNAMED",
    )
}

// On the runtime the desktop app runs on, so extensions meet the JVM they meet in the app
launchOnJetBrainsRuntime(tasks.named<JavaExec>("run"))

// It runs from the build only and is never packaged, so the application plugin's archives aren't made
tasks.distZip { enabled = false }
tasks.distTar { enabled = false }

dependencies {
    implementation(projects.desktop)
    implementation(projects.sources.extensionRuntime)
    implementation(projects.sources.extensionRuntime.jvmHost)
    implementation(projects.core.common)
    implementation(projects.core.domain)
    implementation(projects.sources.entrySourceApi)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.logcat)
    implementation(libs.okhttp.core)
}
