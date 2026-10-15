package mihon.tts.ui.picker.voice

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import mihon.presentation.kit.theme.KatariTheme
import mihon.tts.ui.settings.TtsLanguageOption
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun TtsLanguagePickerList(
    options: List<TtsLanguageOption>,
    selected: LanguageTag?,
    onSelect: (LanguageTag) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(options, query) {
        options.filter { option ->
            query.isBlank() || option.displayName.contains(query, ignoreCase = true) ||
                option.tag.value.contains(query, ignoreCase = true)
        }
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = KatariTheme.spacing.s),
    ) {
        item(key = "search") {
            SearchField(
                query = query,
                onQueryChange = { query = it },
                placeholder = stringResource(MR.strings.action_search),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = KatariTheme.spacing.rowInset, vertical = KatariTheme.spacing.s),
            )
        }
        items(filtered, key = { it.tag.value }) { option ->
            ChoiceRow(
                title = option.displayName,
                selected = option.tag == selected,
                onClick = { onSelect(option.tag) },
                subtitle = option.tag.value,
            )
        }
        if (filtered.isEmpty()) {
            item(key = "empty") {
                NoResults(
                    message = stringResource(
                        if (query.isBlank()) {
                            MR.strings.tts_settings_no_supported_languages
                        } else {
                            MR.strings.no_results_found
                        },
                    ),
                )
            }
        }
    }
}
