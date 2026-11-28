package eu.kanade.tachiyomi.data.backup.conversion.aniyomi

import tachiyomi.domain.entry.model.Entry

/*
 * Aniyomi's episode list flags (`tachiyomi.domain.entries.anime.model.Anime`, aniyomiorg/aniyomi 97414446b8a9), which
 * an anime and the default settings for new ones both hold. Most sit where Katari keeps its chapter flags; the filler
 * filter, the sort and the hidden previews and summaries sit elsewhere.
 */

/** Katari's filler filter for Aniyomi's fillermarked filter in [episodeFlags]. */
internal fun katariFillerFilter(episodeFlags: Long): Long = when (episodeFlags and EPISODE_FILLERMARKED_MASK) {
    EPISODE_SHOW_FILLERMARKED -> Entry.CHAPTER_SHOW_FILLER
    EPISODE_SHOW_NOT_FILLERMARKED -> Entry.CHAPTER_SHOW_NOT_FILLER
    else -> Entry.SHOW_ALL
}

/** Katari's chapter sort for Aniyomi's episode sort in [episodeFlags]. */
internal fun katariSorting(episodeFlags: Long): Long = when (episodeFlags and EPISODE_SORTING_MASK) {
    EPISODE_SORTING_NUMBER -> Entry.CHAPTER_SORTING_NUMBER
    EPISODE_SORTING_UPLOAD_DATE -> Entry.CHAPTER_SORTING_UPLOAD_DATE
    EPISODE_SORTING_ALPHABET -> Entry.CHAPTER_SORTING_ALPHABET
    else -> Entry.CHAPTER_SORTING_SOURCE
}

/** Whether [episodeFlags] hides episode previews. */
internal fun hidesPreviews(episodeFlags: Long): Boolean = episodeFlags and EPISODE_HIDE_PREVIEWS != 0L

/** Whether [episodeFlags] hides episode summaries. */
internal fun hidesSummaries(episodeFlags: Long): Boolean = episodeFlags and EPISODE_HIDE_SUMMARIES != 0L

private const val EPISODE_SHOW_FILLERMARKED = 0x00000080L
private const val EPISODE_SHOW_NOT_FILLERMARKED = 0x00000100L
private const val EPISODE_FILLERMARKED_MASK = 0x00000180L
private const val EPISODE_SORTING_NUMBER = 0x00000200L
private const val EPISODE_SORTING_UPLOAD_DATE = 0x00000400L
private const val EPISODE_SORTING_ALPHABET = 0x00000600L
private const val EPISODE_SORTING_MASK = 0x00000600L
private const val EPISODE_HIDE_PREVIEWS = 0x00000800L
private const val EPISODE_HIDE_SUMMARIES = 0x00001000L
