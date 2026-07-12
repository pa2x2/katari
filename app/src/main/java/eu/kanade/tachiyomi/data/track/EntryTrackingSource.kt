package eu.kanade.tachiyomi.data.track

import eu.kanade.tachiyomi.source.entry.UnifiedSource
import eu.kanade.tachiyomi.source.toLegacySourceForCompatibility
import tachiyomi.domain.source.model.SourceDisplayInfo

data class EntryTrackingSource(
    val sourceId: Long,
    val name: String,
    val lang: String,
    val legacyClassName: String?,
) {
    companion object {
        fun from(source: UnifiedSource, displayInfo: SourceDisplayInfo): EntryTrackingSource =
            EntryTrackingSource(
                sourceId = source.id,
                name = displayInfo.name,
                lang = displayInfo.lang,
                legacyClassName = source.toLegacySourceForCompatibility()?.let { it::class.qualifiedName },
            )
    }
}
