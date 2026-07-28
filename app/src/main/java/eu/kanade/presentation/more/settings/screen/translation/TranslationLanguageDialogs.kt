package eu.kanade.presentation.more.settings.screen.translation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mihon.translation.api.TranslationLanguageTag
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun TranslationLanguagePickerDialog(
    title: String,
    options: List<TranslationLanguageOption>,
    selected: TranslationLanguageTag?,
    includeAppLanguage: Boolean,
    appLanguageLabel: String,
    onSelect: (TranslationLanguageTag?) -> Unit,
    onDismissRequest: () -> Unit,
) {
    var query by remember(title) { mutableStateOf("") }
    val filtered = remember(options, query) {
        val normalized = query.trim()
        if (normalized.isEmpty()) {
            options
        } else {
            options.filter { option ->
                option.displayName.contains(normalized, ignoreCase = true) ||
                    option.tag.value.contains(normalized, ignoreCase = true)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(MR.strings.translation_settings_language_search)) },
                    singleLine = true,
                )
                LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                    if (includeAppLanguage) {
                        item(key = "app-language") {
                            TranslationLanguageRow(
                                label = appLanguageLabel,
                                selected = selected == null,
                                onClick = { onSelect(null) },
                            )
                        }
                    }
                    items(
                        items = filtered,
                        key = { it.tag.value },
                    ) { option ->
                        TranslationLanguageRow(
                            label = option.displayName,
                            supporting = option.tag.value,
                            selected = option.tag == selected,
                            onClick = { onSelect(option.tag) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(MR.strings.action_cancel))
            }
        },
    )
}

@Composable
private fun TranslationLanguageRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    supporting: String? = null,
) {
    ListItem(
        headlineContent = { Text(label) },
        supportingContent = supporting?.let { value -> { Text(value) } },
        trailingContent = if (selected) {
            {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        } else {
            null
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
internal fun TranslationLanguagePairDialog(
    source: TranslationLanguageTag,
    target: TranslationLanguageTag,
    onChooseSource: () -> Unit,
    onChooseTarget: () -> Unit,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(MR.strings.action_change_language)) },
        text = {
            Column {
                ListItem(
                    headlineContent = { Text(stringResource(MR.strings.translation_choose_source_language)) },
                    supportingContent = { Text(source.displayName()) },
                    modifier = Modifier.clickable(onClick = onChooseSource),
                )
                ListItem(
                    headlineContent = { Text(stringResource(MR.strings.translation_choose_target_language)) },
                    supportingContent = { Text(target.displayName()) },
                    modifier = Modifier.clickable(onClick = onChooseTarget),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = source != target,
            ) {
                Text(stringResource(MR.strings.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(MR.strings.action_cancel))
            }
        },
    )
}
