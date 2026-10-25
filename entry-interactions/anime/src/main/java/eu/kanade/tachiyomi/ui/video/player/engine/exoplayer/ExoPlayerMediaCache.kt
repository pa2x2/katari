package eu.kanade.tachiyomi.ui.video.player.engine.exoplayer

import android.app.Application
import android.text.format.Formatter
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import eu.kanade.tachiyomi.util.storage.DiskUtil
import mihon.entry.interactions.runtime.EntryPlayerCache
import java.io.File

/**
 * The ExoPlayer engine's on-disk cache of streamed media, up to 256 MiB, least recently used first out. It is also the
 * anime playback cache the cache settings show and clear.
 */
@OptIn(markerClass = [UnstableApi::class])
internal class ExoPlayerMediaCache(
    private val context: Application,
) : EntryPlayerCache {
    private val cacheDir = File(context.cacheDir, CACHE_DIRECTORY_NAME)
    private val cacheDelegate = lazy {
        SimpleCache(
            cacheDir,
            LeastRecentlyUsedCacheEvictor(MAX_CACHE_BYTES),
            StandaloneDatabaseProvider(context),
        )
    }

    val cache: SimpleCache
        get() = cacheDelegate.value

    override val readableSize: String
        get() = Formatter.formatFileSize(context, DiskUtil.getDirectorySize(cacheDir))

    override fun clear(): Int {
        return if (cacheDelegate.isInitialized()) {
            val keys = cache.keys.toList()
            keys.forEach(cache::removeResource)
            keys.size
        } else {
            cacheDir.listFiles()?.count { it.deleteRecursively() } ?: 0
        }
    }

    private companion object {
        private const val CACHE_DIRECTORY_NAME = "anime_player_media"
        private const val MAX_CACHE_BYTES = 256L * 1024L * 1024L
    }
}
