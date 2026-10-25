package eu.kanade.presentation.entry.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mihon.entry.interactions.download.EntryDownloadOptionGroup
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.dialog.DialogAction
import mihon.presentation.kit.list.ChoiceRow
import mihon.presentation.kit.list.RowGroup
import mihon.presentation.kit.list.RowGroupHeader
import mihon.presentation.kit.list.RowGroupNote
import mihon.presentation.kit.progress.LoadingIndicator
import mihon.presentation.kit.sheet.Sheet
import mihon.presentation.kit.sheet.SheetActions
import mihon.presentation.kit.sheet.SheetHeader
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun EntryDownloadSettingsDialog(
    selectedCount: Int,
    choices: EntryDownloadChoices?,
    onSelect: (groupKey: String, optionKey: String?) -> Unit,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    Sheet(onDismissRequest = onDismissRequest) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SheetHeader(title = stringResource(MR.strings.action_download))
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = KatariTheme.spacing.s),
            ) {
                RowGroupNote(stringResource(MR.strings.download_apply_to_selected, selectedCount))
                if (selectedCount >= 5) {
                    RowGroupNote(stringResource(MR.strings.download_storage_warning), tone = ContentTone.Error)
                }

                choices?.shownGroups?.forEach { group ->
                    DownloadChoiceGroup(
                        group = group,
                        selectedKey = choices.selections[group.key],
                        onSelected = { selected -> onSelect(group.key, selected) },
                    )
                }
                if (choices?.reload?.failed == true) {
                    RowGroupNote(stringResource(MR.strings.download_options_failed), tone = ContentTone.Error)
                } else if (choices == null || choices.reload != null) {
                    LoadingIndicator(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(KatariTheme.spacing.l),
                    )
                }
            }

            SheetActions(
                confirm = DialogAction(
                    label = stringResource(MR.strings.action_download),
                    onClick = onConfirm,
                    enabled = choices?.canDownload == true,
                ),
                dismiss = DialogAction(stringResource(MR.strings.action_cancel), onDismissRequest),
            )
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
    // Picking the automatic option again makes it the group's own pick.
    val shownKey = selectedKey ?: group.automaticKey
    RowGroupHeader(text = group.label)
    RowGroup {
        group.defaultLabel?.let { defaultLabel ->
            row {
                ChoiceRow(title = defaultLabel, selected = shownKey == null, onClick = { onSelected(null) })
            }
        }
        group.options.forEach { option ->
            row(key = option.key) {
                ChoiceRow(
                    title = option.label,
                    selected = shownKey == option.key,
                    onClick = { onSelected(option.key) },
                )
            }
        }
    }
}
