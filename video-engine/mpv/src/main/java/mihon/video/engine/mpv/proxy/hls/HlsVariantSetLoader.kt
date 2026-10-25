package mihon.video.engine.mpv.proxy.hls

import okhttp3.HttpUrl
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future

/** A media playlist as fetched: its text, and its URL after redirects, which its references resolve against. */
internal class FetchedPlaylist(val url: HttpUrl, val text: String)

/** The master playlist mpv gets, and the sets of variants in it that the proxy switches between. */
internal class HlsVariantSetMaster(val text: String, val sets: List<HlsVariantSet>, val setPlaylists: List<String>)

/**
 * Finds the sets of [master]'s variants that can switch at any segment ([findHlsVariantSets]), fetching the media
 * playlists of the variants that might, at once, with [fetch], which returns null when one doesn't load. A variant
 * that doesn't load plays as mpv's own track. Null when there is no set.
 *
 * [base] is where the master playlist came from after redirects; [route] gives the proxy URL of a reference resolved
 * against a base, and [setUrl] and [segmentUrl] the proxy URLs of a set's playlist and its segments.
 */
internal fun loadHlsVariantSets(
    master: HlsMasterPlaylist,
    base: HttpUrl,
    executor: ExecutorService,
    fetch: (HttpUrl) -> FetchedPlaylist?,
    route: (HttpUrl, String) -> String?,
    setUrl: (Int) -> String,
    segmentUrl: (set: Int, segment: Int) -> String,
): HlsVariantSetMaster? {
    val candidates = hlsVariantSetCandidates(master)
    if (candidates.isEmpty()) return null
    val fetches: List<Pair<HlsVariant, Future<FetchedPlaylist?>>> = candidates.mapNotNull { variant ->
        val url = base.resolve(variant.uri) ?: return@mapNotNull null
        variant to executor.submit<FetchedPlaylist?> { fetch(url) }
    }
    val loaded = fetches.mapNotNull { (variant, future) ->
        val fetched = runCatching { future.get() }.getOrNull() ?: return@mapNotNull null
        HlsLoadedVariant(variant, fetched.url, parseHlsMediaPlaylist(fetched.text))
    }
    val sets = findHlsVariantSets(loaded).ifEmpty { return null }
    val (text, programIds) = hlsMasterWithVariantSets(master, sets, setUrl) { reference -> route(base, reference) }
    return HlsVariantSetMaster(
        text = text,
        sets = sets.mapIndexed { index, set -> HlsVariantSet(programIds[index], set) },
        setPlaylists = sets.mapIndexed { index, set ->
            hlsVariantSetPlaylist(set, { segment -> segmentUrl(index, segment) }, route)
        },
    )
}
