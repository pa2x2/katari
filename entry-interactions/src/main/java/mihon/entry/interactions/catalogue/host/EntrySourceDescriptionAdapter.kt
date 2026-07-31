package mihon.entry.interactions.catalogue.host

import eu.kanade.tachiyomi.source.entry.UnifiedSource
import mihon.entry.interactions.catalogue.runtime.EntryCatalogueGraphStateValidator
import tachiyomi.domain.source.model.EntrySourceDescription
import tachiyomi.domain.source.service.EntrySourceDescriptionResolutionPort

internal class EntrySourceDescriptionAdapter(
    private val host: EntryCatalogueProviderHost,
    private val graphStateValidator: EntryCatalogueGraphStateValidator,
) : EntrySourceDescriptionResolutionPort {
    override fun describe(source: UnifiedSource): EntrySourceDescription {
        return host.describe(source).also(graphStateValidator::validate)
    }
}
