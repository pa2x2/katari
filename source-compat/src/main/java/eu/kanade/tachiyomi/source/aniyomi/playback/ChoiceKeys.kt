package eu.kanade.tachiyomi.source.aniyomi.playback

/**
 * Keys for choices the viewer picks by name, such as servers and streams: each name, with a number after the second and
 * later ones that repeat it, so two "1080p" streams stay apart and still match on the next episode.
 */
internal fun distinctKeys(names: List<String>): List<String> {
    val seen = mutableMapOf<String, Int>()
    return names.map { name ->
        val count = (seen[name] ?: 0) + 1
        seen[name] = count
        if (count == 1) name else "$name ($count)"
    }
}
