package mihon.tts.ui.picker.voice

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import mihon.language.api.tag.LanguageTag
import mihon.tts.api.voice.TtsVoice
import mihon.tts.api.voice.TtsVoiceId
import mihon.tts.ui.settings.displayName
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun TtsVoiceOverridesList(
    overrides: Map<LanguageTag, TtsVoiceId>,
    voices: List<TtsVoice>,
    onEdit: (LanguageTag) -> Unit,
    onDelete: (LanguageTag) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "add") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onAdd)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(stringResource(MR.strings.tts_settings_add_language))
            }
        }
        items(
            overrides.entries.sortedBy { it.key.displayName() },
            key = { it.key.value },
        ) { (language, selectedVoice) ->
            val voice = voices.singleOrNull { it.id == selectedVoice }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEdit(language) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(language.displayName(), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        voice?.name ?: stringResource(MR.strings.tts_settings_voice_unavailable),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (voice == null) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                TextButton(onClick = { onDelete(language) }) {
                    Text(stringResource(MR.strings.action_delete))
                }
            }
        }
    }
}
