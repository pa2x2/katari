package eu.kanade.tachiyomi.source.aniyomi.playback

import eu.kanade.tachiyomi.animesource.model.Hoster
import eu.kanade.tachiyomi.animesource.model.Video
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withTimeoutOrNull

/** One of the extension's hosters as a server, under a key unique within the episode. */
internal class ServerOption(val key: String, val hoster: Hoster)

internal class ServerPick(val server: ServerOption, val videos: List<Video>)

/**
 * The server an episode opens on, and its videos. A requested server loads on its own, even one marked lazy. Otherwise,
 * as in Aniyomi, every server not marked lazy loads at once: the first to arrive with a video the extension prefers
 * wins right away; else, once all are done or [deadlineMs] has passed, the first in the extension's order with any
 * videos. With only lazy servers, the first one loads. [load] starts or joins a server's loading and doesn't fail: a
 * server that fails has no videos.
 */
internal suspend fun pickServer(
    servers: List<ServerOption>,
    requestedKey: String?,
    deadlineMs: Long,
    load: (ServerOption) -> Deferred<List<Video>>,
): ServerPick? {
    if (servers.isEmpty()) return null
    servers.find { it.key == requestedKey }?.let { return ServerPick(it, load(it).await()) }

    val candidates = servers.filterNot { it.hoster.lazy }.ifEmpty { listOf(servers.first()) }
    val loads = candidates.associateWith(load)
    // In order of arrival
    val arrived = linkedMapOf<ServerOption, List<Video>>()
    withTimeoutOrNull(deadlineMs) {
        while (arrived.size < loads.size) {
            val (server, videos) = select {
                loads.filterKeys { it !in arrived }.forEach { (server, videos) -> videos.onAwait { server to it } }
            }
            arrived[server] = videos
            if (videos.any(Video::preferred)) break
        }
    }
    val pick = arrived.entries.firstOrNull { (_, videos) -> videos.any(Video::preferred) }?.toPair()
        ?: candidates.firstNotNullOfOrNull { server ->
            arrived[server]?.takeIf { it.isNotEmpty() }?.let { server to it }
        }
        ?: (candidates.first() to arrived[candidates.first()].orEmpty())
    return ServerPick(pick.first, pick.second)
}
