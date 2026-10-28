package mihon.desktop.source

import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import eu.kanade.tachiyomi.source.entry.entryItemOrientation
import eu.kanade.tachiyomi.source.entry.supportedEntryTypes
import tachiyomi.domain.source.model.EntrySourceDescription
import tachiyomi.domain.source.model.UnifiedStubSource
import tachiyomi.domain.source.service.EntrySourceDescriptionResolutionPort

/**
 * Describes sources as Android describes one that offers no catalogue: by language, entry types and cover orientation.
 * Desktop loads no sources, so every source is such a stub; this stands in for the catalogue feature, which reaches
 * sources through Android's extension loading, until desktop loads extensions.
 */
@Inject
class NoSourceDescriptions : EntrySourceDescriptionResolutionPort {
    override fun describe(source: UnifiedSource): EntrySourceDescription = EntrySourceDescription(
        language = (source as? UnifiedStubSource)?.lang.orEmpty(),
        supportedEntryTypes = source.supportedEntryTypes()?.toSet(),
        itemOrientation = source.entryItemOrientation(),
        catalogue = null,
    )
}
