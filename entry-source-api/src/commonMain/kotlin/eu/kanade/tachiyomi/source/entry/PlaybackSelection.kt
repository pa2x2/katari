package eu.kanade.tachiyomi.source.entry

import kotlinx.serialization.Serializable

/**
 * User or source selection keys sent to [UnifiedSource.getMedia].
 *
 * Sources may expose dub, server and source quality choices. When a requested selection
 * is not available, the source should return a resolved fallback selection in
 * the resulting [PlaybackDescriptor].
 *
 * For manga this object is ignored by the adapter.
 *
 * @property dubKey requested dub option key.
 * @property serverKey requested server option key.
 * @property streamKey requested stream key. A source may also set it in its returned selection
 * to name the stream it prefers; the user's saved choice still wins.
 * @property sourceQualityKey requested source-quality option key.
 */
@Serializable
data class PlaybackSelection(
    val dubKey: String? = null,
    val serverKey: String? = null,
    val streamKey: String? = null,
    val sourceQualityKey: String? = null,
)
