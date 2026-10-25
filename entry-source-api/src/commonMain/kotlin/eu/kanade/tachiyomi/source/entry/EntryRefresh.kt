package eu.kanade.tachiyomi.source.entry

/** Result of [UnifiedSource.refresh]. */
class EntryRefresh(
    val entry: SEntry,
    val chapters: List<SEntryChapter>,
)
