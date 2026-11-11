package app.katari.gradle.javaruntime

import java.io.File

/**
 * A Java runtime pinned in `gradle/katari.versions.toml` that tasks start on. The build downloads it once per machine
 * into the Gradle user home, where every build and worktree finds it.
 */
internal class PinnedJavaRuntime(
    /** Names the runtime in the build's messages, in the middle of a sentence. */
    val name: String,
    val url: String,
    /** The archive's sum in [digestAlgorithm], as its publisher gives it beside the archive. */
    val digest: String,
    val digestAlgorithm: String,
    /** The runtime's home, its `java.home`. */
    val home: File,
    /** The runtime's version with its build, as `java -version` prints it. */
    val version: String,
    val vendor: String,
    /** The name of the task that makes this runtime ready in a project. */
    val provisionTaskName: String,
    /** What a build can do instead when it runs offline before the runtime is downloaded, besides going online. */
    val offlineAlternative: String? = null,
)

/** This machine's architecture as the pinned runtimes' archives name it, or null for one none is pinned for. */
internal fun pinnedRuntimeArchitecture(): String? = when (System.getProperty("os.arch")) {
    "amd64", "x86_64" -> "x64"
    "aarch64", "arm64" -> "aarch64"
    else -> null
}
