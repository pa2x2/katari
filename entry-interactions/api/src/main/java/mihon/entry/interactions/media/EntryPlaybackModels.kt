package mihon.entry.interactions

import kotlinx.serialization.Serializable

@Serializable
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

@Serializable
enum class EntryPlaybackQualityMode {
    AUTO,
    SPECIFIC_HEIGHT,
}
