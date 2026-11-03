package mihon.gradle.jetbrainsruntime

import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.jvm.toolchain.JavaInstallationMetadata
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaLauncher

/**
 * The pinned JetBrains Runtime as a `JavaExec` task's launcher, described from its pin rather than probed. Gradle probes
 * a launcher it derives from a task's executable while it stores the configuration cache, before any task has run, so
 * a runtime the build downloads later would fail the build; a launcher the task is handed is only read as it runs.
 */
internal class JetBrainsRuntimeLauncher(
    private val home: Directory,
    private val version: String,
    private val build: String,
) : JavaLauncher {
    override fun getExecutablePath(): RegularFile = home.file("bin/java")

    override fun getMetadata(): JavaInstallationMetadata = object : JavaInstallationMetadata {
        override fun getLanguageVersion(): JavaLanguageVersion =
            JavaLanguageVersion.of(version.substringBefore('.').toInt())

        override fun getJavaRuntimeVersion(): String = "$version+$build"

        override fun getJvmVersion(): String = "$version+$build"

        override fun getVendor(): String = VENDOR

        override fun getInstallationPath(): Directory = home

        override fun isCurrentJvm(): Boolean = false
    }

    private companion object {
        const val VENDOR = "JetBrains s.r.o."
    }
}
