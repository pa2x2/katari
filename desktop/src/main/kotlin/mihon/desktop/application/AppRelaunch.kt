package mihon.desktop.application

import logcat.LogPriority
import mihon.desktop.crash.CrashReportArgument
import tachiyomi.core.common.util.system.logcat
import java.io.IOException
import java.lang.management.ManagementFactory
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.nameWithoutExtension
import kotlin.jvm.optionals.getOrNull

/**
 * Starts the app in a new process the way this one was started, from its AppImage or the build tool alike,
 * without this launch's arguments: the links and files it opened are open already, and crash-report mode's is added
 * where it is wanted.
 */
internal object AppRelaunch {
    /** Starts it in crash-report mode for [crashReport], otherwise as a normal start, even from crash-report mode. */
    fun start(crashReport: Path? = null) {
        val launch = appImageLaunch() ?: processLaunch() ?: run {
            logcat(LogPriority.ERROR) { "Can't restart: the system doesn't tell this process's command" }
            return
        }
        try {
            ProcessBuilder(launch + listOfNotNull(crashReport?.let(CrashReportArgument::of))).inheritIO().start()
        } catch (e: IOException) {
            logcat(LogPriority.ERROR, e) { "Can't restart" }
        }
    }

    /**
     * The AppImage this process runs from, which its runtime names in `APPIMAGE`. The Java command inside it won't do:
     * its files are mounted only while the AppImage's first process lives, which ends as this one does.
     */
    private fun appImageLaunch(): List<String>? = System.getenv("APPIMAGE")?.takeIf { it.isNotEmpty() }?.let(::listOf)

    private fun processLaunch(): List<String>? {
        val command = ProcessHandle.current().info().command().getOrNull() ?: return null
        return if (Path(command).nameWithoutExtension == JAVA_LAUNCHER) javaLaunch(command) else listOf(command)
    }

    /**
     * The Java command line that started this process, rebuilt from what the JVM records of its start: on Linux, Java
     * reads no arguments of a command line as long as the app's class path makes it.
     */
    private fun javaLaunch(java: String): List<String> {
        // The main class or jar comes first, before the arguments, which are dropped
        val started = System.getProperty("sun.java.command").orEmpty().substringBefore(' ')
        val target = if (started.endsWith(".jar")) {
            listOf("-jar", started)
        } else {
            listOf("-cp", System.getProperty("java.class.path"), started)
        }
        return listOf(java) + ManagementFactory.getRuntimeMXBean().inputArguments + target
    }

    private const val JAVA_LAUNCHER = "java"
}
