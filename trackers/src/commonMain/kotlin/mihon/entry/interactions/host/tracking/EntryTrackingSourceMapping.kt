package mihon.entry.interactions.host.tracking

import eu.kanade.tachiyomi.data.track.EntryTrackingSource
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import tachiyomi.domain.source.model.SourceDisplayInfo

internal fun UnifiedSource.toEntryTrackingSource(
    displayInfo: SourceDisplayInfo,
    legacyClassNames: LegacySourceClassNames,
): EntryTrackingSource =
    EntryTrackingSource(
        sourceId = id,
        name = displayInfo.name,
        lang = displayInfo.lang,
        legacyClassName = legacyClassNames.of(this),
    )
