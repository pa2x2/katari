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

internal class FilterCapabilities(private val source: EntryFilter<*>) :
    EntryFilterMetadataProvider,
    EntryFilterValueMigration,
    EntryFilterValidator {
    override fun validateFilter(values: List<EntryFilter<*>>): List<EntryFilterValidationIssue> =
        (source as? EntryFilterValidator)?.validateFilter(values).orEmpty()
    override val filterMetadata: EntryFilterMetadata
        get() = (source as? EntryFilterMetadataProvider)?.filterMetadata ?: EntryFilterMetadata()
    override val filterStateVersion: Int
        get() = (source as? EntryFilterValueMigration)?.filterStateVersion ?: 1
    override fun migrateFilterValue(value: String, previousVersion: Int): String? =
        (source as? EntryFilterValueMigration)?.migrateFilterValue(value, previousVersion)
            ?: value.takeIf { previousVersion == 1 && source !is EntryFilterValueMigration }
}
