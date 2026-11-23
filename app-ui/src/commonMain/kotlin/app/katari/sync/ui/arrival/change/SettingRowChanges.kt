package app.katari.sync.ui.arrival.change

import androidx.compose.runtime.Composable
import app.katari.sync.client.arrival.ArrivedChange
import eu.kanade.presentation.more.settings.Preference

/**
 * What other devices changed in a setting a Settings row shows, told with the row's own labels and value texts. Values
 * come as stores hold them, so an enum entry matches its stored name; a missing value reads as the row's default. Null
 * when the row is not one whose values this can read.
 */
@Composable
internal fun settingRowChanges(row: Preference.PreferenceItem<*, *>, change: ArrivedChange): List<ChangePart>? {
    val default = row.storedPreference?.defaultValue()
    val before = change.before ?: default
    val after = change.after ?: default
    val part = when (row) {
        is Preference.PreferenceItem.SwitchPreference ->
            switchChange(row.title, before as? Boolean ?: return null, after as? Boolean ?: return null)
        is Preference.PreferenceItem.SliderPreference -> valueChange(
            row.title,
            row.valueText(before as? Int ?: return null),
            row.valueText(after as? Int ?: return null),
        )
        is Preference.PreferenceItem.StepperPreference -> valueChange(
            row.title,
            row.valueFormatter(before as? Int ?: return null),
            row.valueFormatter(after as? Int ?: return null),
        )
        is Preference.PreferenceItem.ListPreference<*> -> valueChange(
            row.title,
            row.entries.toMap().labelOf(before) ?: return null,
            row.entries.toMap().labelOf(after) ?: return null,
        )
        is Preference.PreferenceItem.EditTextPreference ->
            valueChange(row.title, before as? String ?: return null, after as? String ?: return null)
        is Preference.PreferenceItem.MultiSelectListPreference<*> -> {
            val entries = row.entries.toMap()
            return setChanges(
                name = row.title,
                before = (before as? Set<*>)?.mapNotNull(entries::keyOf)?.toSet() ?: return null,
                after = (after as? Set<*>)?.mapNotNull(entries::keyOf)?.toSet() ?: return null,
                label = { entries.getValue(it) },
            )
        }
        else -> return null
    }
    return listOfNotNull(part)
}

private fun Map<Any?, String>.labelOf(value: Any?): String? = keyOf(value)?.let(::get)

/** The entry [value] is, given as the entry itself or in its stored form: an enum by its name. */
private fun Map<Any?, String>.keyOf(value: Any?): Any? =
    keys.firstOrNull { it == value || (it is Enum<*> && it.name == value) }
