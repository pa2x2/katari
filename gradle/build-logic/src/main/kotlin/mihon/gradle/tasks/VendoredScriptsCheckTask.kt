package mihon.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.security.MessageDigest

/**
 * Fails when a committed script bundle doesn't match the `SHA256SUMS` its npm build wrote. The bundles are built by
 * hand from a pinned lockfile (see each bundle directory's README) and committed, so this catches a bundle edited or
 * rebuilt without recording it.
 */
abstract class VendoredScriptsCheckTask : DefaultTask() {

    /** `sha256sum`-format files, with paths relative to the file's own directory. */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val manifests: ConfigurableFileCollection

    @TaskAction
    fun check() {
        val problems = manifests.files.flatMap(::problemsIn)
        if (problems.isNotEmpty()) {
            throw GradleException(
                "Committed script bundles don't match their SHA256SUMS; rebuild them as their README says:\n" +
                    problems.joinToString("\n") { "  $it" },
            )
        }
    }

    companion object {
        fun problemsIn(manifest: File): List<String> = manifest.readLines()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val expected = line.substringBefore(' ')
                val file = manifest.parentFile.resolve(line.substringAfter(' ').trimStart(' ', '*')).normalize()
                when {
                    !file.isFile -> "${file.path}: missing"
                    sha256(file) != expected -> "${file.path}: SHA-256 differs from ${manifest.path}"
                    else -> null
                }
            }

        private fun sha256(file: File): String =
            MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString("") { "%02x".format(it) }
    }
}
