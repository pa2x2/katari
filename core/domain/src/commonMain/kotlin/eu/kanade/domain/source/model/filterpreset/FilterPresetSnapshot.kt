package eu.kanade.domain.source.model

import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.source.entry.filter.EntryFilterMetadataProvider
import eu.kanade.tachiyomi.source.entry.filter.EntryFilterValueMigration

fun EntryFilterList.snapshot(): List<FilterStateNode> = map { it.snapshotNode() }

private fun EntryFilter<*>.snapshotNode(): FilterStateNode {
    val metadata = (this as? EntryFilterMetadataProvider)?.filterMetadata
    val optionIndex = when (this) {
        is EntryFilter.Select<*> -> state
        is EntryFilter.Sort -> state?.index
        else -> null
    }
    val identity = metadata?.id?.let {
        FilterIdentity(
            it,
            optionIndex?.let(metadata.optionIds::getOrNull),
            (this as? EntryFilterValueMigration)?.filterStateVersion ?: 1,
        )
    }
    return when (this) {
        is EntryFilter.Header -> FilterStateNode.Header(name)
        is EntryFilter.Separator -> FilterStateNode.Separator(name)
        is EntryFilter.Select<*> -> FilterStateNode.Select(name, state, identity)
        is EntryFilter.Text -> FilterStateNode.Text(name, state, identity)
        is EntryFilter.CheckBox -> FilterStateNode.CheckBox(name, state, identity)
        is EntryFilter.TriState -> FilterStateNode.TriState(name, state, identity)
        is EntryFilter.Sort -> FilterStateNode.Sort(name, state?.index, state?.ascending, identity)
        is EntryFilter.PagedGroup<*> -> FilterStateNode.PagedGroup(name, encodeCurrentState(), identity)
        is EntryFilter.Group<*> -> FilterStateNode.Group(
            name,
            state.filterIsInstance<EntryFilter<*>>().map {
                it.snapshotNode()
            },
            identity,
        )
    }
}
