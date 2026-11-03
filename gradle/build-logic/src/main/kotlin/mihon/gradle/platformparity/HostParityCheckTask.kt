package mihon.gradle.platformparity

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

abstract class HostParityCheckTask : DefaultTask() {

    @get:Internal
    abstract val repositoryRoot: DirectoryProperty

    @TaskAction
    fun action() {
        val root = repositoryRoot.get().asFile
        val sources = root.walkTopDown()
            .onEnter { directory ->
                directory == root || (directory.name !in SKIPPED_DIRECTORIES && !directory.isHidden)
            }
            .filter { it.isFile && it.extension == "kt" }
            .map { HostParitySource(it.relativeTo(root).invariantSeparatorsPath, it.readText()) }
            .toList()
        val findings = checkHostParity(sources)
        if (findings.isEmpty()) return

        throw GradleException(
            "Host parity check failed (${findings.size} findings):\n" +
                findings.joinToString("\n") { "- ${it.relativePath}:${it.lineNumber}: ${it.reason}" },
        )
    }

    private companion object {
        val SKIPPED_DIRECTORIES = setOf("build", "node_modules", "website")
    }
}
