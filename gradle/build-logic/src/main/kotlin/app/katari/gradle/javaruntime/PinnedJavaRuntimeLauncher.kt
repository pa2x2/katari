package app.katari.gradle.javaruntime

import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.jvm.toolchain.JavaInstallationMetadata
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaLauncher

/**
 * A pinned runtime as a task's launcher, described from its pin rather than probed. Gradle probes a launcher it derives
 * from a task's executable while it stores the configuration cache, before any task has run, so a runtime the build
 * downloads later would fail the build; a launcher the task is handed is only read as it runs.
 */
internal class PinnedJavaRuntimeLauncher(
    private val home: Directory,
    private val runtime: PinnedJavaRuntime,
) : JavaLauncher {
    override fun getExecutablePath(): RegularFile = home.file("bin/java")

    override fun getMetadata(): JavaInstallationMetadata = object : JavaInstallationMetadata {
        // A release's first version has no dots ("25+36"), so the feature version is the leading digits.
        override fun getLanguageVersion(): JavaLanguageVersion =
            JavaLanguageVersion.of(runtime.version.takeWhile { it.isDigit() }.toInt())

        override fun getJavaRuntimeVersion(): String = runtime.version

        override fun getJvmVersion(): String = runtime.version

        override fun getVendor(): String = runtime.vendor

        override fun getInstallationPath(): Directory = home

        override fun isCurrentJvm(): Boolean = false
    }
}

internal fun Project.pinnedJavaRuntimeLauncher(runtime: PinnedJavaRuntime): JavaLauncher =
    PinnedJavaRuntimeLauncher(layout.projectDirectory.dir(runtime.home.absolutePath), runtime)
