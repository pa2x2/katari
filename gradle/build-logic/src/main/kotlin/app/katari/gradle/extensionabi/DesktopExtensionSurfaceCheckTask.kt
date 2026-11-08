package app.katari.gradle.extensionabi

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

/**
 * Fails when the desktop extension host lacks something Android extensions link against. The Android ABI checks pin
 * that surface on the SDK modules' Android jars and list it in their [runtimeSurfaces]; desktop runs the same
 * extensions on the modules' desktop jars and the Android stand-ins, whose `expect`/`actual` declarations, file
 * facades and mangled names can differ without breaking any desktop compile. Every listed class and member must be
 * declared, with the same signature, by a class in [desktopJars].
 */
@CacheableTask
abstract class DesktopExtensionSurfaceCheckTask @Inject constructor(
    private val workerExecutor: WorkerExecutor,
) : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val runtimeSurfaces: ConfigurableFileCollection

    @get:Classpath
    abstract val desktopJars: ConfigurableFileCollection

    /** japicmp's class path, for the javassist that reads [desktopJars] the way the Android checks read theirs. */
    @get:Classpath
    abstract val javassistClasspath: ConfigurableFileCollection

    @get:OutputFile
    abstract val report: RegularFileProperty

    @TaskAction
    fun check() {
        workerExecutor.classLoaderIsolation { classpath.from(javassistClasspath) }
            .submit(DesktopExtensionSurfaceComparison::class.java) {
                runtimeSurfaces.from(this@DesktopExtensionSurfaceCheckTask.runtimeSurfaces)
                desktopJars.from(this@DesktopExtensionSurfaceCheckTask.desktopJars)
                report.set(this@DesktopExtensionSurfaceCheckTask.report)
            }
    }
}
