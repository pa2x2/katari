package eu.kanade.tachiyomi.source.aniyomi.playback

import eu.kanade.tachiyomi.animesource.model.Hoster
import eu.kanade.tachiyomi.animesource.model.Video
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import java.util.concurrent.ConcurrentHashMap

/**
 * One episode's servers and their videos, shared by every call about the episode while it is cached: picking another
 * server or resolving a stream reuses what has loaded. Loads run in [scope], so a server still loading when the episode
 * opened elsewhere finishes for a later switch; one that failed or listed nothing loads again when next asked, so a
 * retry reaches the site. A hoster that came with its videos is never fetched; extensions-lib 14 sources wrap theirs
 * in a single [Hoster.NO_HOSTER_LIST] hoster, which isn't shown as a server.
 */
internal class EpisodeServers(
    hosters: List<Hoster>,
    private val scope: CoroutineScope,
    private val sortVideos: (List<Video>) -> List<Video>,
    private val fetchVideos: suspend (Hoster) -> List<Video>,
) {
    val options: List<ServerOption> = distinctKeys(hosters.map { it.hosterName }).zip(hosters, ::ServerOption)

    val showsServers: Boolean = hosters.singleOrNull()?.hosterName != Hoster.NO_HOSTER_LIST

    private val loads = ConcurrentHashMap<String, Deferred<List<Video>>>()
    private val loaded = ConcurrentHashMap<String, List<Video>>()

    /** The server last opened, whose streams the host resolves next. */
    @Volatile
    private var openedKey: String? = null

    suspend fun open(requestedKey: String?, deadlineMs: Long): ServerPick? =
        pickServer(options, requestedKey, deadlineMs, ::load)?.also { openedKey = it.server.key }

    /** The video listed under [streamKey], looked up on the server last opened first. */
    fun video(streamKey: String): Video? = options
        .sortedByDescending { it.key == openedKey }
        .firstNotNullOfOrNull { option ->
            val videos = loaded[option.key] ?: return@firstNotNullOfOrNull null
            videos.getOrNull(distinctKeys(videos.map(Video::videoTitle)).indexOf(streamKey))
        }

    private fun load(option: ServerOption): Deferred<List<Video>> = loads.computeIfAbsent(option.key) {
        val listed = option.hoster.videoList
        if (listed != null) {
            CompletableDeferred(sortVideos(listed).also { loaded[option.key] = it })
        } else {
            scope.async { fetchSorted(option).also { loaded[option.key] = it } }
        }
    }

    private suspend fun fetchSorted(option: ServerOption): List<Video> {
        val videos = try {
            sortVideos(fetchVideos(option.hoster))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            logcat(LogPriority.WARN, e) { "Server '${option.key}' couldn't list its videos" }
            emptyList()
        }
        if (videos.isEmpty()) loads.remove(option.key)
        return videos
    }
}
