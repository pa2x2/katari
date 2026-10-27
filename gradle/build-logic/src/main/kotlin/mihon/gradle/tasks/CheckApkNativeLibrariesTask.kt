package mihon.gradle.tasks

import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.util.zip.ZipFile

/**
 * Fails when an APK packages a native library named like one of libmpv's. Those libraries come from the separately
 * installed mpv add-on and share Katari's linker namespace, so a copy in Katari's own APK, whether the libmpv AAR's
 * own or another dependency's library with the same soname, would be the one the linker binds.
 *
 * The names are read from the APK's own `assets/libmpv/load-order.json`, which comes with the AAR's glue.
 */
@CacheableTask
abstract class CheckApkNativeLibrariesTask : DefaultTask() {

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val apkDirectory: DirectoryProperty

    /** Written when the check passes, so the task is up to date until the APKs change. */
    @get:OutputFile
    abstract val report: RegularFileProperty

    @TaskAction
    fun check() {
        val apks = apkDirectory.get().asFile.walkTopDown().filter { it.isFile && it.extension == "apk" }.toList()
        val lines = apks.map { apk ->
            ZipFile(apk).use { zip ->
                val loadOrder = zip.getEntry(LOAD_ORDER) ?: throw GradleException("${apk.name} has no $LOAD_ORDER")
                val libmpvNames = zip.getInputStream(loadOrder).use(::libraryNames)
                val clashes = zip.entries().asSequence()
                    .map { it.name }
                    .filter { it.startsWith("lib/") && it.substringAfterLast('/') in libmpvNames }
                    .toList()
                if (clashes.isNotEmpty()) {
                    throw GradleException(
                        "${apk.name} packages native libraries that libmpv's add-on provides: " +
                            "${clashes.joinToString()}. Keep the libmpv AAR's jni/ out of the APK, and don't add " +
                            "a dependency whose libraries share these sonames.",
                    )
                }
                "${apk.name}: none of ${libmpvNames.size} libmpv libraries packaged"
            }
        }
        report.get().asFile.writeText(lines.joinToString("\n", postfix = "\n"))
    }

    private fun libraryNames(input: java.io.InputStream): Set<String> {
        val json = JsonSlurper().parse(input) as Map<*, *>
        val loadOrder = json["loadOrder"] as? Map<*, *> ?: throw GradleException("$LOAD_ORDER has no loadOrder")
        return loadOrder.values.flatMap { it as List<*> }.map { it.toString() }.toSet()
    }

    private companion object {
        const val LOAD_ORDER = "assets/libmpv/load-order.json"
    }
}
