package mihon.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.util.zip.ZipFile

/**
 * Takes the Java glue out of an AAR whose native libraries ship separately: its `classes.jar` and its `assets/`.
 * Everything else, above all `jni/`, stays behind, so AGP never sees the libraries and can't package them.
 */
@CacheableTask
abstract class UnpackAarGlueTask : DefaultTask() {

    /** Exactly one AAR. */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val aar: ConfigurableFileCollection

    @get:OutputFile
    abstract val classesJar: RegularFileProperty

    @get:OutputDirectory
    abstract val assetsDir: DirectoryProperty

    @TaskAction
    fun unpack() {
        val archive = aar.singleFile
        val assets = assetsDir.get().asFile
        assets.deleteRecursively()
        assets.mkdirs()
        ZipFile(archive).use { zip ->
            val classes = zip.getEntry(CLASSES_JAR) ?: error("${archive.name} has no $CLASSES_JAR")
            zip.getInputStream(classes).use { input ->
                classesJar.get().asFile.outputStream().use(input::copyTo)
            }
            zip.entries().asSequence()
                .filter { !it.isDirectory && it.name.startsWith(ASSETS_PREFIX) }
                .forEach { entry ->
                    val target = assets.resolve(entry.name.removePrefix(ASSETS_PREFIX))
                    check(target.canonicalPath.startsWith(assets.canonicalPath + java.io.File.separator)) {
                        "${archive.name} has an asset outside assets/: ${entry.name}"
                    }
                    target.parentFile.mkdirs()
                    zip.getInputStream(entry).use { input -> target.outputStream().use(input::copyTo) }
                }
        }
    }

    private companion object {
        const val CLASSES_JAR = "classes.jar"
        const val ASSETS_PREFIX = "assets/"
    }
}
