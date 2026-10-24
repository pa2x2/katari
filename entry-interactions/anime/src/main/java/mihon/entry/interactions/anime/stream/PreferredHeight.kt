package mihon.entry.interactions.anime.stream

/**
 * The height to start at when the title has no saved pick: the anime player's preferred quality, or a download's
 * quality mode.
 */
internal sealed interface PreferredHeight {
    data object Highest : PreferredHeight

    data object Lowest : PreferredHeight

    /** The exact height, else the highest below it, else the lowest above it. */
    data class Near(val height: Int) : PreferredHeight

    companion object {
        /** The anime player's preferred quality setting, where 0 asks for the highest. */
        fun ofPlayerSetting(height: Int): PreferredHeight = if (height <= 0) Highest else Near(height)
    }
}

/** The item nearest [preferred]. Null when no item has a height, so the next rule decides. */
internal fun <T> pickByHeight(items: List<T>, preferred: PreferredHeight, heightOf: (T) -> Int?): T? {
    val heights = items.mapNotNull { item -> heightOf(item)?.let { item to it } }
    if (heights.isEmpty()) return null
    return when (preferred) {
        PreferredHeight.Highest -> heights.maxBy { it.second }
        PreferredHeight.Lowest -> heights.minBy { it.second }
        is PreferredHeight.Near -> heights.firstOrNull { it.second == preferred.height }
            ?: heights.filter { it.second < preferred.height }.maxByOrNull { it.second }
            ?: heights.minBy { it.second }
    }.first
}

private val HEIGHT_PATTERN = Regex("""(\d{3,4})\s*[pP]\b""")
private val K_PATTERN = Regex("""\b([248])\s*[kK]\b""")

/** The height a label such as "1080p", "720p Ultra" or "4K" names. */
internal fun videoHeightOf(text: String): Int? {
    HEIGHT_PATTERN.find(text)?.let { return it.groupValues[1].toInt() }
    return when (K_PATTERN.find(text)?.groupValues?.get(1)) {
        "2" -> 1440
        "4" -> 2160
        "8" -> 4320
        else -> null
    }
}
