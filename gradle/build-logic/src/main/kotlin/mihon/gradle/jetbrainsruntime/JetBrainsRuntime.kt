package mihon.gradle.jetbrainsruntime

import mihon.gradle.extensions.mihonx
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.register
import java.io.File

/**
 * The JetBrains Runtime with JCEF that the desktop app and the tools running its code start on, pinned in
 * `gradle/mihon.versions.toml`: the JVM that carries Chromium for the desktop web view. The build downloads it once per
 * machine into the Gradle user home, where every build and worktree finds it.
 *
 * `-PsystemJdk` starts them on the JDK running Gradle instead, which has no JCEF, to try the app as it runs there.
 */
internal class JetBrainsRuntime(
    val version: String,
    val build: String,
    private val architecture: String,
    val sha512: String,
    gradleUserHome: File,
) {
    val archiveName: String = "$FLAVOUR-$version-linux-$architecture-$build"

    val url: String = "$DOWNLOADS/$archiveName.tar.gz"

    /** The runtime's home, its `java.home`. */
    val home: File = gradleUserHome.resolve("caches/katari/jetbrains-runtime/$archiveName")

    /** The runtime's `release` file, which names the JCEF it carries. */
    val release: File get() = home.resolve("release")

    private companion object {
        /** The runtime with JCEF; the plain one has no Chromium and the SDK only adds the JDK's tools. */
        const val FLAVOUR = "jbr_jcef"

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
    val architecture = when (val arch = System.getProperty("os.arch")) {
        "amd64", "x86_64" -> "x64"
        "aarch64", "arm64" -> "aarch64"
        else -> error("The JetBrains Runtime is pinned for x64 and aarch64 Linux, not $arch; run with -PsystemJdk")
    }
    val runtime = mihonx.versions.jbr
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

/** This project's task that makes [runtime] ready, registered by the first caller. */
internal fun Project.provisionJetBrainsRuntime(runtime: JetBrainsRuntime): TaskProvider<ProvisionJetBrainsRuntimeTask> {
    if (PROVISION_TASK_NAME in tasks.names) {
        return tasks.named(PROVISION_TASK_NAME, ProvisionJetBrainsRuntimeTask::class.java)
    }
    val offline = gradle.startParameter.isOffline
    return tasks.register<ProvisionJetBrainsRuntimeTask>(PROVISION_TASK_NAME) {
        description = "Downloads, verifies and unpacks the JetBrains Runtime the desktop app runs on."
        url.set(runtime.url)
        sha512.set(runtime.sha512)
        home.set(runtime.home)
        this.offline.set(offline)
    }
}

private const val PROVISION_TASK_NAME = "provisionJetBrainsRuntime"
