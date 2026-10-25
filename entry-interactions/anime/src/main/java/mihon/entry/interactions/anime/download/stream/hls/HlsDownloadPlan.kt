package mihon.entry.interactions.anime.download.stream.hls

import mihon.entry.interactions.anime.download.stream.RemoteText
import mihon.entry.interactions.anime.download.stream.segments.SegmentFile
import mihon.entry.interactions.anime.download.stream.segments.SegmentPlan
import mihon.entry.interactions.anime.stream.PreferredHeight
import okhttp3.HttpUrl

/**
 * What downloading the HLS stream whose playlist is [root] takes, with every playlist it needs fetched by [fetch]:
 * the root as [rootFileName], and the variant and renditions of a multivariant root as `<rootName>-v`, `-a<n>` for
 * audio and `-s<n>` for subtitles, each naming its files after itself.
 */
internal suspend fun planHlsDownload(
    root: RemoteText,
    preferredHeight: PreferredHeight,
    isDecodable: (codecs: String?) -> Boolean,
    rootName: String,
    rootFileName: String,
    fetch: suspend (HttpUrl) -> RemoteText,
): SegmentPlan {
    val playlists = LinkedHashMap<String, String>()
    val files = ArrayList<SegmentFile>()
    fun addMediaPlaylist(playlist: RemoteText, name: String, prefix: String) {
        if (isHlsMasterPlaylist(playlist.text)) {
            throw UnsupportedOperationException("A variant that is a multivariant playlist: ${playlist.url}")
        }
        val local = localHlsMediaPlaylist(playlist.text, playlist.url, prefix)
        playlists[name] = local.text
        files += local.files
    }

    if (!isHlsMasterPlaylist(root.text)) {
        addMediaPlaylist(root, rootFileName, "$rootName-v")
        return SegmentPlan(rootFileName, playlists, files)
    }

    val master = HlsMasterPlaylist(root.text.lines(), root.url)
    val choice = chooseHlsVariant(master, preferredHeight, isDecodable)
    val variantName = "$rootName-v$PLAYLIST_EXTENSION"
    addMediaPlaylist(fetch(choice.variant.url), variantName, "$rootName-v")
    val renditionNames = HashMap<HlsRendition, String>()
    val counts = HashMap<String, Int>()
    for (rendition in choice.renditions) {
        val url = rendition.url ?: continue
        val kind = if (rendition.type == "SUBTITLES") "s" else "a"
        val prefix = "$rootName-$kind${counts.merge(kind, 1, Int::plus)}"
        val playlist = fetch(url)
        renditionNames[rendition] = if (isHlsPlaylist(playlist.text)) {
            "$prefix$PLAYLIST_EXTENSION".also { addMediaPlaylist(playlist, it, prefix) }
        } else {
            // Some sources point a subtitle rendition straight at its WebVTT file, which is kept as it came.
            "$prefix.vtt".also { playlists[it] = playlist.text }
        }
    }
    playlists[rootFileName] = master.local(choice.variant, variantName, choice.renditions, renditionNames::getValue)
    return SegmentPlan(rootFileName, playlists, files)
}

/**
 * Playlists are written as `.m3u`: Android's shared storage gives a file it types as a playlist its type's own
 * extension, so a `.m3u8` comes out as `.m3u` and the playlist naming it would point at nothing.
 */
internal const val PLAYLIST_EXTENSION = ".m3u"
