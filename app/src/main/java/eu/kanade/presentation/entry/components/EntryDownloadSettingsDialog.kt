package eu.kanade.presentation.entry.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.AdaptiveSheet
import mihon.entry.interactions.EntryDownloadOptionGroup
import mihon.entry.interactions.EntryDownloadOptionSelection
import mihon.entry.interactions.EntryDownloadOptions
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun EntryDownloadSettingsDialog(
    selectedCount: Int,
    options: EntryDownloadOptions?,
    onDismissRequest: () -> Unit,
    onConfirm: (EntryDownloadOptionSelection) -> Unit,
) {
    var selections by remember(options) {
        mutableStateOf(options?.groups?.associate { it.key to it.selectedKey }.orEmpty())
    }

    AdaptiveSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(MR.strings.action_download), style = MaterialTheme.typography.titleLarge)
            Text(
                text = stringResource(MR.strings.download_apply_to_selected, selectedCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (selectedCount >= 5) {
                Text(
                    text = stringResource(MR.strings.download_storage_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            if (options == null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else {
                options.groups.forEach { group ->
                    DownloadChoiceGroup(
                        group = group,
                        selectedKey = selections[group.key],
                        onSelected = { selected -> selections = selections + (group.key to selected) },
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                Button(onClick = onDismissRequest) {
                    Text(stringResource(MR.strings.action_cancel))
                }
                Button(
                    enabled = isEntryDownloadSelectionValid(options, selections),
                    onClick = { onConfirm(EntryDownloadOptionSelection(selections)) },
                ) {
                    Text(stringResource(MR.strings.action_download))
                }
            }
        }
    }
}

@Composable
private fun DownloadChoiceGroup(
    group: EntryDownloadOptionGroup,
    selectedKey: String?,
    onSelected: (String?) -> Unit,
) {
    if (group.options.isEmpty() && group.defaultLabel == null) return
    Text(group.label, style = MaterialTheme.typography.titleMedium)
    group.defaultLabel?.let { defaultLabel ->
        DownloadChoiceRow(defaultLabel, selectedKey == null) { onSelected(null) }
    }
    group.options.forEach { option ->
        DownloadChoiceRow(option.label, selectedKey == option.key) { onSelected(option.key) }
    }
}

internal fun isEntryDownloadSelectionValid(
    options: EntryDownloadOptions?,
    selections: Map<String, String?>,
): Boolean {
    return options != null && options.groups
        .filter(EntryDownloadOptionGroup::required)
        .all { !selections[it.key].isNullOrBlank() }
}

@Composable
private fun DownloadChoiceRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, modifier = Modifier.padding(start = 8.dp))
    }
}
