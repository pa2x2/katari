package mihon.entry.interactions.anime.stream

import eu.kanade.tachiyomi.source.entry.VideoPlaybackOption

/**
 * The source quality nearest [preferred] among [options], read from labels such as "1080p" or "4K". Null when no
 * option names a height, so the source's own pick stands.
 */
internal fun pickPreferredSourceQuality(options: List<VideoPlaybackOption>, preferred: PreferredHeight): String? {
    return pickByHeight(options, preferred) { videoHeightOf(it.label) ?: videoHeightOf(it.key) }?.key
}
