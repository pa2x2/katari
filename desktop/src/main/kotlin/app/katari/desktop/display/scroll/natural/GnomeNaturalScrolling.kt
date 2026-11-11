package app.katari.desktop.display.scroll.natural

import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * GNOME's compositor, Mutter, which takes the touchpads' settings from GSettings, read with GLib's `gsettings`, which
 * GNOME ships: GSettings has no D-Bus service of its own to ask.
 */
internal object GnomeNaturalScrolling {
    /** A name on the session bus that only Mutter owns. */
    const val SERVICE = "org.gnome.Mutter.DisplayConfig"

    /** Whether the touchpads scroll naturally; null when `gsettings` doesn't answer. Throws [IOException] without it. */
    fun read(): Boolean? {
        val process = ProcessBuilder("gsettings", "get", SCHEMA, KEY)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start()
        val answer = process.inputStream.bufferedReader().use { it.readText() }.trim()
        if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) process.destroy()
        return answer.toBooleanStrictOrNull()
    }

    private const val SCHEMA = "org.gnome.desktop.peripherals.touchpad"
    private const val KEY = "natural-scroll"
    private const val TIMEOUT_SECONDS = 5L
}
