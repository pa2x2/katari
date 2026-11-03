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
 * Starts the app in a new process the way this one was started, from a packaged launcher or the build tool alike,
 * without this launch's arguments: the links and files it opened are open already, and crash-report mode's is added
 * where it is wanted.
 */
internal object AppRelaunch {
    /** Starts it in crash-report mode for [crashReport], otherwise as a normal start, even from crash-report mode. */
    fun start(crashReport: Path? = null) {
        val command = ProcessHandle.current().info().command().getOrNull()
        if (command == null) {
            logcat(LogPriority.ERROR) { "Can't restart: the system doesn't tell this process's command" }
            return
        }
        val launch = if (Path(command).nameWithoutExtension == JAVA_LAUNCHER) javaLaunch(command) else listOf(command)
        try {
            ProcessBuilder(launch + listOfNotNull(crashReport?.let(CrashReportArgument::of))).inheritIO().start()
        } catch (e: IOException) {
            logcat(LogPriority.ERROR, e) { "Can't restart" }
        }
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
