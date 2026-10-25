package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.runtime.Composable
import eu.kanade.tachiyomi.ui.video.player.picture.label
import mihon.presentation.kit.chip.FilterChip
import mihon.presentation.kit.list.ChipsRow
import tachiyomi.domain.entry.model.PictureFraming
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Fit or Fill for the title; a pinch's zoom shows as a third, selected choice until one of them replaces it. */
@Composable
internal fun VideoPlayerPictureRow(
    framing: PictureFraming,
    onSelect: (PictureFraming) -> Unit,
) {
    ChipsRow(title = stringResource(MR.strings.anime_playback_picture)) {
        listOf(PictureFraming.Fit, PictureFraming.Fill).forEach { choice ->
            FilterChip(
                label = choice.label(),
                selected = framing == choice,
                onClick = { onSelect(choice) },
            )
        }
        if (framing is PictureFraming.Zoom) {
            FilterChip(label = framing.label(), selected = true, onClick = {})
        }
    }
}
