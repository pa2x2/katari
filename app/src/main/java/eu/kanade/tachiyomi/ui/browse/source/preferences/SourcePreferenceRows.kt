package eu.kanade.tachiyomi.ui.browse.source.preferences

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.widget.ListPreferenceWidget
import eu.kanade.presentation.more.settings.widget.MultiSelectListPreferenceWidget
import eu.kanade.presentation.more.settings.widget.SwitchPreferenceWidget
import eu.kanade.presentation.more.settings.widget.TextPreferenceWidget
import mihon.presentation.kit.list.SliderRow

/**
 * A source's sections as settings items: a titled section becomes a group with its header and card, rows outside
 * any category share a card at the top level.
 */
internal fun SourcePreferenceHost.settingsItems(sections: List<SourcePreferenceSection>): List<Preference> =
    sections.flatMap { section ->
        val rows = section.nodes.map { node ->
            Preference.PreferenceItem.CustomPreference(title = node.title) { SourcePreferenceRow(node, this) }
        }
        val title = section.title
        if (title == null) rows else listOf(Preference.PreferenceGroup(title = title, preferenceItems = rows))
    }

@Composable
private fun SourcePreferenceRow(node: SourcePreferenceNode, host: SourcePreferenceHost) {
    when (node) {
        is SourcePreferenceNode.Toggle -> SwitchPreferenceWidget(
            title = node.title,
            subtitle = node.summary,
            checked = node.checked,
            enabled = node.enabled,
            onCheckedChanged = { host.toggle(node.preference) },
        )
        is SourcePreferenceNode.Choice -> ListPreferenceWidget(
            value = node.value,
            title = node.title,
            subtitle = node.summary,
            icon = null,
            isProfileSpecific = false,
            entries = node.entries,
            enabled = node.enabled,
            onValueChange = { value -> value?.let { host.choose(node.preference, it) } },
        )
        is SourcePreferenceNode.MultiChoice -> MultiSelectListPreferenceWidget(
            values = node.values,
            title = node.title,
            subtitle = node.summary,
            icon = null,
            entries = node.entries,
            enabled = node.enabled,
            onValuesChange = { host.choose(node.preference, it) },
        )
        is SourcePreferenceNode.Text -> SourceTextPreferenceRow(node, host)
        is SourcePreferenceNode.Slider -> SliderRow(
            title = node.title,
            value = node.value,
            valueRange = node.range,
            onValueChange = { host.slide(node.preference, it) },
            subtitle = node.summary,
            steps = with(node.range) { ((last - first) / step - 1).coerceAtLeast(0) },
            enabled = node.enabled,
        )
        is SourcePreferenceNode.Action -> TextPreferenceWidget(
            title = node.title,
            subtitle = node.summary,
            enabled = node.enabled,
            onPreferenceClick = { host.click(node.preference) }.takeIf { node.selectable },
        )
    }
}

@Composable
private fun SourceTextPreferenceRow(node: SourcePreferenceNode.Text, host: SourcePreferenceHost) {
    val context = LocalContext.current
    // The source's bind listener runs on every opening, as it does for androidx's dialog.
    var input by remember { mutableStateOf<SourceEditTextInput?>(null) }
    TextPreferenceWidget(
        title = node.title,
        subtitle = node.summary,
        enabled = node.enabled,
        onPreferenceClick = { input = node.preference.editTextInput(context) },
    )
    input?.let {
        SourceEditTextDialog(
            title = node.dialogTitle,
            message = node.dialogMessage,
            input = it,
            onDismissRequest = { input = null },
            onConfirm = { text ->
                input = null
                host.enter(node.preference, text)
            },
        )
    }
}
