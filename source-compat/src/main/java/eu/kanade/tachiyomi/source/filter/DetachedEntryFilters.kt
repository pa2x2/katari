package eu.kanade.tachiyomi.source.filter

import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.source.entry.filter.EntryDateFilter
import eu.kanade.tachiyomi.source.entry.filter.EntryFilterGroupSummary
import eu.kanade.tachiyomi.source.entry.filter.EntryFilterMetadata
import eu.kanade.tachiyomi.source.entry.filter.EntryFilterMetadataProvider
import eu.kanade.tachiyomi.source.entry.filter.EntryFilterStateSemantics
import eu.kanade.tachiyomi.source.entry.filter.EntryFilterValidationIssue
import eu.kanade.tachiyomi.source.entry.filter.EntryFilterValidator
import eu.kanade.tachiyomi.source.entry.filter.EntryFilterValueMigration

/** Host projections hold editor values without mutating the source's concrete filter instances. */
fun EntryFilterList.detachedCopy(binding: EntryFilterBinding? = null): EntryFilterList {
    val owner = binding ?: filterIsInstance<DetachedEntryFilter>().firstOrNull()?.binding ?: EntryFilterBinding()
    return EntryFilterList(map { it.detachedFilter(owner) })
}

internal interface DetachedEntryFilter {
    val sourceFilter: EntryFilter<*>
    val binding: EntryFilterBinding
}

private fun EntryFilter<*>.detachedFilter(owner: EntryFilterBinding): EntryFilter<*> {
    val source = (this as? DetachedEntryFilter)?.sourceFilter ?: this
    val capabilities = FilterCapabilities(source)
    return when (this) {
        is EntryFilter.Header -> object : EntryFilter.Header(name), DetachedEntryFilter {
            override val sourceFilter = source
            override val binding = owner
        }
        is EntryFilter.Separator -> object : EntryFilter.Separator(name), DetachedEntryFilter {
            override val sourceFilter = source
            override val binding = owner
        }
        is EntryDateFilter ->
            object :
                EntryDateFilter(
                    name,
                    filterMetadata,
                    null,
                    allowedPrecisions,
                    minimum,
                    maximum,
                    required,
                ),
                DetachedEntryFilter,
                EntryFilterValueMigration by capabilities {
                override fun validateFilter(values: List<EntryFilter<*>>): List<EntryFilterValidationIssue> =
                    capabilities.validateFilter(values.ifEmpty { listOf(this) })
                override val sourceFilter = source
                override val binding = owner
            }.also { it.state = state }
        is EntryFilter.Autocomplete -> detachedAutocomplete(source as EntryFilter.Autocomplete, owner)
        is EntryFilter.Text ->
            object :
                EntryFilter.Text(name, state),
                DetachedEntryFilter,
                EntryFilterMetadataProvider by capabilities,
                EntryFilterValueMigration by capabilities,
                EntryFilterValidator by capabilities {
                override val sourceFilter = source
                override val binding = owner
            }
        is EntryFilter.Select<*> ->
            object :
                EntryFilter.Select<Any?>(
                    name,
                    values.map {
                        it
                    }.toTypedArray(),
                    state,
                ),
                DetachedEntryFilter,
                EntryFilterMetadataProvider by capabilities,
                EntryFilterValidator by capabilities {
                override val sourceFilter = source
                override val binding = owner
            }
        is EntryFilter.CheckBox ->
            object :
                EntryFilter.CheckBox(name, state),
                DetachedEntryFilter,
                EntryFilterMetadataProvider by capabilities,
                EntryFilterValidator by capabilities {
                override val sourceFilter = source
                override val binding = owner
            }
        is EntryFilter.TriState ->
            object :
                EntryFilter.TriState(name, state),
                DetachedEntryFilter,
                EntryFilterMetadataProvider by capabilities,
                EntryFilterValidator by capabilities {
                override val sourceFilter = source
                override val binding = owner
            }
        is EntryFilter.Sort ->
            object :
                EntryFilter.Sort(name, values.copyOf(), state),
                DetachedEntryFilter,
                EntryFilterMetadataProvider by capabilities,
                EntryFilterValidator by capabilities {
                override val sourceFilter = source
                override val binding = owner
            }
        is EntryFilter.Group<*> ->
            object :
                EntryFilter.Group<EntryFilter<*>>(
                    name,
                    state.filterIsInstance<EntryFilter<*>>().map {
                        it.detachedFilter(owner)
                    },
                ),
                DetachedEntryFilter,
                EntryFilterMetadataProvider by capabilities,
                EntryFilterValidator,
                EntryFilterGroupSummary {
                override val sourceFilter = source
                override val binding = owner
                override fun validateFilter(values: List<EntryFilter<*>>): List<EntryFilterValidationIssue> =
                    (source as? EntryFilterValidator)?.validateFilter(values).orEmpty()
                override fun selectionSummary(values: List<EntryFilter<*>>): String? =
                    (source as? EntryFilterGroupSummary)?.selectionSummary(values)
            }
        is EntryFilter.PagedGroup<*> -> detachedPagedGroup(source, this, owner)
    }
}

/** Resolve optional semantics through a host projection without requiring old extensions to implement anything. */
fun EntryFilter<*>.sourceStateSemantics(): EntryFilterStateSemantics? =
    (((this as? DetachedEntryFilter)?.sourceFilter) ?: this) as? EntryFilterStateSemantics
