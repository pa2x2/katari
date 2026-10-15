package mihon.entry.interactions.catalogue.host

import eu.kanade.tachiyomi.source.entry.UnifiedSource
import tachiyomi.domain.source.model.EntrySourceDescription
import tachiyomi.domain.source.service.EntrySourceDescriptionResolutionPort

internal class EntrySourceDescriptionAdapter(
    private val host: EntryCatalogueProviderHost,
) : EntrySourceDescriptionResolutionPort {
    override fun describe(source: UnifiedSource): EntrySourceDescription {
        return host.describe(source)
    }
}
