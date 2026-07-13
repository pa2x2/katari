package mihon.entry.interactions

import tachiyomi.domain.entry.model.Entry

interface EntryPlaybackPreferencesInteraction {
    suspend fun snapshot(entry: Entry): EntryPlaybackPreferencesSnapshot?
    suspend fun restore(entry: Entry, snapshot: EntryPlaybackPreferencesSnapshot)
    suspend fun copy(sourceEntry: Entry, targetEntry: Entry)
}

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
