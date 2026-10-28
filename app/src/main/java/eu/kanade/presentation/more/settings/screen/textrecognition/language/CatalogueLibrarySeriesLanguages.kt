package eu.kanade.presentation.more.settings.screen.textrecognition.language

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.first
import mihon.entry.interactions.catalogue.EntryCatalogueFeature
import mihon.language.api.tag.LanguageTag
import tachiyomi.domain.entry.repository.EntryRepository

/** Reads each series' language from the catalogue's description of its source. */
@Inject
class CatalogueLibrarySeriesLanguages(
    private val entryRepository: EntryRepository,
    private val catalogue: EntryCatalogueFeature,
) : LibrarySeriesLanguages {
    override suspend fun languages(): List<LanguageTag> {
        catalogue.isInitialized.first { it }
        return entryRepository.getLibraryEntries().mapNotNull { entry ->
            LanguageTag.parse(catalogue.description(entry.source).language)
        }
    }
}
