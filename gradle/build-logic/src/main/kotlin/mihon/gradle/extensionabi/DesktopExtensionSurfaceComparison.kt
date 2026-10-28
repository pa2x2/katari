package mihon.gradle.extensionabi

import javassist.ClassPool
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import java.io.ByteArrayInputStream
import java.io.File
import java.util.zip.ZipFile

/** Lists the surface [DesktopExtensionSurfaceCheckTask] requires that its desktop jars don't declare. */
abstract class DesktopExtensionSurfaceComparison : WorkAction<DesktopExtensionSurfaceComparison.Parameters> {

    interface Parameters : WorkParameters {
        val runtimeSurfaces: ConfigurableFileCollection
        val desktopJars: ConfigurableFileCollection
        val report: RegularFileProperty
    }

    override fun execute() {
        // Parsed from bytes read here: through javassist's own jar class path, the worker, which Gradle reuses between
        // builds, read a jar rebuilt since the last run through a stale zip directory ("invalid LOC header").
        val pool = ClassPool(false)
        val declared = parameters.desktopJars.files.sortedBy { it.name }
            .flatMap { jar -> classesIn(jar).map(pool::makeClass) }
            .flatMap(::declaredSurfaceOf)
            .toHashSet()
        val required = parameters.runtimeSurfaces.files.sortedBy { it.name }.flatMap { surface ->
            surface.readLines().filter(String::isNotBlank).map { surface.nameWithoutExtension to it }
        }
        val missing = required.filterNot { (_, element) -> element in declared }
        val report = parameters.report.get().asFile
        report.writeText(missing.joinToString("\n") { (surface, element) -> "$surface: $element" })
        check(missing.isEmpty()) {
            val elements = missing.map { it.second }.distinct()
            "The desktop extension host lacks ${elements.size} elements Android extensions link against:\n" +
                elements.joinToString("\n") + "\nBy check: $report"
        }
    }

    private fun classesIn(jar: File): List<ByteArrayInputStream> = ZipFile(jar).use { zip ->
        zip.entries().asSequence()
            .filter { it.name.endsWith(".class") && !it.name.startsWith("META-INF/") }
            .filterNot { it.name.endsWith("module-info.class") }
            .map { entry -> ByteArrayInputStream(zip.getInputStream(entry).use { it.readBytes() }) }
            .toList()
    }
}
