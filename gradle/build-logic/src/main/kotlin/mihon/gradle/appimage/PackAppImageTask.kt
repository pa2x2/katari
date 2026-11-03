package mihon.gradle.appimage

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import javax.inject.Inject

/** Packs [appDir] into the AppImage [appImage] with the pinned appimagetool and AppImage runtime. */
abstract class PackAppImageTask : DefaultTask() {

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val appDir: DirectoryProperty

    /** The pins of [appimagetool] and [runtime], which stand for the files in the Gradle user home. */
    @get:Input
    abstract val toolPins: ListProperty<String>

    @get:Internal
    abstract val appimagetool: RegularFileProperty

    @get:Internal
    abstract val runtime: RegularFileProperty

    @get:OutputFile
    abstract val appImage: RegularFileProperty

    @get:Inject
    abstract val execOperations: ExecOperations

    @TaskAction
    fun pack() {
        val appImage = appImage.get().asFile
        appImage.delete()
        execOperations.exec {
            // appimagetool is an AppImage itself; extracted, it needs no FUSE, which containers such as CI lack
            environment("APPIMAGE_EXTRACT_AND_RUN", "1")
            environment("ARCH", ARCHITECTURE)
            commandLine(
                appimagetool.get().asFile.absolutePath,
                // The app ships no AppStream metadata, which appimagetool would otherwise look for and warn about
                "--no-appstream",
                "--runtime-file",
                runtime.get().asFile.absolutePath,
                // zstd's level 19 and blocks twice the default size, which with the jars stored uncompressed made the
                // AppImage a tenth smaller than the defaults and started the app 0.2 s slower. Larger blocks shrank it
                // further but slowed the start more, each small class read unpacking a whole block.
                "--mksquashfs-opt", "-b",
                "--mksquashfs-opt", "256K",
                "--mksquashfs-opt", "-Xcompression-level",
                "--mksquashfs-opt", "19",
                appDir.get().asFile.absolutePath,
                appImage.absolutePath,
            )
        }
    }
}
