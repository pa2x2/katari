package eu.kanade.tachiyomi.ui.browse.source.preferences

import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceGroup
import androidx.preference.SeekBarPreference
import androidx.preference.TwoStatePreference

/**
 * The visible preferences of [root] in order, split into sections at each nested group. Groups below a category
 * become sections of their own, since a card holds one level of rows. Each node's actions go through [host].
 */
internal fun sourcePreferenceSections(
    root: PreferenceGroup,
    host: SourcePreferenceHost,
): List<SourcePreferenceSection> {
    val sections = mutableListOf<SourcePreferenceSection>()
    fun collect(group: PreferenceGroup, title: String?) {
        var run = mutableListOf<SourcePreferenceNode>()
        fun flush() {
            if (run.isNotEmpty()) sections += SourcePreferenceSection(title, run)
            run = mutableListOf()
        }
        for (index in 0..<group.preferenceCount) {
            val preference = group.getPreference(index)
            if (!preference.isVisible) continue
            if (preference is PreferenceGroup) {
                flush()
                collect(preference, preference.title?.toString()?.takeIf { it.isNotBlank() })
            } else {
                run += preference.toNode(host)
            }
        }
        flush()
    }
    collect(root, null)
    return sections
}

private fun Preference.toNode(host: SourcePreferenceHost): SourcePreferenceNode {
    val title = title?.toString().orEmpty()
    val summary = summary?.toString()?.takeIf { it.isNotEmpty() }
    return when (this) {
        is TwoStatePreference -> SourcePreferenceNode.Toggle(
            title = title,
            // androidx shows the on/off summary in place of the summary when one is set for the current state.
            summary = (if (isChecked) summaryOn else summaryOff)?.toString()?.takeIf { it.isNotEmpty() } ?: summary,
            enabled = isEnabled,
            checked = isChecked,
            toggle = { host.toggle(this) },
        )
        is ListPreference -> SourcePreferenceNode.Choice(
            title = title,
            summary = summary,
            enabled = isEnabled,
            value = value,
            entries = entryMap(entryValues, entries),
            choose = { host.choose(this, it) },
        )
        is MultiSelectListPreference -> SourcePreferenceNode.MultiChoice(
            title = title,
            summary = summary,
            enabled = isEnabled,
            values = values,
            entries = entryMap(entryValues, entries),
            choose = { host.choose(this, it) },
        )
        is EditTextPreference -> SourcePreferenceNode.Text(
            title = title,
            summary = summary,
            enabled = isEnabled,
            dialogTitle = dialogTitle?.toString()?.takeIf { it.isNotBlank() } ?: title,
            dialogMessage = dialogMessage?.toString()?.takeIf { it.isNotBlank() },
            input = { host.editTextInput(this) },
            enter = { host.enter(this, it) },
        )
        is SeekBarPreference -> SourcePreferenceNode.Slider(
            title = title,
            summary = summary,
            enabled = isEnabled,
            value = value,
            range = min..max step seekBarIncrement.coerceAtLeast(1),
            slide = { host.slide(this, it) },
        )
        else -> SourcePreferenceNode.Action(
            title = title,
            summary = summary,
            enabled = isEnabled,
            click = { host.click(this) }.takeIf { isSelectable },
        )
    }
}

private fun entryMap(values: Array<CharSequence>?, labels: Array<CharSequence>?): Map<String, String> {
    if (values == null || labels == null) return emptyMap()
    return values.indices
        .filter { it < labels.size }
        .associate { values[it].toString() to labels[it].toString() }
}
