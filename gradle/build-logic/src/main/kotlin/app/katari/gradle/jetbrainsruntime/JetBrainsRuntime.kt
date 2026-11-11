package app.katari.gradle.jetbrainsruntime

import app.katari.gradle.extensions.katari
import app.katari.gradle.javaruntime.PinnedJavaRuntime
import app.katari.gradle.javaruntime.pinnedRuntimeArchitecture
import org.gradle.api.Project
import java.io.File

/**
 * The JetBrains Runtime with JCEF that the desktop app and the tools running its code start on, pinned in
 * `gradle/katari.versions.toml`: the JVM that carries Chromium for the desktop web view.
 *
 * `-PsystemJdk` starts them on the JDK running Gradle instead, which has no JCEF, to try the app as it runs there.
 */
internal class JetBrainsRuntime(
    private val version: String,
    private val build: String,
    private val architecture: String,
    private val sha512: String,
    gradleUserHome: File,
    private val flavour: Flavour = Flavour.Runtime,
) {
    val archiveName: String = "${flavour.archivePrefix}-$version-linux-$architecture-$build"

    private val url: String = "$DOWNLOADS/$archiveName.tar.gz"

    /** The runtime's home, its `java.home`. */
    val home: File = gradleUserHome.resolve("caches/katari/jetbrains-runtime/$archiveName")

    /** The runtime's `release` file, which names the JCEF it carries and the modules it has. */
    val release: File get() = home.resolve("release")

    /** This runtime as the build downloads it and starts tasks on it. */
    val pinned = PinnedJavaRuntime(
        name = "the JetBrains Runtime",
        url = url,
        digest = sha512,
        digestAlgorithm = "SHA-512",
        home = home,
        version = "$version+$build",
        vendor = "JetBrains s.r.o.",
        provisionTaskName = flavour.provisionTaskName,
        offlineAlternative = "run with -PsystemJdk to start on the JDK running Gradle",
    )

    /** Both carry JCEF; the plain runtime, without Chromium, is never used. */
    enum class Flavour(val archivePrefix: String, val provisionTaskName: String) {
        /** What the app runs on. */
        Runtime("jbr_jcef", "provisionJetBrainsRuntime"),

        /** The runtime with the JDK's tools and every module's jmod, which `jlink` links a runtime from. */
        Sdk("jbrsdk_jcef", "provisionJetBrainsRuntimeSdk"),
    }

    private companion object {
        const val DOWNLOADS = "https://cache-redirector.jetbrains.com/intellij-jbr"
    }
}

/**
 * Whether `-PsystemJdk` asks to run on the JDK running Gradle instead of the JetBrains Runtime. A bare `-PsystemJdk`
 * sets the property to an empty value, which asks for it as `-PsystemJdk=true` does.
 */
internal val Project.runsOnSystemJdk: Boolean
    get() = providers.gradleProperty("systemJdk").orNull?.let { it.isEmpty() || it.toBoolean() } ?: false

/** The pinned runtime for this machine's architecture. */
internal fun Project.jetBrainsRuntime(): JetBrainsRuntime {
    val architecture = pinnedRuntimeArchitecture() ?: error(
        "The JetBrains Runtime is pinned for x64 and aarch64 Linux, not ${System.getProperty("os.arch")}; " +
            "run with -PsystemJdk",
    )
    val runtime = katari.versions.jbr
    return JetBrainsRuntime(
        version = runtime.asProvider().get(),
        build = runtime.build.get(),
        architecture = architecture,
        sha512 = when (architecture) {
            "x64" -> runtime.linux.x64.sha512.get()
            else -> runtime.linux.aarch64.sha512.get()
        },
        gradleUserHome = gradle.gradleUserHomeDir,
    )
}

/** The pinned runtime's SDK, for x86_64 only: the one architecture the AppImage, which links from it, is built for. */
internal fun Project.jetBrainsRuntimeSdk(): JetBrainsRuntime {
    val runtime = katari.versions.jbr
    return JetBrainsRuntime(
        version = runtime.asProvider().get(),
        build = runtime.build.get(),
        architecture = "x64",
        sha512 = runtime.sdk.linux.x64.sha512.get(),
        gradleUserHome = gradle.gradleUserHomeDir,
        flavour = JetBrainsRuntime.Flavour.Sdk,
    )
}
