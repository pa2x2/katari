package app.katari.desktop.notification.freedesktop

import app.katari.desktop.application.icon.AppIcon
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

/**
 * The app's icon as [file], a PNG the notification server reads by its `file://` URI, the one kind of icon reference
 * the specification takes besides a name in an installed icon theme, which the app has none in. Written on first use
 * in each run; where it can't be, notifications go without an icon and the server shows its own.
 */
internal class NotificationIconFile(private val file: Path) {
    private val uri: String by lazy {
        try {
            Files.write(file, AppIcon.png(SIZE))
            file.toUri().toString()
        } catch (e: IOException) {
            logcat(LogPriority.WARN, e) { "Couldn't write the app's icon for notifications" }
            ""
        }
    }

    /** The URI to pass as a notification's `app_icon`, or the empty string for none. */
    fun uri(): String = uri

    private companion object {
        // Servers scale it down to the size they draw it at, which on a scaled display can be over a hundred pixels
        const val SIZE = 256
    }
}
