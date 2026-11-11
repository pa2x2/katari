package app.katari.gradle.syncserver

import app.katari.gradle.extensions.katari
import app.katari.gradle.javaruntime.PinnedJavaRuntime
import app.katari.gradle.javaruntime.pinnedJavaRuntimeLauncher
import app.katari.gradle.javaruntime.pinnedRuntimeArchitecture
import app.katari.gradle.javaruntime.provisionJavaRuntime
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.withType

/**
 * Runs this project's tests on the Temurin runtime the sync server's image ships, pinned in
 * `gradle/katari.versions.toml`, so they run on the Java the server runs on for its users. The build workflow's image
 * check fails when the image's runtime is another one.
 */
fun Project.testOnSyncServerRuntime() {
    val runtime = syncServerRuntime()
    val provision = provisionJavaRuntime(runtime)
    val launcher = pinnedJavaRuntimeLauncher(runtime)
    tasks.withType<Test>().configureEach {
        dependsOn(provision)
        javaLauncher.set(launcher)
    }
}

/** The pinned runtime's JRE archive for this machine, from Adoptium's release of it on GitHub. */
private fun Project.syncServerRuntime(): PinnedJavaRuntime {
    val architecture = pinnedRuntimeArchitecture()
        ?: error("The sync server's runtime is pinned for x64 and aarch64 Linux, not ${System.getProperty("os.arch")}")
    val pin = katari.versions.sync.server.runtime
    val version = pin.asProvider().get()
    val feature = version.takeWhile { it.isDigit() }
    val archive = "OpenJDK${feature}U-jre_${architecture}_linux_hotspot_${version.replace('+', '_')}"
    val release = "jdk-${version.replace("+", "%2B")}"
    return PinnedJavaRuntime(
        name = "the sync server's Temurin runtime",
        url = "https://github.com/adoptium/temurin$feature-binaries/releases/download/$release/$archive.tar.gz",
        digest = when (architecture) {
            "x64" -> pin.linux.x64.sha256.get()
            else -> pin.linux.aarch64.sha256.get()
        },
        digestAlgorithm = "SHA-256",
        home = gradle.gradleUserHomeDir.resolve("caches/katari/temurin/$archive"),
        version = version,
        vendor = "Eclipse Adoptium",
        provisionTaskName = "provisionSyncServerRuntime",
    )
}
