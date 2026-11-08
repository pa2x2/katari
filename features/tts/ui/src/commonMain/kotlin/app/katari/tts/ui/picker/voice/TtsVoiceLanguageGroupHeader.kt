package app.katari.tts.ui.picker.voice

import androidx.compose.runtime.Composable
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.KeyboardArrowDown
import app.katari.icons.materialsymbols.rounded.KeyboardArrowUp
import app.katari.language.api.tag.LanguageTag
import app.katari.presentation.kit.content.Icon
import app.katari.presentation.kit.list.SettingsRow
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource

@Composable
internal fun TtsVoiceLanguageGroupHeader(
    language: LanguageTag,
    displayName: String,
    voiceCount: Int,
    expanded: Boolean,
    collapsible: Boolean,
    onToggle: () -> Unit,
) {
    SettingsRow(
        title = displayName,
        subtitle = "${language.value} · ${
            pluralStringResource(MR.plurals.tts_settings_voice_count, voiceCount, voiceCount)
        }",
        trailing = {
            Icon(
                imageVector = if (expanded) {
                    MaterialSymbols.Rounded.KeyboardArrowUp
                } else {
                    MaterialSymbols.Rounded.KeyboardArrowDown
                },
                contentDescription = null,
            )
        },
        onClick = if (collapsible) onToggle else null,
    )
}
