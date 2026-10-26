package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.runtime.Composable
import mihon.entry.interactions.settings.AnimeMarkerSkipMode
import mihon.presentation.kit.chip.FilterChip
import mihon.presentation.kit.list.ChipsRow
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** [override] is the title's own skip mode; null follows the one in the anime player settings. */
internal data class VideoPlayerMarkerSkipChoice(val override: AnimeMarkerSkipMode?)

/** What the title does at its marked intro, recap, credits and preview. */
@Composable
internal fun VideoPlayerMarkerSkipRow(
    choice: VideoPlayerMarkerSkipChoice,
    onSelect: (AnimeMarkerSkipMode?) -> Unit,
) {
    ChipsRow(title = stringResource(MR.strings.anime_marker_skip)) {
        FilterChip(
            label = stringResource(MR.strings.label_default),
            selected = choice.override == null,
            onClick = { onSelect(null) },
        )
        AnimeMarkerSkipMode.entries.forEach { mode ->
            FilterChip(
                label = stringResource(
                    when (mode) {
                        AnimeMarkerSkipMode.BUTTON -> MR.strings.anime_marker_skip_button
                        AnimeMarkerSkipMode.COUNTDOWN -> MR.strings.anime_marker_skip_countdown
                        AnimeMarkerSkipMode.AUTOMATIC -> MR.strings.anime_marker_skip_automatic
                        AnimeMarkerSkipMode.OFF -> MR.strings.off
                    },
                ),
                selected = choice.override == mode,
                onClick = { onSelect(mode) },
            )
        }
    }
}
