package mihon.tts.ui.picker.voice

import androidx.compose.runtime.Composable
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.VolumeUp
import mihon.icons.materialsymbols.rounded.Stop
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.list.ChoiceRow
import mihon.presentation.kit.theme.ContentTone

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
