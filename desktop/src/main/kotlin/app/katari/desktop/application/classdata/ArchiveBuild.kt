package app.katari.desktop.application.classdata

import app.katari.desktop.application.thisJavaCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import java.io.IOException
import java.nio.file.Path

/**
 * Builds class data archives in a process of their own: the JVM the app runs on, with the options the app was started
 * with, so the archive matches the runs that map it. The JVM builds an archive only as a process of its own, and one
 * that adds this process's classes would stop all of its threads while it writes, the window's too.
 *
 * A build ends with the app: it runs from the AppImage, whose mount ends with the app, and would crash once it read
 * from it. Where the system's setpriv can, the kernel kills the build when the thread that started it ends, which waits
 * for the build and so ends before it only with the app, however the app ends; elsewhere a shutdown hook kills it as
 * the app's JVM exits in order. It is killed rather than asked to end, as the JVM leaves a request to end until it has
 * written the archive.
 */
internal object ArchiveBuild {

    /**
     * Builds [archive] from [classList] and tells whether it was built whole, or null where the app wasn't started by
     * a Java command it can rerun. Cancelling it kills the build.
     */
    suspend fun run(classList: Path, archive: Path): Boolean? = runInterruptible(Dispatchers.IO) {
        val command = command(classList, archive) ?: return@runInterruptible null
        val process = ProcessBuilder(command)
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .apply {
                // The options it held are among the app's own, which the command passes already
                environment().remove("JDK_JAVA_OPTIONS")
            }
            .start()
        val stopOnExit = Thread({ process.destroyForcibly() }, "Class data archive build stop")
        try {
            Runtime.getRuntime().addShutdownHook(stopOnExit)
        } catch (_: IllegalStateException) {
            // The app began exiting as the build started
            process.destroyForcibly()
            return@runInterruptible false
        }
        try {
            process.waitFor() == 0
        } finally {
            process.destroyForcibly()
            try {
                Runtime.getRuntime().removeShutdownHook(stopOnExit)
            } catch (_: IllegalStateException) {
                // The app is exiting, past removing hooks; the build is killed already
            }
        }
    }

    private fun command(classList: Path, archive: Path): List<String>? {
        val java = thisJavaCommand() ?: return null
        // The app's run records its classes and maps the archive there is; the build does neither
        val options = java.drop(1).filterNot { option -> RUN_ONLY_OPTIONS.any(option::startsWith) }
        // At the lowest priority, so the app's own work, and the user's, comes first
        return endsWithStarter + listOf("nice", "-n", "19", java.first()) + options + listOf(
            "-Xshare:dump",
            "-XX:SharedClassListFile=$classList",
            "-XX:SharedArchiveFile=$archive",
            "-cp",
            System.getProperty("java.class.path"),
        )
    }

    // setpriv comes with util-linux, which has had the option since 2.33; a system without either gets none
    private val endsWithStarter: List<String> by lazy {
        val setpriv = listOf("setpriv", "--pdeathsig", "KILL")
        val works = try {
            ProcessBuilder(setpriv + "true")
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start()
                .waitFor() == 0
        } catch (_: IOException) {
            false
        }
        if (works) setpriv else emptyList()
    }

    private val RUN_ONLY_OPTIONS = listOf("-XX:DumpLoadedClassList=", "-XX:SharedArchiveFile=")
}
