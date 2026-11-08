package app.katari.tts.ui.picker.voice

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.Add
import app.katari.language.api.tag.LanguageTag
import app.katari.presentation.kit.button.Button
import app.katari.presentation.kit.button.ButtonStyle
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.layout.RowList
import app.katari.presentation.kit.list.RowGroupItem
import app.katari.presentation.kit.list.RowGroupPosition
import app.katari.presentation.kit.list.SettingsRow
import app.katari.presentation.kit.theme.ContentTone
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole
import app.katari.tts.api.voice.TtsVoice
import app.katari.tts.api.voice.TtsVoiceId
import app.katari.tts.ui.settings.displayName
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
