package mihon.translation.ui.picker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import mihon.translation.api.TranslationLanguageTag
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun TranslationLanguagePickerList(
    options: List<TranslationLanguageOption>,
    selected: TranslationLanguageTag?,
    onSelect: (TranslationLanguageTag) -> Unit,
    modifier: Modifier = Modifier,
    defaultOptionLabel: String? = null,
    defaultOptionSupporting: String? = null,
    defaultSelected: Boolean = false,
    onSelectDefault: (() -> Unit)? = null,
) {
    var query by remember { mutableStateOf("") }
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
    Column(modifier = modifier) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            label = { Text(stringResource(MR.strings.translation_settings_language_search)) },
            singleLine = true,
        )
        LazyColumn {
            if (defaultOptionLabel != null && defaultOptionSupporting != null && onSelectDefault != null) {
                item(key = "default") {
                    TranslationPickerRow(
                        label = defaultOptionLabel,
                        supporting = defaultOptionSupporting,
                        selected = defaultSelected,
                        enabled = true,
                        onClick = onSelectDefault,
                    )
                }
            }
            items(filtered, key = { it.tag.value }) { option ->
                TranslationPickerRow(
                    label = option.displayName,
                    supporting = option.tag.value,
                    selected = option.tag == selected,
                    enabled = true,
                    onClick = { onSelect(option.tag) },
                )
            }
        }
    }
}

@Composable
private fun TranslationPickerRow(
    label: String,
    supporting: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    ListItem(
        supportingContent = { Text(supporting) },
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
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick),
        content = { Text(label) },
    )
}
