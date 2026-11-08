package app.katari.tts.ui.picker.voice

import androidx.compose.runtime.Composable
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.automirroredrounded.VolumeUp
import app.katari.icons.materialsymbols.rounded.Stop
import app.katari.presentation.kit.content.Icon
import app.katari.presentation.kit.list.ChoiceRow
import app.katari.presentation.kit.theme.ContentTone

/** A voice to choose; its trailing icon shows that it is being previewed, or that it is the chosen voice. */
@Composable
internal fun TtsVoiceRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    playing: Boolean,
    onSelect: () -> Unit,
) {
    ChoiceRow(
        title = title,
        selected = selected,
        onClick = onSelect,
        subtitle = subtitle,
        trailing = when {
            playing -> {
                { Icon(MaterialSymbols.Rounded.Stop, contentDescription = null, tone = ContentTone.Primary) }
            }
            selected -> {
                {
                    Icon(
                        MaterialSymbols.AutoMirroredRounded.VolumeUp,
                        contentDescription = null,
                        tone = ContentTone.Primary,
                    )
                }
            }
            else -> null
        },
    )
}
