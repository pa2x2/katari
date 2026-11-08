package app.katari.core.common.network.cookie

import app.katari.core.common.storage.replaceAtomically
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import logcat.LogPriority
import okhttp3.Cookie
import tachiyomi.core.common.util.system.logcat
import java.io.IOException
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path

/**
 * The JSON file a [DesktopCookieStore] keeps its cookies in, with every attribute a cookie is matched and sent by, so a
 * cookie reads back as the one that was saved.
 */
internal class CookieFile(private val path: Path) {

    /** The stored cookies, or none when the file is missing or unreadable, so a damaged file never stops the app. */
    fun read(): List<Cookie> {
        val text = try {
            Files.readString(path)
        } catch (_: NoSuchFileException) {
            return emptyList()
        } catch (e: IOException) {
            logcat(LogPriority.ERROR, e) { "Could not read cookies from $path; starting without them" }
            return emptyList()
        }
        val stored = try {
            json.decodeFromString<StoredCookies>(text).cookies
        } catch (e: SerializationException) {
            logcat(LogPriority.ERROR, e) { "Malformed cookies in $path; starting without them" }
            return emptyList()
        }
        return stored.mapNotNull { it.toCookie() }
    }

    /** Replaces the file with [cookies], leaving the previous file whole if it fails. Returns whether it succeeded. */
    fun write(cookies: List<Cookie>): Boolean {
        val bytes = json.encodeToString(StoredCookies(cookies.map { it.toStoredCookie() })).encodeToByteArray()
        return try {
            replaceAtomically(path, bytes)
            true
        } catch (e: IOException) {
            logcat(LogPriority.ERROR, e) { "Could not write cookies to $path" }
            false
        }
    }

    @Serializable
    private data class StoredCookies(val cookies: List<StoredCookie>)

    @Serializable
    private data class StoredCookie(
        val name: String,
        val value: String,
        val domain: String,
        val hostOnly: Boolean,
        val path: String,
        /** Milliseconds since the epoch; null for a session cookie, which has no expiry. */
        val expiresAt: Long? = null,
        val secure: Boolean = false,
        val httpOnly: Boolean = false,
        val sameSite: String? = null,
    ) {
        /** The cookie, or null where the file was edited into one OkHttp refuses. */
        fun toCookie(): Cookie? = try {
            Cookie.Builder()
                .name(name)
                .value(value)
                .apply { if (hostOnly) hostOnlyDomain(domain) else domain(domain) }
                .path(path)
                .apply {
                    expiresAt?.let(::expiresAt)
                    if (secure) secure()
                    if (httpOnly) httpOnly()
                    sameSite?.let(::sameSite)
                }
                .build()
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun Cookie.toStoredCookie() = StoredCookie(
        name = name,
        value = value,
        domain = domain,
        hostOnly = hostOnly,
        path = path,
        expiresAt = expiresAt.takeIf { persistent },
        secure = secure,
        httpOnly = httpOnly,
        sameSite = sameSite,
    )

    private companion object {
        val json = Json { ignoreUnknownKeys = true }
    }
}
