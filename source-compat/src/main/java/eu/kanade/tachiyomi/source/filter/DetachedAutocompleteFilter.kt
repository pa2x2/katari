package eu.kanade.tachiyomi.source.filter

import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.source.entry.EntryFilterSuggestion
import eu.kanade.tachiyomi.source.entry.EntryFilterTextEdit
import eu.kanade.tachiyomi.source.entry.EntryFilterTextInput
import eu.kanade.tachiyomi.source.entry.filter.EntryFilterMetadata
import eu.kanade.tachiyomi.source.entry.filter.EntryFilterMetadataProvider
import eu.kanade.tachiyomi.source.entry.filter.EntryFilterValidator
import eu.kanade.tachiyomi.source.entry.filter.EntryFilterValueMigration

internal fun EntryFilter.Autocomplete.detachedAutocomplete(
    source: EntryFilter.Autocomplete,
    owner: EntryFilterBinding,
): EntryFilter.Autocomplete =
    object :
        EntryFilter.Autocomplete(
            name,
            state,
            options,
        ),
        DetachedEntryFilter,
        EntryFilterMetadataProvider,
        EntryFilterValueMigration by FilterCapabilities(source),
        EntryFilterValidator by FilterCapabilities(source) {
        override val sourceFilter = source
        override val binding = owner
        override val filterMetadata get() = (source as? EntryFilterMetadataProvider)?.filterMetadata
            ?: EntryFilterMetadata()
        override fun getSuggestionQuery(input: EntryFilterTextInput): String? = source.getSuggestionQuery(input)
        override suspend fun getSuggestions(input: EntryFilterTextInput, query: String): List<EntryFilterSuggestion> =
            EntryFilterList(this).withSourceFilterValues { source.getSuggestions(input, query) }
        override fun applySuggestion(
            input: EntryFilterTextInput,
            suggestion: EntryFilterSuggestion,
        ): EntryFilterTextEdit =
            source.applySuggestion(input, suggestion)
    }
