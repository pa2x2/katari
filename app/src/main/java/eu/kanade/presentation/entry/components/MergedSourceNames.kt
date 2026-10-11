package eu.kanade.presentation.entry.components

/** Names the first two sources of a merge and counts the rest: "MangaDex, Asura Scans +1". */
internal fun mergedSourceNames(names: List<String>): String {
    val shown = names.take(MAX_NAMED_SOURCES).joinToString(", ")
    val hidden = names.size - MAX_NAMED_SOURCES
    return if (hidden > 0) "$shown +$hidden" else shown
}

private const val MAX_NAMED_SOURCES = 2
