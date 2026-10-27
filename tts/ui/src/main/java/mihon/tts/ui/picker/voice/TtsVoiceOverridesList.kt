package mihon.tts.ui.picker.voice

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Add
import mihon.language.api.tag.LanguageTag
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.layout.RowList
import mihon.presentation.kit.list.RowGroupItem
import mihon.presentation.kit.list.RowGroupPosition
import mihon.presentation.kit.list.SettingsRow
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import mihon.tts.api.voice.TtsVoice
import mihon.tts.api.voice.TtsVoiceId
import mihon.tts.ui.settings.displayName
import tachiyomi.i18n.*
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
    val sortedOverrides = overrides.entries.sortedBy { it.key.displayName() }
    // The overrides and the row that adds one share a card.
    val rowCount = sortedOverrides.size + 1
    RowList(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = KatariTheme.spacing.s),
    ) {
        itemsIndexed(sortedOverrides, key = { _, override -> override.key.value }) { index, (language, selectedVoice) ->
            val voice = voices.singleOrNull { it.id == selectedVoice }
            RowGroupItem(position = RowGroupPosition.of(index, rowCount), modifier = Modifier.animateItem()) {
                SettingsRow(
                    title = language.displayName(),
                    trailing = {
                        Button(
                            label = stringResource(MR.strings.action_delete),
                            onClick = { onDelete(language) },
                            style = ButtonStyle.Text,
                        )
                    },
                    onClick = { onEdit(language) },
                    below = {
                        Text(
                            text = voice?.name ?: stringResource(MR.strings.tts_settings_voice_unavailable),
                            role = TextRole.Supporting,
                            tone = if (voice == null) ContentTone.Error else ContentTone.Secondary,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                        )
                    },
                )
            }
        }
        item(key = "add") {
            RowGroupItem(position = RowGroupPosition.of(rowCount - 1, rowCount), modifier = Modifier.animateItem()) {
                SettingsRow(
                    title = stringResource(MR.strings.tts_settings_add_language),
                    icon = MaterialSymbols.Rounded.Add,
                    onClick = onAdd,
                )
            }
        }
    }
}
