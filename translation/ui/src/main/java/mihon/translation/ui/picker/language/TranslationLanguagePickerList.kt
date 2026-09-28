package mihon.translation.ui.picker.language

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
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
import mihon.translation.ui.presentation.language.TranslationDirectionText
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import java.text.Normalizer

/**
 * A row listed before the languages that follows a default instead of pinning a language, such as the profile's
 * target language or detecting the source.
 */
data class TranslationLanguageDefaultOption(
    val label: String,
    val supporting: String,
    val selected: Boolean,
)

/**
 * Languages listed after the selectable ones, disabled, because [engineName] cannot translate into them from
 * [source].
 */
data class TranslationUnpairableLanguages(
    val source: LanguageTag,
    val engineName: String,
    val options: List<TranslationLanguageOption>,
)

@Composable
fun TranslationLanguagePickerList(
    options: List<TranslationLanguageOption>,
    selected: LanguageTag?,
    onSelect: (LanguageTag) -> Unit,
    modifier: Modifier = Modifier,
    defaultOption: TranslationLanguageDefaultOption? = null,
    onSelectDefault: () -> Unit = {},
    recents: List<TranslationLanguageOption> = emptyList(),
    unpairable: TranslationUnpairableLanguages? = null,
) {
    var query by remember { mutableStateOf("") }
    val normalizedQuery = remember(query) { query.trim().normalizedForSearch() }
    val filtered = remember(options, normalizedQuery) { options.matching(normalizedQuery) }
    val filteredUnpairable = remember(unpairable, normalizedQuery) {
        unpairable?.options.orEmpty().matching(normalizedQuery)
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
            if (defaultOption != null && normalizedQuery.isEmpty()) {
                item(key = "default") {
                    TranslationPickerRow(
                        label = defaultOption.label,
                        supporting = defaultOption.supporting,
                        selected = defaultOption.selected,
                        enabled = true,
                        onClick = onSelectDefault,
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
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
            if (unpairable != null && filteredUnpairable.isNotEmpty()) {
                item(key = "unpairable") {
                    Text(
                        text = stringResource(
                            MR.strings.translation_languages_unpairable,
                            unpairable.source.displayName(),
                        ),
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                items(filteredUnpairable, key = { "unpairable:${it.tag.value}" }) { option ->
                    TranslationPickerRow(
                        label = option.displayName,
                        supporting = stringResource(
                            MR.strings.translation_language_unpairable_reason,
                            unpairable.engineName,
                            unpairable.source.displayName(),
                            option.displayName,
                        ),
                        selected = false,
                        enabled = false,
                        onClick = {},
                    )
                }
            }
            if (filtered.isEmpty() && filteredUnpairable.isEmpty()) {
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

private fun List<TranslationLanguageOption>.matching(normalizedQuery: String): List<TranslationLanguageOption> =
    if (normalizedQuery.isEmpty()) this else filter { option -> option.matches(normalizedQuery) }

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
        enabled = enabled,
        supportingContent = { TranslationDirectionText(supporting) },
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
