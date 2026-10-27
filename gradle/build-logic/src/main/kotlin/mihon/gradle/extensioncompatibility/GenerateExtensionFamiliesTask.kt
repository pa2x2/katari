package mihon.gradle.extensioncompatibility

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/** Writes the families in [source] as the app's `ExtensionFamily` enum, which its extension loader runs by. */
@CacheableTask
abstract class GenerateExtensionFamiliesTask : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val source: RegularFileProperty

    @get:Input
    abstract val packageName: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val sourceFile = source.get().asFile
        val families = readExtensionFamilies(sourceFile)
        val entries = families.joinToString("\n") { family ->
            val versions = family.versions.joinToString { "\"$it\"" }
            "    ${family.id.uppercase()}(library = \"${family.library}\", libVersions = listOf($versions)),"
        }
        val code = """
            |// Generated from ${sourceFile.name} by ${GenerateExtensionFamiliesTask::class.simpleName}; edit that file instead.
            |package ${packageName.get()}
            |
            |/**
            | * The extension families this build loads and the versions of each family's [library] it runs, `major.minor`, or
            | * `major` for libraries numbered that way.
            | */
            |enum class ExtensionFamily(val library: String, val libVersions: List<String>) {
            |$entries
            |}
            |
        """.trimMargin()
        val directory = outputDir.get().asFile
        directory.deleteRecursively()
        val file = directory.resolve(packageName.get().replace('.', '/')).resolve("ExtensionFamily.kt")
        file.parentFile.mkdirs()
        file.writeText(code)
    }
}
