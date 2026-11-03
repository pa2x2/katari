package mihon.entry.interactions.host.tracking

import eu.kanade.tachiyomi.data.track.EntryTrackingSource
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import eu.kanade.tachiyomi.source.implementationClass
import tachiyomi.domain.source.model.SourceDisplayInfo

internal fun UnifiedSource.toEntryTrackingSource(displayInfo: SourceDisplayInfo): EntryTrackingSource =
    EntryTrackingSource(
        sourceId = id,
        name = displayInfo.name,
        lang = displayInfo.lang,
        className = implementationClass.qualifiedName,
    )
