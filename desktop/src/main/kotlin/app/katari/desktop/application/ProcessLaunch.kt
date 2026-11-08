package app.katari.desktop.application

import java.lang.management.ManagementFactory
import kotlin.io.path.Path
import kotlin.io.path.nameWithoutExtension
import kotlin.jvm.optionals.getOrNull

/**
 * The command that started this process, without its arguments: the app's own launcher, or the Java command line the
 * build tool runs. Inside an AppImage it is the launcher in the AppImage's mount, which lasts only while the
 * AppImage's first process lives. Null where the system doesn't tell a process its command.
 */
internal fun thisProcessLaunch(): List<String>? {
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
