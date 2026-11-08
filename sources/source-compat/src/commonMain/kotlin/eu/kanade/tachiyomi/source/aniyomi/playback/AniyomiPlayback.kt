package eu.kanade.tachiyomi.source.aniyomi.playback

import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.model.Hoster
import eu.kanade.tachiyomi.animesource.model.Hoster.Companion.toHosterList
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.source.aniyomi.capability.hasHosters
import eu.kanade.tachiyomi.source.aniyomi.capability.resolvesVideos
import eu.kanade.tachiyomi.source.aniyomi.entry.toSEpisode
import eu.kanade.tachiyomi.source.entry.EntryMedia
import eu.kanade.tachiyomi.source.entry.PlaybackDescriptor
import eu.kanade.tachiyomi.source.entry.PlaybackSelection
import eu.kanade.tachiyomi.source.entry.SEntryChapter
import eu.kanade.tachiyomi.source.entry.VideoPlaybackOption
import eu.kanade.tachiyomi.source.entry.VideoStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.io.IOException

/**
 * An extension's episodes as Katari playback: its hosters become servers and its videos streams. A video gets its link
 * only once picked when that takes work: an extensions-lib 14 video whose link is on its own page, or one from an
 * extension that finishes links in `resolveVideo`. Streams served by the extension's own loopback server are pointed at
 * it. What an episode lists stays cached for a few minutes, so switching servers or resolving a stream doesn't ask the
 * site again.
 */
internal class AniyomiPlayback(private val source: AnimeSource) : AutoCloseable {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val hasHosters = source.hasHosters
    private val resolvesVideos = source.resolvesVideos
    private val loopbackServer = AniyomiLoopbackServer(source)
    private val episodes = object : LinkedHashMap<String, CachedEpisode>(CACHED_EPISODES, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CachedEpisode>) = size > CACHED_EPISODES
    }

    private val sourceHeaders get() = (source as? AnimeHttpSource)?.headers

    suspend fun media(episode: SEntryChapter, selection: PlaybackSelection): EntryMedia {
        val servers = cachedOrListed(episode)
        val pick = servers.open(selection.serverKey, EAGER_SERVERS_DEADLINE_MS)
        val videos = pick?.videos.orEmpty()
        val streams = streamsOf(videos, sourceHeaders, ::isDeferred).map { it.onLoopbackServer() }
        return EntryMedia.Playback(
            PlaybackDescriptor(
                selection = PlaybackSelection(
                    serverKey = pick?.server?.key?.takeIf { servers.showsServers },
                    streamKey = streams.getOrNull(videos.indexOfFirst(Video::preferred))?.key,
                ),
                servers = if (servers.showsServers) {
                    servers.options.map { VideoPlaybackOption(it.key, it.hoster.hosterName) }
                } else {
                    emptyList()
                },
                streams = streams,
            ),
        )
    }

    suspend fun resolve(episode: SEntryChapter, stream: VideoStream): VideoStream {
        val video = cached(episode)?.video(stream.key)
            ?: cachedOrListed(episode).also { it.open(null, EAGER_SERVERS_DEADLINE_MS) }.video(stream.key)
            ?: throw IOException("Stream '${stream.key}' is no longer listed")
        return withLink(video).toVideoStream(stream.key, sourceHeaders).onLoopbackServer()
    }

    override fun close() {
        scope.cancel()
        loopbackServer.close()
    }

    private fun isDeferred(video: Video): Boolean = video.linkOnLib14Page || (resolvesVideos && !video.initialized)

    private suspend fun withLink(video: Video): Video {
        var linked = video
        if (linked.linkOnLib14Page) {
            val httpSource = source as? AnimeHttpSource ?: throw IOException("Source ${source.name} can't link a video")
            linked = linked.copy(videoUrl = httpSource.getVideoUrl(linked))
        }
        if (resolvesVideos && !linked.initialized) {
            linked = (source as? AnimeHttpSource)?.resolveVideo(linked)
                ?: throw IOException("Source ${source.name} couldn't resolve '${video.videoTitle}'")
        }
        return linked
    }

    private fun VideoStream.onLoopbackServer(): VideoStream {
        if (!usesLoopbackServer) return this
        return loopbackServer.url()?.let(::withLoopbackServer) ?: this
    }

    private fun cached(episode: SEntryChapter): EpisodeServers? = synchronized(episodes) {
        episodes[episode.url]?.takeIf { System.currentTimeMillis() - it.listedAt < CACHE_LIFETIME_MS }?.servers
    }

    private suspend fun cachedOrListed(episode: SEntryChapter): EpisodeServers {
        cached(episode)?.let { return it }
        val hosters = hostersOf(episode)
        val servers = EpisodeServers(
            hosters = hosters,
            scope = scope,
            sortVideos = ::sortVideos,
            fetchVideos = { hoster -> videosOf(episode, hoster) },
        )
        // An episode without hosters is asked again on retry
        if (hosters.isNotEmpty()) {
            synchronized(episodes) { episodes[episode.url] = CachedEpisode(servers, System.currentTimeMillis()) }
        }
        return servers
    }

    // A source without hosters gets one placeholder, as extensions-lib 14 sources built for later libs return
    private suspend fun hostersOf(episode: SEntryChapter): List<Hoster> {
        if (!hasHosters) return listOf(Hoster(hosterName = Hoster.NO_HOSTER_LIST))
        val hosters = try {
            source.getHosterList(episode.toSEpisode())
        } catch (_: UnsupportedOperationException) {
            // A class built against an older lib may carry the interface's default as its own method
            return source.getVideoList(episode.toSEpisode()).toHosterList()
        }
        return (source as? AnimeHttpSource)?.run { hosters.sortHosters() } ?: hosters
    }

    // A placeholder without its videos has no URL to fetch; its videos come from the episode, as in Aniyomi
    private suspend fun videosOf(episode: SEntryChapter, hoster: Hoster): List<Video> =
        if (hasHosters && hoster.hosterName != Hoster.NO_HOSTER_LIST) {
            source.getVideoList(hoster)
        } else {
            source.getVideoList(episode.toSEpisode())
        }

    private fun sortVideos(videos: List<Video>): List<Video> =
        (source as? AnimeHttpSource)?.run { videos.sortVideos() } ?: videos

    private class CachedEpisode(val servers: EpisodeServers, val listedAt: Long)

    private companion object {
        // Below the host's 15 seconds for the whole answer, which also lists the hosters
        const val EAGER_SERVERS_DEADLINE_MS = 10_000L
        const val CACHE_LIFETIME_MS = 5 * 60_000L
        const val CACHED_EPISODES = 8
    }
}
