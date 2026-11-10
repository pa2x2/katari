package app.katari.gradle.appimage

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import javax.inject.Inject

/**
 * Lays out the desktop app as an AppDir, the folder appimagetool packs into an AppImage:
 *
 * - `runtime/`, the runtime [LinkAppImageRuntimeTask] links;
 * - `lib/`, the app's jars as [copyJarForAppImage] writes them, under names that tell apart jars which share a file
 *   name;
 * - `AppRun`, which the AppImage starts, running [mainClass] with the jars on the class path in their order, with the
 *   class data the app keeps for this build in its cache folder, and showing [splash] meanwhile (see [appRun]);
 * - `splash.png`, that image;
 * - the desktop entry and icon, named after [applicationId], which the desktop shows for an integrated AppImage.
 */
abstract class AssembleAppDirTask : DefaultTask() {

    /** In class path order. */
    @get:Classpath
    abstract val jars: ConfigurableFileCollection

    /** The name in `lib/` of each of [jars], in the same order. */
    @get:Input
    abstract val jarNames: ListProperty<String>

    /** What is left out of the jars that hold files the app never reads, by their names in `lib/`. */
    @get:Input
    abstract val unusedFiles: MapProperty<String, UnusedLibraryFiles>

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val runtimeHome: DirectoryProperty

    @get:Input
    abstract val mainClass: Property<String>

    @get:Input
    abstract val jvmArguments: ListProperty<String>

    @get:Input
    abstract val applicationId: Property<String>

    @get:Input
    abstract val displayName: Property<String>

    /** The name of the app's own folder in each XDG base directory. */
    @get:Input
    abstract val directoryName: Property<String>

    /** The link schemes the app registers to open, such as `katari`; none for a build that leaves links to another. */
    @get:Input
    abstract val linkSchemes: ListProperty<String>

    /** An SVG. */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val icon: RegularFileProperty

    /** A PNG, shown at its size in pixels in the middle of the screen from the launch until the app's window opens. */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val splash: RegularFileProperty

    @get:OutputDirectory
    abstract val appDir: DirectoryProperty

    @get:Inject
    abstract val execOperations: ExecOperations

    @TaskAction
    fun assemble() {
        val appDir = appDir.get().asFile
        appDir.deleteRecursively()
        appDir.mkdirs()
        // The runtime's symbolic links are kept as they are, which Gradle's copying would follow
        execOperations.exec {
            commandLine("cp", "-a", runtimeHome.get().asFile.absolutePath, appDir.resolve(RUNTIME).absolutePath)
        }

        val jarFiles = jars.files.toList()
        val names = jarNames.get()
        check(jarFiles.size == names.size) { "${jarFiles.size} jars but ${names.size} names for them" }
        val lib = appDir.resolve(LIB).also { it.mkdirs() }
        val unusedFiles = unusedFiles.get()
        jarFiles.zip(names).forEach { (jar, name) -> copyJarForAppImage(jar, lib.resolve(name), unusedFiles[name]) }

        splash.get().asFile.copyTo(appDir.resolve(SPLASH))
        appDir.resolve("AppRun").apply {
            writeText(appRun(names, classDataFingerprint(appDir, names)))
            setExecutable(true, false)
        }
        appDir.resolve("${applicationId.get()}.desktop").writeText(desktopEntry())
        val icon = icon.get().asFile.copyTo(appDir.resolve("${applicationId.get()}.svg"))
        Files.createSymbolicLink(appDir.resolve(".DirIcon").toPath(), File(icon.name).toPath())
    }

