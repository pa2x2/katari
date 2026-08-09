package mihon.tts.ui.picker.voice

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import mihon.tts.api.provider.TtsVoiceProcessing
import mihon.tts.api.voice.TtsDefaultVoiceSelection
import mihon.tts.api.voice.TtsVoice
import mihon.tts.api.voice.TtsVoiceId
import mihon.tts.ui.settings.compatibleWith
import mihon.tts.ui.settings.displayName
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import java.util.Locale

@Composable
fun TtsVoicePickerList(
    voices: List<TtsVoice>,
    selected: TtsDefaultVoiceSelection?,
    previewingVoice: TtsVoiceId?,
    onSelect: (TtsDefaultVoiceSelection, TtsVoice) -> Unit,
    modifier: Modifier = Modifier,
    language: LanguageTag? = null,
    engineDefaultVoice: TtsVoiceId? = null,
) {
    var query by remember { mutableStateOf("") }
    val locale = Locale.getDefault()
    val compatible = remember(voices, language) {
        language?.let(voices::compatibleWith) ?: voices.sortedWith(voiceOrder(locale))
    }
    val engineDefault = remember(voices, engineDefaultVoice) {
        voices.singleOrNull { it.id == engineDefaultVoice }
    }
    val selectedVoiceId = when (selected) {
        TtsDefaultVoiceSelection.EngineDefault -> engineDefaultVoice
        is TtsDefaultVoiceSelection.Explicit -> selected.voice
        null -> null
    }
    val initiallyExpandedLanguages = remember(voices, selectedVoiceId, engineDefaultVoice, language) {
        buildSet {
            language?.let(::add)
            voices.singleOrNull { it.id == selectedVoiceId }?.language?.let(::add)
            voices.singleOrNull { it.id == engineDefaultVoice }?.language?.let(::add)
        }
    }
    var expandedLanguages by remember(voices, language) { mutableStateOf(initiallyExpandedLanguages) }
    val filtered = remember(compatible, query, locale) {
        compatible.filter { voice -> voice.matches(query, locale) }
    }
    val groups = remember(filtered, locale) {
        filtered
            .groupBy(TtsVoice::language)
            .map { (tag, groupedVoices) ->
                TtsVoiceLanguageGroup(
                    language = tag,
                    displayName = tag.displayName(locale),
                    voices = groupedVoices,
                )
            }
            .sortedWith(compareBy({ it.displayName.lowercase(locale) }, { it.language.value }))
    }
    val engineDefaultTitle = stringResource(MR.strings.tts_settings_voice_engine_default)
    val showEngineDefault = engineDefault != null && language == null &&
        (query.isBlank() || engineDefault.matches(query, locale) || engineDefaultTitle.contains(query, true))

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
                label = { Text(stringResource(MR.strings.tts_settings_search_voices)) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = if (query.isNotEmpty()) {
                    {
                        IconButton(onClick = { query = "" }) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
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
        if (showEngineDefault) {
            item(key = "engine-default") {
                TtsRadioRow(
                    title = engineDefaultTitle,
                    subtitle = stringResource(
                        MR.strings.tts_settings_voice_engine_default_summary,
                        engineDefault.name,
                        engineDefault.language.displayName(),
                        engineDefault.processing.label(),
                    ),
                    selected = selected == TtsDefaultVoiceSelection.EngineDefault,
                    playing = previewingVoice == engineDefault.id,
                    onSelect = { onSelect(TtsDefaultVoiceSelection.EngineDefault, engineDefault) },
                )
            }
        }
        groups.forEach { group ->
            val expanded = query.isNotBlank() || group.language in expandedLanguages
            item(key = "language-${group.language.value}") {
                TtsVoiceLanguageGroupHeader(
                    language = group.language,
                    displayName = group.displayName,
                    voiceCount = group.voices.size,
                    expanded = expanded,
                    collapsible = query.isBlank(),
                    onToggle = {
                        if (query.isBlank()) {
                            expandedLanguages = if (expanded) {
                                expandedLanguages - group.language
                            } else {
                                expandedLanguages + group.language
                            }
                        }
                    },
                )
            }
            if (expanded) {
                items(group.voices, key = { "${group.language.value}:${it.id.value}" }) { voice ->
                    val selection = TtsDefaultVoiceSelection.Explicit(voice.id)
                    TtsRadioRow(
                        title = voice.name,
                        subtitle = stringResource(
                            MR.strings.tts_settings_voice_summary,
                            voice.language.displayName(),
                            voice.processing.label(),
                        ),
                        selected = selected == selection,
                        playing = previewingVoice == voice.id,
                        onSelect = { onSelect(selection, voice) },
                    )
                }
            }
        }
        if (!showEngineDefault && groups.isEmpty()) {
            item(key = "empty") {
                Text(
                    text = stringResource(
                        if (query.isBlank()) {
                            MR.strings.tts_settings_no_compatible_voices
                        } else {
                            MR.strings.no_results_found
                        },
                    ),
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TtsVoiceProcessing.label(): String {
    return stringResource(
        when (this) {
            TtsVoiceProcessing.OnDevice -> MR.strings.tts_settings_voice_on_device
            TtsVoiceProcessing.NetworkRequired -> MR.strings.tts_settings_voice_network
            TtsVoiceProcessing.Unknown -> MR.strings.tts_settings_voice_unknown
        },
    )
}

private data class TtsVoiceLanguageGroup(
    val language: LanguageTag,
    val displayName: String,
    val voices: List<TtsVoice>,
)

private fun TtsVoice.matches(query: String, locale: Locale): Boolean {
    if (query.isBlank()) return true
    return name.contains(query, ignoreCase = true) ||
        id.value.contains(query, ignoreCase = true) ||
        language.value.contains(query, ignoreCase = true) ||
        language.displayName(locale).contains(query, ignoreCase = true)
}

private fun voiceOrder(locale: Locale) = compareBy<TtsVoice>(
    { it.language.displayName(locale).lowercase(locale) },
    { it.language.value },
    { it.processing == TtsVoiceProcessing.NetworkRequired },
    { it.name.lowercase(locale) },
    { it.id.value },
)
