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
    // The main class or jar comes last, after the JVM's options; the arguments after it are dropped
    return thisJavaCommand()?.plus(javaTarget()) ?: listOf(command)
}

/**
 * The Java command that started this process with the JVM options it was given, up to the class it runs, rebuilt from
 * what the JVM records of its start: on Linux, Java reads no arguments of a command line as long as the app's class
 * path makes it. Null where a launcher of the app's own started it, or the system doesn't tell a process its command.
 */
internal fun thisJavaCommand(): List<String>? {
    val command = ProcessHandle.current().info().command().getOrNull() ?: return null
    if (Path(command).nameWithoutExtension != JAVA_LAUNCHER) return null
    return listOf(command) + ManagementFactory.getRuntimeMXBean().inputArguments
}

private fun javaTarget(): List<String> {
    val started = System.getProperty("sun.java.command").orEmpty().substringBefore(' ')
    return if (started.endsWith(".jar")) {
        listOf("-jar", started)
    } else {
        listOf("-cp", System.getProperty("java.class.path"), started)
    }
}

private const val JAVA_LAUNCHER = "java"
