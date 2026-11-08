package app.katari.tts.ui.picker.voice

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.katari.language.api.tag.LanguageTag
import app.katari.presentation.kit.feedback.NoResults
import app.katari.presentation.kit.field.SearchField
import app.katari.presentation.kit.layout.RowList
import app.katari.presentation.kit.list.ChoiceRow
import app.katari.presentation.kit.list.rowGroupItems
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.tts.ui.settings.TtsLanguageOption
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
    RowList(
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
                    .padding(horizontal = KatariTheme.spacing.m, vertical = KatariTheme.spacing.s),
            )
        }
        rowGroupItems(filtered, key = { it.tag.value }) { option ->
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
