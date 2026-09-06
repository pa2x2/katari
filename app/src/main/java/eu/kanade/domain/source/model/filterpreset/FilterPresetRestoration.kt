package eu.kanade.domain.source.model

import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.source.entry.filter.EntryFilterMetadataProvider
import eu.kanade.tachiyomi.source.entry.filter.EntryFilterValueMigration
import eu.kanade.tachiyomi.source.entry.filter.validationIssues

data class FilterRestoreIssue(val saved: FilterStateNode, val path: List<Int>, val target: EntryFilter<*>? = null)

data class FilterRestoreResult(val issues: List<FilterRestoreIssue>) {
    val isCompatible: Boolean get() = issues.isEmpty()
}

class FilterPresetRepairException(val issues: List<FilterRestoreIssue>) :
    IllegalArgumentException("This preset needs repair: " + issues.joinToString { it.saved.name })

/** Execution callers cannot silently run a partly restored preset. Editors use [restoreSnapshot] for repair. */
fun EntryFilterList.applySnapshot(snapshot: List<FilterStateNode>): EntryFilterList {
    val result = restoreSnapshot(snapshot)
    if (!result.isCompatible) throw FilterPresetRepairException(result.issues)
    return this
}

fun EntryFilterList.restoreSnapshot(snapshot: List<FilterStateNode>): FilterRestoreResult {
    val targets = flattenFilters(this)
    val used = mutableSetOf<List<Int>>()
    val issues = mutableListOf<FilterRestoreIssue>()
    for ((path, saved) in flattenNodes(snapshot)) {
        val identity = saved.identity
        val candidates = if (identity != null) {
            targets.filter { (_, filter) ->
                val metadata = (filter as? EntryFilterMetadataProvider)?.filterMetadata
                metadata?.id == identity.id || metadata?.aliases?.contains(identity.id) == true
            }
        } else {
            val declared = targets.filter { (_, filter) ->
                (filter as? EntryFilterMetadataProvider)?.filterMetadata?.legacyNames?.contains(saved.name) == true
            }
            declared.ifEmpty {
                targets.filter { (targetPath, filter) ->
                    (filter as? EntryFilterMetadataProvider)?.filterMetadata?.id == null &&
                        targetPath == path && filter.name == saved.name
                }
            }
        }
        val match = candidates.singleOrNull()?.takeUnless { it.first in used }
        if (match == null || !restoreValue(match.second, saved)) {
            issues += FilterRestoreIssue(saved, path, match?.second)
        }
        match?.let { used += it.first }
    }
    return FilterRestoreResult(issues)
}

private fun restoreValue(filter: EntryFilter<*>, node: FilterStateNode): Boolean = runCatching {
    val metadata = (filter as? EntryFilterMetadataProvider)?.filterMetadata
    fun option(index: Int?, size: Int): Int? {
        if (index == null) return null
        val id = node.identity?.optionId ?: metadata?.legacyOptionIds?.getOrNull(index)
        if ((node.identity != null || metadata?.id != null) && id == null) return null
        return if (id == null) {
            index.takeIf { it in 0 until size }
        } else {
            val resolved = metadata?.optionAliases?.get(id) ?: id
            metadata?.optionIds?.indexOf(resolved)?.takeIf { it in 0 until size }
        }
    }
    fun encoded(value: String): String? {
        val migration = filter as? EntryFilterValueMigration
        val version = node.identity?.stateVersion ?: 1
        return if (migration == null) value.takeIf { version == 1 } else migration.migrateFilterValue(value, version)
    }
    when {
        filter is EntryFilter.Select<*> && node is FilterStateNode.Select -> {
            filter.state = option(node.state, filter.values.size) ?: return@runCatching false
        }
        filter is EntryFilter.Text && node is FilterStateNode.Text -> {
            filter.state = encoded(node.state) ?: return@runCatching false
        }
        filter is EntryFilter.CheckBox && node is FilterStateNode.CheckBox -> filter.state = node.state
        filter is EntryFilter.TriState && node is FilterStateNode.TriState -> {
            if (node.state !in
                EntryFilter.TriState.STATE_IGNORE..EntryFilter.TriState.STATE_EXCLUDE
            ) {
                return@runCatching false
            }
            filter.state = node.state
        }
        filter is EntryFilter.Sort && node is FilterStateNode.Sort -> {
            filter.state = if (node.index == null && node.ascending == null) {
                null
            } else {
                EntryFilter.Sort.Selection(
                    option(node.index, filter.values.size) ?: return@runCatching false,
                    node.ascending ?: return@runCatching false,
                )
            }
        }
        filter is EntryFilter.PagedGroup<*> && node is FilterStateNode.PagedGroup -> {
            if (!filter.restoreEncodedState(encoded(node.state) ?: return@runCatching false)) return@runCatching false
        }
        else -> return@runCatching false
    }
    filter.validationIssues().isEmpty()
}.getOrDefault(false)

private fun flattenFilters(
    filters: List<EntryFilter<*>>,
    path: List<Int> = emptyList(),
): List<Pair<List<Int>, EntryFilter<*>>> =
    filters.flatMapIndexed { index, filter ->
        val current = path + index
        when (filter) {
            is EntryFilter.Group<*> -> flattenFilters(filter.state.filterIsInstance<EntryFilter<*>>(), current)
            is EntryFilter.Header, is EntryFilter.Separator -> emptyList()
            else -> listOf(current to filter)
        }
    }

private fun flattenNodes(
    nodes: List<FilterStateNode>,
    path: List<Int> = emptyList(),
): List<Pair<List<Int>, FilterStateNode>> =
    nodes.flatMapIndexed { index, node ->
        val current = path + index
        when (node) {
            is FilterStateNode.Group -> flattenNodes(node.state, current)
            is FilterStateNode.Header, is FilterStateNode.Separator -> emptyList()
            else -> listOf(current to node)
        }
    }
