package app.katari.desktop.autostart

import java.nio.file.Path

/**
 * The desktop entry that starts the app at login, by XDG Autostart, which GNOME, KDE Plasma, Xfce, Cinnamon, MATE,
 * LXQt, Budgie and sessions systemd starts read from the user's autostart folder. It runs [program] with
 * [START_IN_TRAY_ARGUMENT], and marks itself as the app's own, so the app only ever changes or removes an entry it wrote.
 * `TryExec` has the desktop skip the entry while [program] is gone, as when the AppImage moved, until the app runs from
 * its new place and writes the entry again.
 */
internal object AutostartEntry {
    const val START_IN_TRAY_ARGUMENT = "--start-in-tray"

    // The app's mark on the entries it writes, which desktops ignore as an extension key
    private const val OWN_KEY = "X-Katari-Autostart"

    fun text(program: Path, name: String, icon: String): String = buildString {
        appendLine("[Desktop Entry]")
        appendLine("Type=Application")
        appendLine("Name=${escaped(name)}")
        appendLine("Exec=${escaped(execArgument(program.toString()))} $START_IN_TRAY_ARGUMENT")
        appendLine("TryExec=${escaped(program.toString())}")
        appendLine("Icon=${escaped(icon)}")
        appendLine("Terminal=false")
        appendLine("X-GNOME-Autostart-enabled=true")
        appendLine("$OWN_KEY=true")
    }

    /**
     * Whether [text] is an entry the app wrote that the desktop starts: one the desktop's settings turned off, by
     * `Hidden` as KDE's do or by `X-GNOME-Autostart-enabled` as GNOME's did, doesn't count.
     */
    fun isOwnAndEnabled(text: String): Boolean {
        val keys = keysOf(text)
        return keys[OWN_KEY] == "true" && keys["Hidden"] != "true" && keys["X-GNOME-Autostart-enabled"] != "false"
    }

    /** Whether [text] is an entry the app wrote, enabled or not, which the app may remove. */
    fun isOwn(text: String): Boolean = keysOf(text)[OWN_KEY] == "true"

    /** The keys of the `[Desktop Entry]` group, as written, which the ones the app reads need no unescaping for. */
    private fun keysOf(text: String): Map<String, String> {
        var inEntry = false
        return buildMap {
            text.lineSequence().map(String::trim).forEach { line ->
                when {
                    line.startsWith("[") -> inEntry = line == "[Desktop Entry]"
                    inEntry && "=" in line && !line.startsWith("#") ->
                        put(line.substringBefore('=').trim(), line.substringAfter('=').trim())
                }
            }
        }
    }

    /**
     * [argument] quoted as the Exec key's quoting rules ask: in double quotes, with a double quote, backtick, dollar
     * sign and backslash escaped by a backslash, and a percent sign doubled, as it would start a field code.
     */
    private fun execArgument(argument: String): String = buildString {
        append('"')
        argument.forEach { character ->
            when (character) {
                '"', '`', '$', '\\' -> append('\\').append(character)
                '%' -> append("%%")
                else -> append(character)
            }
        }
        append('"')
    }

    /** [value] as a string value of a desktop entry, whose backslashes and control characters are escaped. */
    private fun escaped(value: String): String = buildString {
        value.forEach { character ->
            when (character) {
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\t' -> append("\\t")
                '\r' -> append("\\r")
                else -> append(character)
            }
        }
    }
}
