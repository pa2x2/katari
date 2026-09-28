package eu.kanade.presentation.more.settings.screen.textrecognition.language

import kotlinx.coroutines.flow.first
import mihon.entry.interactions.catalogue.EntryCatalogueFeature
import mihon.language.api.tag.LanguageTag
import tachiyomi.domain.entry.repository.EntryRepository

/**
 * Languages worth offering above the full list when picking a text language: the device language, then the languages
 * of the library's series, most common first. Only languages recognition reads are suggested, which also drops the
 * `all` and `multi` codes of sources that span several languages.
 */
internal fun textRecognitionSuggestedLanguages(
    deviceLanguage: LanguageTag?,
    seriesLanguages: List<LanguageTag>,
    supported: List<LanguageTag>,
): List<LanguageTag> {
    val byUse = seriesLanguages.groupingBy { it.primary }.eachCount().entries
        .sortedByDescending { it.value }
        .map { it.key }
    return (listOfNotNull(deviceLanguage?.primary) + byUse)
        .flatMap { primary -> supported.filter { it.primary == primary } }
        .distinct()
        .take(MAX_SUGGESTED_LANGUAGES)
}

/** The content language each library series' source declares, one per series, once sources are loaded. */
internal suspend fun librarySeriesLanguages(
    entryRepository: EntryRepository,
    catalogue: EntryCatalogueFeature,
): List<LanguageTag> {
    catalogue.isInitialized.first { it }
    return entryRepository.getLibraryEntries().mapNotNull { entry ->
        LanguageTag.parse(catalogue.description(entry.source).language)
    }
}

private val LanguageTag.primary: String
    get() = value.substringBefore('-')

private const val MAX_SUGGESTED_LANGUAGES = 5
