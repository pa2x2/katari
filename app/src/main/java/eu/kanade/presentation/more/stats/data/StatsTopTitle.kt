package eu.kanade.presentation.more.stats.data

import eu.kanade.tachiyomi.source.entry.EntryType

data class StatsTopTitle(
    val entryId: Long,
    val type: EntryType,
    val title: String,
    val durationMillis: Long,
)
