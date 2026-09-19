package mihon.translation.ui.picker.language

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import mihon.language.api.tag.LanguageTag
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import java.text.Normalizer

@Composable
fun TranslationLanguagePickerList(
    options: List<TranslationLanguageOption>,
    selected: LanguageTag?,
    onSelect: (LanguageTag) -> Unit,
    modifier: Modifier = Modifier,
    defaultOptionLabel: String? = null,
    defaultOptionSupporting: String? = null,
    defaultSelected: Boolean = false,
    onSelectDefault: (() -> Unit)? = null,
    recents: List<TranslationLanguageOption> = emptyList(),
) {
    var query by remember { mutableStateOf("") }
    val normalizedQuery = remember(query) { query.trim().normalizedForSearch() }
    val filtered = remember(options, normalizedQuery) {
        if (normalizedQuery.isEmpty()) {
            options
        } else {
            options.filter { option -> option.matches(normalizedQuery) }
        }
    }
    Column(modifier = modifier) {
        if (recents.isNotEmpty()) {
            TranslationLanguageRecentsRow(
                recents = recents,
                selected = selected,
                onSelect = onSelect,
            )
        }
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
            if (defaultOptionLabel != null && defaultOptionSupporting != null && onSelectDefault != null &&
                normalizedQuery.isEmpty()
            ) {
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
                    supporting = option.supportingText(),
                    selected = option.tag == selected,
                    enabled = true,
                    onClick = { onSelect(option.tag) },
                )
            }
            if (filtered.isEmpty()) {
                item(key = "empty") {
                    Text(
                        text = stringResource(MR.strings.no_results_found),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

/**
 * Search matches the localized name, the language's own native name, and the raw tag, so a query
 * succeeds regardless of which name the user knows the language by. Accents are ignored, so an
 * ASCII query such as "espanol" still finds "Español".
 */
private fun TranslationLanguageOption.matches(normalizedQuery: String): Boolean =
    displayName.normalizedForSearch().contains(normalizedQuery) ||
        nativeName.normalizedForSearch().contains(normalizedQuery) ||
        tag.value.contains(normalizedQuery, ignoreCase = true)

private fun String.normalizedForSearch(): String = Normalizer
    .normalize(this, Normalizer.Form.NFD)
    .replace(diacriticalMarks, "")
    .lowercase()

private val diacriticalMarks = Regex("\\p{Mn}+")

/**
 * Secondary line pairs the language's native name with its tag, replacing the bare tag line so the
 * language can be recognized both by what it calls itself and by its code.
 */
private fun TranslationLanguageOption.supportingText(): String {
    if (nativeName.equals(displayName, ignoreCase = true)) return tag.value
    return "$nativeName · ${tag.value}"
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
                    imageVector = Icons.Filled.Check,
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
