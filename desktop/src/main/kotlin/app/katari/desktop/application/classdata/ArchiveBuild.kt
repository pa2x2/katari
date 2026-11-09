package app.katari.desktop.application.classdata

import app.katari.desktop.application.thisJavaCommand
import kotlinx.coroutines.runInterruptible
import java.nio.file.Path

/**
 * A class data archive building in a process of its own: the JVM the app runs on, with the options the app was started
 * with, so the archive matches the runs that map it, reading [classList] and writing [archive]. The JVM builds an
 * archive only as a process of its own, and one that adds this process's classes would stop all of its threads while
 * it writes, the window's too.
 */
internal class ArchiveBuild private constructor(private val process: Process) {

    /** Whether the archive was built whole. */
    suspend fun await(): Boolean = runInterruptible { process.waitFor() } == 0

    fun stop() {
        process.destroy()
    }

    companion object {
        /** Null where the app wasn't started by a Java command it can rerun. */
        fun start(classList: Path, archive: Path): ArchiveBuild? {
            val java = thisJavaCommand() ?: return null
            // The app's run records its classes and maps the archive there is; the build does neither
            val options = java.drop(1).filterNot { option -> RUN_ONLY_OPTIONS.any(option::startsWith) }
            // At the lowest priority, so the app's own work, and the user's, comes first
            val command = listOf("nice", "-n", "19", java.first()) + options + listOf(
                "-Xshare:dump",
                "-XX:SharedClassListFile=$classList",
                "-XX:SharedArchiveFile=$archive",
                "-cp",
                System.getProperty("java.class.path"),
            )
            val process = ProcessBuilder(command)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .apply {
                    // The options it held are among the app's own, which the command passes already
                    environment().remove("JDK_JAVA_OPTIONS")
                }
                .start()
            return ArchiveBuild(process)
        }

        private val RUN_ONLY_OPTIONS = listOf("-XX:DumpLoadedClassList=", "-XX:SharedArchiveFile=")
    }
}
