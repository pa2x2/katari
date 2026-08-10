package mihon.tts.ui.picker.voice

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mihon.language.api.tag.LanguageTag
import mihon.tts.ui.settings.TtsLanguageOption
import tachiyomi.i18n.MR
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
        contentPadding = PaddingValues(16.dp),
    ) {
        item(key = "search") {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                label = { Text(stringResource(MR.strings.action_search)) },
                singleLine = true,
            )
        }
        items(filtered, key = { it.tag.value }) { option ->
            TtsRadioRow(
                title = option.displayName,
                subtitle = option.tag.value,
                selected = option.tag == selected,
                onSelect = { onSelect(option.tag) },
            )
        }
        if (filtered.isEmpty()) {
            item(key = "empty") {
                Text(
                    text = stringResource(
                        if (query.isBlank()) {
                            MR.strings.tts_settings_no_supported_languages
                        } else {
                            MR.strings.no_results_found
                        },
                    ),
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}
