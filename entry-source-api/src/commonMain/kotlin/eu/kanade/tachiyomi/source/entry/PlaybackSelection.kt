package eu.kanade.tachiyomi.source.entry

import kotlinx.serialization.Serializable

/**
 * User or source selection keys sent to [UnifiedSource.getMedia].
 *
 * Sources may expose dub and source quality choices. When a requested selection
 * is not available, the source should return a resolved fallback selection in
 * the resulting [PlaybackDescriptor].
 *
 * For manga this object is ignored by the adapter.
 */
@Serializable
data class PlaybackSelection(
    val dubKey: String? = null,
    val streamKey: String? = null,
    val sourceQualityKey: String? = null,
)
