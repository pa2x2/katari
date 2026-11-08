package app.katari.desktop.crash

import java.nio.file.Path

/**
 * The command-line argument that starts the app in crash-report mode, which shows only the crash screen, for the
 * report in the file the argument names.
 */
internal object CrashReportArgument {
    private const val PREFIX = "--crash-report="

    fun of(report: Path): String = PREFIX + report

    /** The report file [arguments] name, or null for a normal start. */
    fun reportIn(arguments: Array<String>): Path? =
        arguments.firstOrNull { it.startsWith(PREFIX) }?.removePrefix(PREFIX)?.let(Path::of)
}
