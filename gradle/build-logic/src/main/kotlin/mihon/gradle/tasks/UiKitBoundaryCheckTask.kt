package mihon.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class UiKitBoundaryCheckTask : DefaultTask() {

    @get:Internal
    abstract val repositoryRoot: DirectoryProperty

    /** Every finding, one per line; the exception message only shows the first ones. */
    @get:Internal
    abstract val report: RegularFileProperty

    @TaskAction
    fun action() {
        val root = repositoryRoot.get().asFile
        val files = root.walkTopDown()
            .onEnter { directory ->
                directory == root || (directory.name !in SKIPPED_DIRECTORIES && !directory.isHidden)
            }
            .filter(File::isFile)
            .toList()
        val findings = checkUiKitBoundaries(
            kotlinSources = files.filter { it.extension == "kt" }.map { it.toSource(root) },
            buildScripts = files.filter { it.name == "build.gradle.kts" }.map { it.toSource(root) },
        )
        val lines = findings.map { finding ->
            val location = finding.lineNumber?.let { "${finding.relativePath}:$it" } ?: finding.relativePath
            "- $location: ${finding.reason}"
        }
        val reportFile = report.get().asFile
        reportFile.parentFile.mkdirs()
        reportFile.writeText(lines.joinToString(separator = "\n", postfix = "\n"))
        if (findings.isEmpty()) return

        val maxFindings = 80
        val suffix = if (findings.size >
            maxFindings
        ) {
            "\n- ... ${findings.size - maxFindings} more, see $reportFile"
        } else {
            ""
        }
        throw GradleException(
            "UI kit boundary check failed (${findings.size} findings):\n${lines.take(
                maxFindings,
            ).joinToString("\n")}$suffix",
        )
    }

    private fun File.toSource(root: File) = UiKitBoundarySource(relativeTo(root).invariantSeparatorsPath, readText())

    private companion object {
        val SKIPPED_DIRECTORIES = setOf("build", "node_modules", "website")
    }
}
