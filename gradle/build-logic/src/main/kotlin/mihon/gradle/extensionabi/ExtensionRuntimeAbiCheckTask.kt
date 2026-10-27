package mihon.gradle.extensionabi

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Fails when R8 may have changed what external code links against in a minified app variant.
 *
 * The ABI checks prove the library jars keep their surface; R8 then rewrites whatever no keep rule matches, removing
 * members, turning Kotlin objects into static methods and narrowing return types. It leaves exactly the elements a
 * keep rule matched, and lists those in [seeds], so every line of every [runtimeSurfaces] file must be there.
 */
@CacheableTask
abstract class ExtensionRuntimeAbiCheckTask : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val runtimeSurfaces: ConfigurableFileCollection

    /** Elements no ABI check covers that something outside the app reads by name, as `seeds.txt` lines. */
    @get:Input
    abstract val additionalElements: ListProperty<String>

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val seeds: RegularFileProperty

    @get:OutputFile
    abstract val report: RegularFileProperty

    @TaskAction
    fun check() {
        val kept = seeds.get().asFile.readLines().toHashSet()
        val required = runtimeSurfaces.files.sortedBy { it.name }.flatMap { surface ->
            surface.readLines().filter(String::isNotBlank).map { surface.nameWithoutExtension to it }
        } + additionalElements.get().map { "additional" to it }
        val missing = required.filterNot { (_, element) -> element in kept }
        report.get().asFile.writeText(missing.joinToString("\n") { (surface, element) -> "$surface: $element" })
        check(missing.isEmpty()) {
            val classes = missing.map { (_, element) -> element.substringBefore(':') }.distinct()
            "R8 doesn't keep ${missing.size} elements external extensions link against, in ${classes.size} classes. " +
                "Add keep rules to the module that owns them:\n" + classes.joinToString("\n") +
                "\nFull list: ${report.get().asFile}"
        }
    }
}
