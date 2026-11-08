package app.katari.gradle.platformparity

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class PlatformFacilityCheckTask : DefaultTask() {

    @get:Internal
    abstract val repositoryRoot: DirectoryProperty

    /** What `indexPlatformApis` wrote, which tells a platform-bound multibinding element from a neutral one. */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val platformApis: RegularFileProperty

    @TaskAction
    fun action() {
        val root = repositoryRoot.get().asFile
        val findings = checkPlatformFacilities(
            root.platformSources(),
            root.platformBuildFiles(),
            PlatformApis.read(platformApis.get().asFile),
        )
        if (findings.isEmpty()) return

        throw GradleException(
            "Platform facility check failed (${findings.size} findings):\n" +
                findings.joinToString("\n") { "- ${it.relativePath}:${it.lineNumber}: ${it.reason}" },
        )
    }
}

internal data class PlatformFacilityFinding(val relativePath: String, val lineNumber: Int, val reason: String)
