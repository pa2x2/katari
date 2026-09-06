package eu.kanade.domain.source.model

import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.filter.EntryFilterMetadata
import eu.kanade.tachiyomi.source.entry.filter.EntryFilterMetadataProvider

internal class PresetChoice(
    name: String = "Type",
    id: String = "type",
    options: List<String> = listOf("any", "movie", "tv"),
    aliases: Set<String> = emptySet(),
    legacyNames: Set<String> = emptySet(),
    legacyOptions: List<String> = emptyList(),
    optionAliases: Map<String, String> = emptyMap(),
) : EntryFilter.Select<String>(name, options.toTypedArray()), EntryFilterMetadataProvider {
    override val filterMetadata =
        EntryFilterMetadata(
            id = id,
            optionIds = options,
            aliases = aliases,
            legacyNames = legacyNames,
            legacyOptionIds = legacyOptions,
            optionAliases = optionAliases,
        )
}

internal fun presetGroup(name: String, vararg children: EntryFilter<*>): EntryFilter.Group<EntryFilter<*>> =
    object : EntryFilter.Group<EntryFilter<*>>(name, children.toList()) {}
