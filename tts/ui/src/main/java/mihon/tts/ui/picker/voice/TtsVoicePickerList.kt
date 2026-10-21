package mihon.tts.ui.picker.voice

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mihon.language.api.tag.LanguageTag
import mihon.presentation.kit.feedback.NoResults
import mihon.presentation.kit.list.RowGroupGap
import mihon.presentation.kit.list.RowGroupItem
import mihon.presentation.kit.list.RowGroupPosition
import mihon.presentation.kit.list.rowGroupItems
import mihon.presentation.kit.progress.LoadingIndicator
import mihon.presentation.kit.theme.KatariTheme
import mihon.tts.api.provider.TtsVoiceProcessing
import mihon.tts.api.voice.TtsDefaultVoiceSelection
import mihon.tts.api.voice.TtsVoice
import mihon.tts.api.voice.TtsVoiceId
import tachiyomi.i18n.*
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
    val locale = Locale.getDefault()
    val catalog by produceState<TtsVoicePickerCatalog?>(null, voices, language, locale) {
        value = withContext(Dispatchers.Default) {
            ttsVoicePickerCatalog(voices, language, locale)
        }
    }
    val readyCatalog = catalog
    if (readyCatalog == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            LoadingIndicator()
        }
        return
    }

    TtsVoicePickerContent(
        catalog = readyCatalog,
        selected = selected,
        previewingVoice = previewingVoice,
        onSelect = onSelect,
        modifier = modifier,
        language = language,
        engineDefaultVoice = engineDefaultVoice,
        locale = locale,
    )
}

@Composable
private fun TtsVoicePickerContent(
    catalog: TtsVoicePickerCatalog,
    selected: TtsDefaultVoiceSelection?,
    previewingVoice: TtsVoiceId?,
    onSelect: (TtsDefaultVoiceSelection, TtsVoice) -> Unit,
    modifier: Modifier,
    language: LanguageTag?,
    engineDefaultVoice: TtsVoiceId?,
    locale: Locale,
) {
    var searchQuery by remember { mutableStateOf("") }
    val engineDefault = catalog.find(engineDefaultVoice)
    val selectedVoiceId = when (selected) {
        TtsDefaultVoiceSelection.EngineDefault -> engineDefaultVoice
        is TtsDefaultVoiceSelection.Explicit -> selected.voice
        null -> null
    }
    val initiallyExpandedLanguages = remember(catalog, selectedVoiceId, engineDefaultVoice, language) {
        buildSet {
            language?.let(::add)
            catalog.find(selectedVoiceId)?.voice?.language?.let(::add)
            catalog.find(engineDefaultVoice)?.voice?.language?.let(::add)
        }
    }
    var expandedLanguages by remember(catalog, language) { mutableStateOf(initiallyExpandedLanguages) }
    val groups = remember(catalog, searchQuery, locale) {
        catalog.search(searchQuery, locale)
    }
    val engineDefaultTitle = stringResource(MR.strings.tts_settings_voice_engine_default)
    val showEngineDefault = engineDefault != null && language == null &&
        (
            searchQuery.isBlank() ||
                engineDefault.matches(searchQuery.lowercase(locale)) ||
                engineDefaultTitle.contains(searchQuery, true)
            )
    val rows = remember(groups, expandedLanguages, searchQuery) {
        buildList {
            groups.forEach { group ->
                add(TtsVoicePickerRow.Language(group))
                if (searchQuery.isNotBlank() || group.language in expandedLanguages) {
                    group.voices.forEach { add(TtsVoicePickerRow.Voice(group.language, it)) }
                }
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = KatariTheme.spacing.s),
    ) {
        item(key = "search") {
            TtsVoiceSearchField(
                onSearchQueryChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = KatariTheme.spacing.m, vertical = KatariTheme.spacing.s),
            )
        }
        if (showEngineDefault) {
            item(key = "engine-default") {
                RowGroupItem(position = RowGroupPosition.Alone) {
                    TtsVoiceRow(
                        title = engineDefaultTitle,
                        subtitle = stringResource(
                            MR.strings.tts_settings_voice_engine_default_summary,
                            engineDefault.voice.name,
                            engineDefault.languageDisplayName,
                            engineDefault.voice.processing.label(),
                        ),
                        selected = selected == TtsDefaultVoiceSelection.EngineDefault,
                        playing = previewingVoice == engineDefault.voice.id,
                        onSelect = { onSelect(TtsDefaultVoiceSelection.EngineDefault, engineDefault.voice) },
                    )
                }
            }
            if (rows.isNotEmpty()) item(key = "engine-default-gap") { RowGroupGap() }
        }
        // The languages and the voices of the expanded ones share one card.
        rowGroupItems(
            items = rows,
            key = TtsVoicePickerRow::key,
            contentType = TtsVoicePickerRow::contentType,
        ) { row ->
            when (row) {
                is TtsVoicePickerRow.Language -> {
                    val group = row.group
                    val expanded = searchQuery.isNotBlank() || group.language in expandedLanguages
                    TtsVoiceLanguageGroupHeader(
                        language = group.language,
                        displayName = group.displayName,
                        voiceCount = group.voices.size,
                        expanded = expanded,
                        collapsible = searchQuery.isBlank(),
                        onToggle = {
                            if (searchQuery.isBlank()) {
                                expandedLanguages = if (expanded) {
                                    expandedLanguages - group.language
                                } else {
                                    expandedLanguages + group.language
                                }
                            }
                        },
                    )
                }
                is TtsVoicePickerRow.Voice -> {
                    val entry = row.entry
                    val voice = entry.voice
                    val selection = TtsDefaultVoiceSelection.Explicit(voice.id)
                    TtsVoiceRow(
                        title = voice.name,
                        subtitle = stringResource(
                            MR.strings.tts_settings_voice_summary,
                            entry.languageDisplayName,
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
                NoResults(
                    message = stringResource(
                        if (searchQuery.isBlank()) {
                            MR.strings.tts_settings_no_compatible_voices
                        } else {
                            MR.strings.no_results_found
                        },
                    ),
                )
            }
        }
    }
}

@Immutable
private sealed interface TtsVoicePickerRow {
    val key: String
    val contentType: String

    data class Language(val group: TtsVoiceLanguageGroup) : TtsVoicePickerRow {
        override val key = "language-${group.language.value}"
        override val contentType = "language"
    }

    data class Voice(
        val language: LanguageTag,
        val entry: TtsVoicePickerEntry,
    ) : TtsVoicePickerRow {
        override val key = "voice-${language.value}:${entry.voice.id.value}"
        override val contentType = "voice"
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
