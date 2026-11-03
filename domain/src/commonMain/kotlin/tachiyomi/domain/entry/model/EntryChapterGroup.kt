package tachiyomi.domain.entry.model

/**
 * A part of an entry's story its source declares, such as a volume; chapters name it by [key]. [position] is its place
 * in reading order among the entry's groups.
 */
data class EntryChapterGroup(
    val entryId: Long,
    val key: String,
    val title: String,
    val position: Int,
)
