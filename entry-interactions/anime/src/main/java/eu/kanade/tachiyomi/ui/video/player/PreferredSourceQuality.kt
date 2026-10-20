package eu.kanade.tachiyomi.ui.video.player

import eu.kanade.tachiyomi.source.entry.VideoPlaybackOption

/**
 * The source quality closest to [preferredHeight] among [options], read from labels such as "1080p" or "4K": the exact
 * height, else the highest below it, else the lowest above it. 0 asks for the highest. Null when no option names a
 * height, so the source's own pick stands.
 */
internal fun pickPreferredSourceQuality(options: List<VideoPlaybackOption>, preferredHeight: Int): String? {
    val heights = options.mapNotNull { option ->
        (videoHeightOf(option.label) ?: videoHeightOf(option.key))?.let { option.key to it }
    }
    if (heights.isEmpty()) return null
    if (preferredHeight <= 0) return heights.maxBy { it.second }.first
    return (
        heights.firstOrNull { it.second == preferredHeight }
            ?: heights.filter { it.second < preferredHeight }.maxByOrNull { it.second }
            ?: heights.minBy { it.second }
        ).first
}

private val HEIGHT_PATTERN = Regex("""(\d{3,4})\s*[pP]\b""")
private val K_PATTERN = Regex("""\b([248])\s*[kK]\b""")

private fun videoHeightOf(text: String): Int? {
    HEIGHT_PATTERN.find(text)?.let { return it.groupValues[1].toInt() }
    return when (K_PATTERN.find(text)?.groupValues?.get(1)) {
        "2" -> 1440
        "4" -> 2160
        "8" -> 4320
        else -> null
    }
}
