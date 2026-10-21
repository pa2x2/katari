package eu.kanade.tachiyomi.ui.browse.source.preferences

import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceGroup
import androidx.preference.SeekBarPreference
import androidx.preference.TwoStatePreference

/**
 * What one of a source's preferences shows, read from the androidx [Preference] the source built. [preference] stays
 * the object every change goes through, so the source's listeners, dependencies and persistence apply as they did in
 * androidx's own screen.
 */
internal sealed interface SourcePreferenceNode {
    val preference: Preference
    val title: String
    val summary: String?
    val enabled: Boolean

    data class Toggle(
        override val preference: TwoStatePreference,
        override val title: String,
        override val summary: String?,
        override val enabled: Boolean,
        val checked: Boolean,
    ) : SourcePreferenceNode

    data class Choice(
        override val preference: ListPreference,
        override val title: String,
        override val summary: String?,
        override val enabled: Boolean,
        val value: String?,
        val entries: Map<String, String>,
    ) : SourcePreferenceNode

    data class MultiChoice(
        override val preference: MultiSelectListPreference,
        override val title: String,
        override val summary: String?,
        override val enabled: Boolean,
        val values: Set<String>,
        val entries: Map<String, String>,
    ) : SourcePreferenceNode

    data class Text(
        override val preference: EditTextPreference,
        override val title: String,
        override val summary: String?,
        override val enabled: Boolean,
        val dialogTitle: String,
        val dialogMessage: String?,
    ) : SourcePreferenceNode

    data class Slider(
        override val preference: SeekBarPreference,
        override val title: String,
        override val summary: String?,
        override val enabled: Boolean,
        val value: Int,
        val range: IntProgression,
    ) : SourcePreferenceNode

    /** Any other preference: a row that runs the preference's own click handling when it can be selected. */
    data class Action(
        override val preference: Preference,
        override val title: String,
        override val summary: String?,
        override val enabled: Boolean,
        val selectable: Boolean,
    ) : SourcePreferenceNode
}

/** A run of preferences under a category; a null [title] is a run outside any category. */
internal data class SourcePreferenceSection(
    val title: String?,
    val nodes: List<SourcePreferenceNode>,
)

/**
 * The visible preferences of [root] in order, split into sections at each nested group. Groups below a category
 * become sections of their own, since a card holds one level of rows.
 */
internal fun sourcePreferenceSections(root: PreferenceGroup): List<SourcePreferenceSection> {
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
                run += preference.toNode()
            }
        }
        flush()
    }
    collect(root, null)
    return sections
}

private fun Preference.toNode(): SourcePreferenceNode {
    val title = title?.toString().orEmpty()
    val summary = summary?.toString()?.takeIf { it.isNotEmpty() }
    return when (this) {
        is TwoStatePreference -> SourcePreferenceNode.Toggle(
            preference = this,
            title = title,
            // androidx shows the on/off summary in place of the summary when one is set for the current state.
            summary = (if (isChecked) summaryOn else summaryOff)?.toString()?.takeIf { it.isNotEmpty() } ?: summary,
            enabled = isEnabled,
            checked = isChecked,
        )
        is ListPreference -> SourcePreferenceNode.Choice(
            preference = this,
            title = title,
            summary = summary,
            enabled = isEnabled,
            value = value,
            entries = entryMap(entryValues, entries),
        )
        is MultiSelectListPreference -> SourcePreferenceNode.MultiChoice(
            preference = this,
            title = title,
            summary = summary,
            enabled = isEnabled,
            values = values,
            entries = entryMap(entryValues, entries),
        )
        is EditTextPreference -> SourcePreferenceNode.Text(
            preference = this,
            title = title,
            summary = summary,
            enabled = isEnabled,
            dialogTitle = dialogTitle?.toString()?.takeIf { it.isNotBlank() } ?: title,
            dialogMessage = dialogMessage?.toString()?.takeIf { it.isNotBlank() },
        )
        is SeekBarPreference -> SourcePreferenceNode.Slider(
            preference = this,
            title = title,
            summary = summary,
            enabled = isEnabled,
            value = value,
            range = min..max step seekBarIncrement.coerceAtLeast(1),
        )
        else -> SourcePreferenceNode.Action(
            preference = this,
            title = title,
            summary = summary,
            enabled = isEnabled,
            selectable = isSelectable,
        )
    }
}

private fun entryMap(values: Array<CharSequence>?, labels: Array<CharSequence>?): Map<String, String> {
    if (values == null || labels == null) return emptyMap()
    return values.indices
        .filter { it < labels.size }
        .associate { values[it].toString() to labels[it].toString() }
}
