package mihon.translation.ui.picker.language

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import mihon.language.api.tag.LanguageTag
import mihon.presentation.kit.feedback.NoResults
import mihon.presentation.kit.field.SearchField
import mihon.presentation.kit.list.ChoiceRow
import mihon.presentation.kit.list.RowGroupGap
import mihon.presentation.kit.list.RowGroupHeader
import mihon.presentation.kit.list.RowGroupItem
import mihon.presentation.kit.list.RowGroupPosition
import mihon.presentation.kit.list.rowGroupItems
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import mihon.translation.ui.presentation.language.TranslationDirectionText
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

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
    shortcuts: List<TranslationLanguageOption> = emptyList(),
    shortcutsTitle: String = stringResource(MR.strings.translation_recent_languages),
    unpairable: TranslationUnpairableLanguages? = null,
) {
    var query by remember { mutableStateOf("") }
    val normalizedQuery = remember(query) { query.trim().normalizedForSearch() }
    val filtered = remember(options, normalizedQuery) { options.matching(normalizedQuery) }
    val filteredUnpairable = remember(unpairable, normalizedQuery) {
        unpairable?.options.orEmpty().matching(normalizedQuery)
    }
    Column(modifier = modifier) {
        if (shortcuts.isNotEmpty()) {
            TranslationLanguageShortcutsRow(
                title = shortcutsTitle,
                shortcuts = shortcuts,
                selected = selected,
                onSelect = onSelect,
            )
        }
        SearchField(
            query = query,
            onQueryChange = { query = it },
            placeholder = stringResource(MR.strings.translation_settings_language_search),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = KatariTheme.spacing.m, vertical = KatariTheme.spacing.s),
        )
        LazyColumn(contentPadding = PaddingValues(bottom = KatariTheme.spacing.m)) {
            // The default stands apart from the languages it can resolve to.
            if (defaultOption != null && normalizedQuery.isEmpty()) {
                item(key = "default") {
                    RowGroupItem(position = RowGroupPosition.Alone) {
                        LanguageRow(
                            label = defaultOption.label,
                            supporting = defaultOption.supporting,
                            selected = defaultOption.selected,
                            enabled = true,
                            onClick = onSelectDefault,
                        )
                    }
                }
                item(key = "default-gap") { RowGroupGap() }
            }
            rowGroupItems(filtered, key = { it.tag.value }) { option ->
                LanguageRow(
                    label = option.displayName,
                    supporting = option.supportingText(),
                    selected = option.tag == selected,
                    enabled = true,
                    onClick = { onSelect(option.tag) },
                )
            }
            if (unpairable != null && filteredUnpairable.isNotEmpty()) {
                if (filtered.isNotEmpty()) item(key = "unpairable-gap") { RowGroupGap() }
                item(key = "unpairable") {
                    RowGroupHeader(
                        stringResource(MR.strings.translation_languages_unpairable, unpairable.source.displayName()),
                    )
                }
                rowGroupItems(filteredUnpairable, key = { "unpairable:${it.tag.value}" }) { option ->
                    LanguageRow(
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
                    NoResults()
                }
            }
        }
    }
}

/**
 * Secondary line pairs the language's native name with its tag, replacing the bare tag line so the
 * language can be recognized both by what it calls itself and by its code.
 */
private fun TranslationLanguageOption.supportingText(): String {
    if (nativeName.equals(displayName, ignoreCase = true)) return tag.value
    return "$nativeName · ${tag.value}"
}

// The supporting line goes below rather than as the row's subtitle so its direction arrows are drawn as icons.
@Composable
private fun LanguageRow(
    label: String,
    supporting: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    ChoiceRow(
        title = label,
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        below = { TranslationDirectionText(supporting, role = TextRole.Supporting) },
    )
}
