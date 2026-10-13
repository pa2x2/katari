package mihon.tts.ui.picker.voice

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Close
import mihon.icons.materialsymbols.rounded.Search
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun TtsVoiceSearchField(onSearchQueryChange: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    val currentOnSearchQueryChange by rememberUpdatedState(onSearchQueryChange)
    LaunchedEffect(query) {
        delay(SEARCH_DEBOUNCE_MILLIS)
        currentOnSearchQueryChange(query)
    }

    OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        label = { Text(stringResource(MR.strings.tts_settings_search_voices)) },
        leadingIcon = { Icon(MaterialSymbols.Rounded.Search, contentDescription = null) },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = { query = "" }) {
                    Icon(
                        imageVector = MaterialSymbols.Rounded.Close,
                        contentDescription = stringResource(MR.strings.tts_settings_clear_voice_search),
                    )
                }
            }
        } else {
            null
        },
        singleLine = true,
    )
}

private const val SEARCH_DEBOUNCE_MILLIS = 100L
