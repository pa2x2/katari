package eu.kanade.tachiyomi.source.entry.filter

import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.EntryFilterList

/** Reject ambiguous opted-in identities before they can overwrite persisted selections. */
internal fun EntryFilterList.identityIssues(): List<EntryFilterValidationIssue> {
    val owners = mutableMapOf<String, EntryFilter<*>>()
    val issues = mutableListOf<EntryFilterValidationIssue>()
    fun invalid(filter: EntryFilter<*>) {
        val id = (filter as? EntryFilterMetadataProvider)?.filterMetadata?.id
        issues +=
            EntryFilterValidationIssue(
                setOfNotNull(id),
                EntryFilterValidationCode.SOURCE,
                "Ambiguous filter identity: ${filter.name}",
            )
    }
    fun visit(filter: EntryFilter<*>) {
        val metadata = (filter as? EntryFilterMetadataProvider)?.filterMetadata
        metadata?.id?.let { id ->
            (metadata.aliases + id).forEach { key ->
                if (key.isBlank() || (owners.put(key, filter)?.let { it !== filter } == true)) invalid(filter)
            }
            val optionCount = when (filter) {
                is EntryFilter.Select<*> -> filter.values.size
                is EntryFilter.Sort -> filter.values.size
                else -> null
            }
            if (optionCount != null &&
                (
                    metadata.optionIds.size != optionCount || metadata.optionIds.any { it.isBlank() } ||
                        metadata.optionIds.distinct().size != optionCount
                    )
            ) {
                invalid(filter)
            }
            if (filter is EntryFilter.Select<*> && filter.state !in filter.values.indices) invalid(filter)
            if (filter is EntryFilter.Sort && filter.state?.index?.let { it !in filter.values.indices } == true) {
                invalid(filter)
            }
            if (metadata.neutralOptionId != null && metadata.neutralOptionId !in metadata.optionIds) invalid(filter)
            if (metadata.optionAliases.any { (alias, target) ->
                    alias.isBlank() || target !in metadata.optionIds ||
                        (alias in metadata.optionIds && alias != target)
                }
            ) {
                invalid(filter)
            }
        }
        (filter as? EntryFilter.Group<*>)?.state?.filterIsInstance<EntryFilter<*>>()?.forEach(::visit)
    }
    forEach(::visit)
    return issues.distinct()
}
