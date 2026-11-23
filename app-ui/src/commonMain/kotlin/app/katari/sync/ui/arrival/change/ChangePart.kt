package app.katari.sync.ui.arrival.change

import androidx.compose.runtime.Composable
import dev.icerock.moko.resources.StringResource
import tachiyomi.core.common.preference.TriState
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

/**
 * One thing other devices changed in a setting or a menu, as Changed on other devices tells it. [text] says what
 * happened, naming the first few items of a long list ("Hid MangaDex, Comick and 4 more"); [full] names them all. Text
 * that [readsAlone] says what it changed by itself ("Hid MangaDex"); other text reads after [name] ("Unread: Filtered
 * out"), except on the row of the setting [name] is.
 */
internal class ChangePart(val name: String, val text: String, val full: String, val readsAlone: Boolean)

/** [verb] ("Hid %1$s") said of [items], or null when there are none. */
@Composable
internal fun listChange(
    name: String,
    verb: StringResource,
    items: List<String>,
    readsAlone: Boolean = true,
): ChangePart? {
    if (items.isEmpty()) return null
    val shown = if (items.size > SHOWN_ITEMS + 1) {
        pluralStringResource(
            MR.plurals.sync_arrived_list_more,
            items.size - SHOWN_ITEMS,
            items.take(SHOWN_ITEMS).joinToString(", "),
            items.size - SHOWN_ITEMS,
        )
    } else {
        itemsText(items)
    }
    return ChangePart(name, stringResource(verb, shown), stringResource(verb, itemsText(items)), readsAlone)
}

/** What was added to a set and what was taken out of it, each named by [label]. */
@Composable
internal fun <T> setChanges(
    name: String,
    before: Set<T>,
    after: Set<T>,
    label: @Composable (T) -> String,
    added: StringResource = MR.strings.sync_arrived_added,
    removed: StringResource = MR.strings.sync_arrived_removed,
    readsAlone: Boolean = false,
): List<ChangePart> = listOfNotNull(
    listChange(name, added, (after - before).map { label(it) }, readsAlone),
    listChange(name, removed, (before - after).map { label(it) }, readsAlone),
)

/** A value told before and after, or null when both read alike. */
@Composable
internal fun valueChange(name: String, before: String, after: String): ChangePart? {
    if (before == after) return null
    val text = stringResource(MR.strings.sync_arrived_value_change, before, after)
    return ChangePart(name, text, text, readsAlone = false)
}

@Composable
internal fun switchChange(name: String, before: Boolean, after: Boolean): ChangePart? {
    if (before == after) return null
    val text = stringResource(if (after) MR.strings.sync_arrived_turned_on else MR.strings.sync_arrived_turned_off)
    return ChangePart(name, text, text, readsAlone = false)
}

/**
 * Filters that include, exclude or ignore what they are named by, said by what each now does: "Filtered to Unread",
 * "Filtered out Downloaded", "Stopped filtering by Bookmarked".
 */
@Composable
internal fun filterChanges(name: String, filters: List<FilterChange>): List<ChangePart> {
    val changed = filters.filter { it.before != it.after }
    return listOf(TriState.ENABLED_IS, TriState.ENABLED_NOT, TriState.DISABLED).mapNotNull { state ->
        val verb = when (state) {
            TriState.ENABLED_IS -> MR.strings.sync_arrived_filtered_to
            TriState.ENABLED_NOT -> MR.strings.sync_arrived_filtered_out
            TriState.DISABLED -> MR.strings.sync_arrived_stopped_filtering
        }
        listChange(name, verb, changed.filter { it.after == state }.map { it.label })
    }
}

internal class FilterChange(val label: String, val before: TriState, val after: TriState)

/** "A", "A and B", "A, B and C". */
@Composable
internal fun itemsText(items: List<String>): String = when (items.size) {
    1 -> items.single()
    else -> stringResource(MR.strings.sync_arrived_list_last, items.dropLast(1).joinToString(", "), items.last())
}

/** How many items of a list the short text names before "and N more". */
private const val SHOWN_ITEMS = 2
