package eu.kanade.tachiyomi.data.track.source

import okhttp3.OkHttpClient

/**
 * What the self-hosted trackers (Kavita, Suwayomi) read from the installed extension source that talks to the same
 * server: its settings, where the user entered the server's address and key, and for Suwayomi the source's client.
 * Each lookup takes the source's local key from [eu.kanade.tachiyomi.data.track.TrackerSourceKeys] and returns null
 * when that source can't provide it, such as when it isn't installed.
 */
interface TrackerSourceAccess {
    fun settings(sourceKey: Long): TrackerSourceSettings?

    fun connection(sourceKey: Long): TrackerSourceConnection?
}

/** The settings an extension source stores under its own keys. */
interface TrackerSourceSettings {
    fun getString(key: String, defaultValue: String): String?

    fun getBoolean(key: String, defaultValue: Boolean): Boolean
}

class TrackerSourceConnection(
    val settings: TrackerSourceSettings,
    val homeUrl: String,
    val client: OkHttpClient,
)
