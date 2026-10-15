package mihon.tts.ui.picker.voice

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import mihon.presentation.kit.field.SearchField
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Voice search that reports its query only after typing pauses, since every change regroups the whole catalog. */
@Composable
internal fun TtsVoiceSearchField(onSearchQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    var query by remember { mutableStateOf("") }
    val currentOnSearchQueryChange by rememberUpdatedState(onSearchQueryChange)
    LaunchedEffect(query) {
        delay(SEARCH_DEBOUNCE_MILLIS)
        currentOnSearchQueryChange(query)
    }

    SearchField(
        query = query,
        onQueryChange = { query = it },
        placeholder = stringResource(MR.strings.tts_settings_search_voices),
        modifier = modifier,
    )
}

private const val SEARCH_DEBOUNCE_MILLIS = 100L
