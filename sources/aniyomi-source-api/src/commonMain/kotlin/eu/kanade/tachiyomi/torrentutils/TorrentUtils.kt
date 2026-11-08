package eu.kanade.tachiyomi.torrentutils

import app.katari.aniyomi.runtime.TorrentUnsupportedException
import eu.kanade.tachiyomi.torrentutils.model.TorrentInfo

/**
 * Katari plays no torrents, so these calls always fail. They exist so that torrent extensions load and can be shown
 * as unsupported, instead of failing to load.
 */
object TorrentUtils {

    suspend fun getTorrentInfo(url: String, title: String): TorrentInfo = throw TorrentUnsupportedException()

    // Komikku's fork declares the call without suspend, which is a different JVM signature
    @Deprecated("Binary compatibility with Komikku's extensions-lib only", level = DeprecationLevel.HIDDEN)
    @JvmName("getTorrentInfo")
    fun getTorrentInfoBlocking(url: String, title: String): TorrentInfo = throw TorrentUnsupportedException()
}
