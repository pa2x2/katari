package mihon.entry.interactions

import tachiyomi.domain.entry.model.Entry

interface EntryPlaybackInteraction {
    suspend fun snapshot(entry: Entry): EntryPlaybackSnapshot
    suspend fun restore(entry: Entry, snapshot: EntryPlaybackSnapshot)
    suspend fun copy(sourceEntry: Entry, targetEntry: Entry, chapterMappings: List<EntryPlaybackChapterMapping>)
}

data class EntryPlaybackSnapshot(
    val states: List<EntryPlaybackStateSnapshot> = emptyList(),
    val preferences: EntryPlaybackPreferencesSnapshot? = null,
)

data class EntryPlaybackStateSnapshot(
    val chapterId: Long,
    val positionMs: Long,
    val durationMs: Long,
    val completed: Boolean,
    val lastWatchedAt: Long,
)

data class EntryPlaybackPreferencesSnapshot(
    val dubKey: String? = null,
    val streamKey: String? = null,
    val sourceQualityKey: String? = null,
    val subtitleKey: String? = null,
    val playerQualityMode: EntryPlaybackQualityMode = EntryPlaybackQualityMode.AUTO,
    val playerQualityHeight: Int? = null,
    val subtitleOffsetX: Double? = null,
    val subtitleOffsetY: Double? = null,
    val subtitleTextSize: Double? = null,
    val subtitleTextColor: Int? = null,
    val subtitleBackgroundColor: Int? = null,
    val subtitleBackgroundOpacity: Double? = null,
    val updatedAt: Long = 0L,
)

enum class EntryPlaybackQualityMode {
    AUTO,
    SPECIFIC_HEIGHT,
}

data class EntryPlaybackChapterMapping(
    val sourceChapterId: Long,
    val targetChapterId: Long,
)