    /**
     * The script that starts the app. Besides the app's own options, the JVM gets the class data the app keeps for this
     * build in its cache folder, laid out as the app's `ClassDataArchive` reads it: the JVM records the classes of each
     * run in `sessions/`, named after its process, and maps `archive.jsa`, once the app has built it from them, instead
     * of loading those classes from the jars again. Without a cache folder it can write in, the app runs without.
     *
     * The splash is the Java launcher's own, which shows it before it starts the JVM and takes it down when the app's
     * first window opens or the process ends. The launcher keeps the option to itself, so the Java commands the app
     * rebuilds from its own JVM's options, such as the class data archive's build, show none. Nor does a launch the
     * running app will answer, which ends before any window opens: the app keeps its `LinuxSingleInstance` socket in
     * its runtime folder while it answers launches. A killed app leaves the socket behind, which costs the next launch
     * its splash and nothing else.
     */
    private fun appRun(names: List<String>, fingerprint: String): String {
        val classPath = names.joinToString(":") { "\$dir/$LIB/$it" }
        val command = listOf("\"\$dir/$RUNTIME/bin/java\"", "\${splash:+\"-splash:\$splash\"}") +
            jvmArguments.get().map(::shellQuoted) +
            listOf(
                // Each of these three is left out where the variable it reads is empty
                "\${archive:+\"-XX:SharedArchiveFile=\$archive\"}",
                "\${classData:+\"-XX:DumpLoadedClassList=\$classData/sessions/%p.classlist\"}",
                "\${classData:+\"-D$CLASS_DATA_PROPERTY=\$classData\"}",
                "-cp",
                "\"$classPath\"",
                shellQuoted(mainClass.get()),
                "\"\$@\"",
            )
        return """
            |#!/bin/sh
            |# Generated by ${AssembleAppDirTask::class.simpleName}. The AppImage runtime sets APPDIR to where it mounted the
            |# AppImage; an AppDir started by hand finds itself beside this script.
            |dir="${'$'}{APPDIR:-${'$'}(dirname "${'$'}(readlink -f "${'$'}0")")}"
            |# The XDG cache directory, where an empty or relative XDG_CACHE_HOME counts as unset
            |case "${'$'}XDG_CACHE_HOME" in /*) cache="${'$'}XDG_CACHE_HOME" ;; *) cache="${'$'}HOME/.cache" ;; esac
            |classData="${'$'}cache/${directoryName.get()}/class-data/$fingerprint"
            |mkdir -p "${'$'}classData/sessions" 2>/dev/null || classData=
            |archive="${'$'}{classData:+${'$'}classData/archive.jsa}"
            |[ -f "${'$'}archive" ] || archive=
            |# No splash on a launch the running app answers, which opens no window: its socket is there while it runs
            |splash="${'$'}dir/$SPLASH"
            |case "${'$'}XDG_RUNTIME_DIR" in
            |    /*) [ -S "${'$'}XDG_RUNTIME_DIR/${directoryName.get()}/$INSTANCE_SOCKET" ] && splash= ;;
            |esac
            |exec ${command.joinToString(" \\\n|    ")}
            |
        """.trimMargin()
    }

    /**
     * Tells this build's class data apart from another build's, which the JVM would refuse: an archive holds the
     * classes of the jars it was built from, in the runtime it was built by, with the options it was built with.
     */
    private fun classDataFingerprint(appDir: File, names: List<String>): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(jvmArguments.get().joinToString("\u0000").toByteArray())
        (listOf(appDir.resolve("$RUNTIME/release")) + names.map { appDir.resolve("$LIB/$it") }).forEach { file ->
            file.forEachBlock { buffer, read -> digest.update(buffer, 0, read) }
        }
        return digest.digest().take(FINGERPRINT_BYTES).joinToString("") { "%02x".format(it) }
    }

    private fun desktopEntry(): String = buildString {
        appendLine("[Desktop Entry]")
        appendLine("Type=Application")
        appendLine("Name=${displayName.get()}")
        appendLine("Exec=AppRun %U")
        appendLine("Icon=${applicationId.get()}")
        // The app names its windows after its application ID, which ties them to this entry when integration renames it
        appendLine("StartupWMClass=${applicationId.get()}")
        appendLine("Categories=Graphics;Viewer;")
        appendLine("Terminal=false")
        val schemes = linkSchemes.get()
        if (schemes.isNotEmpty()) appendLine("MimeType=${schemes.joinToString("") { "x-scheme-handler/$it;" }}")
    }

    private fun shellQuoted(value: String) = "'${value.replace("'", "'\\''")}'"

    private companion object {
        const val RUNTIME = "runtime"
        const val LIB = "lib"
        const val SPLASH = "splash.png"

        // The file name of the socket the app's LinuxSingleInstance answers later launches on
        const val INSTANCE_SOCKET = "instance.socket"
        const val FINGERPRINT_BYTES = 8

        // The system property the app's ClassDataArchive reads the build's class data folder from
        const val CLASS_DATA_PROPERTY = "katari.classData"
    }
}
