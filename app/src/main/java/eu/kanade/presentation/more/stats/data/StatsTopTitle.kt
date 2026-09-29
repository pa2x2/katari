package eu.kanade.presentation.more.stats.data

import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.entry.model.EntryCover

data class StatsTopTitle(
    val entryId: Long,
    val type: EntryType,
    val title: String,
    val durationMillis: Long,
    val cover: EntryCover,
    val completionCount: Long,
)
