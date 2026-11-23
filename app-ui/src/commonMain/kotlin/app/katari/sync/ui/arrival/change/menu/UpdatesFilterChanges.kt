package app.katari.sync.ui.arrival.change.menu

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import app.katari.sync.ui.arrival.change.ChangeLookups
import app.katari.sync.ui.arrival.change.ChangePart
import app.katari.sync.ui.arrival.change.FilterChange
import app.katari.sync.ui.arrival.change.KeyChange
import app.katari.sync.ui.arrival.change.filterChanges
import eu.kanade.presentation.category.visualName
import eu.kanade.presentation.entry.entryTypePresentation
import eu.kanade.presentation.updates.unifiedUpdatesFilterOptions
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.TriState
import tachiyomi.domain.updates.service.UpdatesPreferences
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The Updates filter, tab by tab as its sheet shows it: the filters, then the categories, types and sources each
 * included, excluded or left alone.
 */
@Composable
internal fun updatesFilterChanges(changes: List<KeyChange>, lookups: ChangeLookups): List<ChangePart> {
    val filtered = changes.flatMap { change ->
        unifiedUpdatesFilterOptions().mapNotNull { option ->
            change.of { option.preference(UpdatesPreferences(it)) }
                ?.let { FilterChange(stringResource(option.label), it.before, it.after) }
        }
    }
    val profileId = changes.first().profileId
    val categories by remember(lookups, profileId) { lookups.categories(profileId) }.collectAsState(emptyList())
    val categoryNames = categories.associate { it.id to it.visualName }
    return filterChanges(stringResource(MR.strings.action_filter), filtered) +
        filterChanges(
            stringResource(MR.strings.categories),
            itemFilters(
                changes,
                UpdatesPreferences::filterIncludedCategories,
                UpdatesPreferences::filterExcludedCategories,
            )
                .mapNotNull { (id, before, after) -> categoryNames[id]?.let { FilterChange(it, before, after) } },
        ) +
        filterChanges(
            stringResource(MR.strings.library_updates_types),
            itemFilters(
                changes,
                UpdatesPreferences::filterIncludedEntryTypes,
                UpdatesPreferences::filterExcludedEntryTypes,
            )
                .map { (type, before, after) ->
                    FilterChange(stringResource(type.entryTypePresentation().displayNameLabel), before, after)
                },
        ) +
        filterChanges(
            stringResource(MR.strings.label_sources),
            itemFilters(changes, UpdatesPreferences::filterIncludedSources, UpdatesPreferences::filterExcludedSources)
                .map { (sourceKey, before, after) -> FilterChange(lookups.sourceName(sourceKey), before, after) },
        )
}

/**
 * Each item an included and an excluded list hold, with whether it was included, excluded or neither before and after.
 * A list the changes leave alone counts as empty: an item that moves from one list to the other changes both.
 */
private fun <T, C : Collection<T>> itemFilters(
    changes: List<KeyChange>,
    included: (UpdatesPreferences) -> Preference<C>,
    excluded: (UpdatesPreferences) -> Preference<C>,
): List<Triple<T, TriState, TriState>> {
    val ins = changes.firstNotNullOfOrNull { change -> change.of { included(UpdatesPreferences(it)) } }
    val outs = changes.firstNotNullOfOrNull { change -> change.of { excluded(UpdatesPreferences(it)) } }
    if (ins == null && outs == null) return emptyList()
    fun state(item: T, isIn: Collection<T>?, isOut: Collection<T>?) = when {
        isIn != null && item in isIn -> TriState.ENABLED_IS
        isOut != null && item in isOut -> TriState.ENABLED_NOT
        else -> TriState.DISABLED
    }
    val items = listOfNotNull(ins?.before, ins?.after, outs?.before, outs?.after).flatten().distinct()
    return items.map { item ->
        Triple(item, state(item, ins?.before, outs?.before), state(item, ins?.after, outs?.after))
    }
}
