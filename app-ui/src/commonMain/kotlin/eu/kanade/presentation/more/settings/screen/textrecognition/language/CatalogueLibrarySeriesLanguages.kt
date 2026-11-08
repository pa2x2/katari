package eu.kanade.presentation.more.settings.screen.textrecognition.language

import app.katari.entry.interactions.catalogue.EntryCatalogueFeature
import app.katari.language.api.tag.LanguageTag
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.first
import tachiyomi.domain.entry.repository.EntryRepository

/** Reads each series' language from the catalogue's description of its source. */
@Inject
@ContributesBinding(AppScope::class)
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
