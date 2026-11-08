package app.katari.gradle.platformparity

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/** Writes [PlatformApis]: what desktop and Android code can name from outside the repository. */
@CacheableTask
abstract class PlatformApiIndexTask : DefaultTask() {

    /** The external libraries of the desktop app, the union of what its modules' desktop compilations see. */
    @get:Classpath
    abstract val desktopLibraries: ConfigurableFileCollection

    /** The JDK running the build, which desktop code compiles against; its classes are read from it. */
    @get:Input
    abstract val desktopJdkVersion: Property<String>

    /** What the Android app's `indexAndroidPlatformApiLibraries` read from `android.jar` and its libraries. */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val androidLibraryNames: ConfigurableFileCollection

    @get:Classpath
    abstract val androidStandIns: ConfigurableFileCollection

    @get:OutputFile
    abstract val index: RegularFileProperty

    @TaskAction
    fun action() {
        PlatformApis(
            desktop = indexClasspath(desktopLibraries) + indexRunningJdk(),
            android = androidLibraryNames.files
                .map { PlatformApiNames.read(it.readLines()) }
                .fold(PlatformApiNames.EMPTY, PlatformApiNames::plus),
            androidStandIns = indexClasspath(androidStandIns),
        ).write(index.get().asFile)
    }
}
