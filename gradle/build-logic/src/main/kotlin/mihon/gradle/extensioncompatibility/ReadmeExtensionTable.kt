package mihon.gradle.extensioncompatibility

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import org.gradle.work.DisableCachingByDefault
import java.io.File

/**
 * README.md's table of extension families, kept between two markers. GitHub shows the file as it is, so the table is
 * written into it: [UpdateReadmeExtensionTableTask] runs with spotlessApply and [CheckReadmeExtensionTableTask] with
 * spotlessCheck.
 */
internal object ReadmeExtensionTable {

    private const val START = "<!-- extension-compatibility:start -->"
    private const val END = "<!-- extension-compatibility:end -->"

    fun update(readme: String, families: List<ExtensionFamily>, sourceName: String): String {
        val start = readme.indexOf(START)
        val end = readme.indexOf(END)
        if (start < 0 || end < start) throw GradleException("README.md has no $START … $END block")
        return readme.substring(0, start) + render(families, sourceName) + readme.substring(end + END.length)
    }

    private fun render(families: List<ExtensionFamily>, sourceName: String): String = buildString {
        appendLine(START)
        appendLine("<!-- Generated from app/$sourceName by ./gradlew spotlessApply; edit that file instead. -->")
        appendLine()
        appendLine("| Extensions | Built with | Versions |")
        appendLine("| --- | --- | --- |")
        families.forEach { appendLine("| ${it.name} | ${it.library} | ${it.versions.joinToString()} |") }
        appendLine()
        append(END)
    }
}

@UntrackedTask(because = "It rewrites README.md in place")
abstract class UpdateReadmeExtensionTableTask : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val source: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val readme: RegularFileProperty

    @TaskAction
    fun update() {
        val readmeFile = readme.get().asFile
        val updated = expectedReadme(source.get().asFile, readmeFile)
        if (updated != readmeFile.readText()) readmeFile.writeText(updated)
    }
}

@DisableCachingByDefault(because = "Comparing two small files is cheaper than caching")
abstract class CheckReadmeExtensionTableTask : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val source: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val readme: RegularFileProperty

    @TaskAction
    fun check() {
        val readmeFile = readme.get().asFile
        if (expectedReadme(source.get().asFile, readmeFile) != readmeFile.readText()) {
            throw GradleException("README.md's extension table is out of date. Run ./gradlew spotlessApply")
        }
    }
}

private fun expectedReadme(source: File, readme: File): String =
    ReadmeExtensionTable.update(readme.readText(), readExtensionFamilies(source), source.name)
