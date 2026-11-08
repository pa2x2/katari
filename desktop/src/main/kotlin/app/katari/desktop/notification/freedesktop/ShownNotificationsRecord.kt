package app.katari.desktop.notification.freedesktop

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import java.io.IOException
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.deleteIfExists
import kotlin.io.path.readText
import kotlin.io.path.writeText

/**
 * Linux: the server ids of the app's notifications on screen, kept in [file] so that a later run of the app replaces
 * them instead of posting a second copy, as Android's fixed ids do. Ids are only the server's that gave them, so the
 * record names that server by [ShownNotifications.server], and a record of another server is never used. Only the
 * notifications' worker reads and writes it, and only the running instance of the app has one.
 */
internal class ShownNotificationsRecord(private val file: Path) {

    /** What an earlier write left, or null for none or one this build can't read. */
    fun read(): ShownNotifications? = try {
        json.decodeFromString<ShownNotifications>(file.readText())
    } catch (_: NoSuchFileException) {
        null
    } catch (e: IOException) {
        logcat(LogPriority.WARN, e) { "Couldn't read the notifications an earlier run showed" }
        null
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }

    /** Replaces the record with [shown] in one step, or removes it when nothing shows. */
    fun write(shown: ShownNotifications) {
        try {
            if (shown.notifications.isEmpty()) {
                file.deleteIfExists()
                return
            }
            val written = file.resolveSibling("${file.fileName}.new")
            written.writeText(json.encodeToString(shown))
            Files.move(written, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (e: IOException) {
            logcat(LogPriority.WARN, e) { "Couldn't record the notifications on screen" }
        }
    }

    fun clear() {
        try {
            file.deleteIfExists()
        } catch (e: IOException) {
            logcat(LogPriority.WARN, e) { "Couldn't remove the record of the notifications on screen" }
        }
    }

    private companion object {
        val json = Json { ignoreUnknownKeys = true }
    }
}

/** @param server the [NotificationServer.identity] of the server that gave the ids. */
@Serializable
internal data class ShownNotifications(val server: String, val notifications: List<ShownNotificationId>)

/** @param ongoing whether it shows work under way, which ends with the run that showed it. */
@Serializable
internal data class ShownNotificationId(val key: String, val id: Int, val ongoing: Boolean)
