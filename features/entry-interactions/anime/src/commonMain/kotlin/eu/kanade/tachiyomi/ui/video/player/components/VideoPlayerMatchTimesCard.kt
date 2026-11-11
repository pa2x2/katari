package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.runtime.Composable
import app.katari.entry.interactions.anime.introtimes.IntroTimesService
import app.katari.presentation.kit.control.MediaMarkedSpan
import app.katari.presentation.kit.control.MediaMarksLine
import app.katari.presentation.kit.list.ControlRow
import app.katari.presentation.kit.list.RowAction
import app.katari.presentation.kit.list.RowActions
import app.katari.presentation.kit.list.RowGroupItem
import app.katari.presentation.kit.list.RowGroupPosition
import app.katari.presentation.kit.list.SettingsRow
import app.katari.presentation.kit.progress.CircularProgressIndicator
import app.katari.presentation.kit.progress.ProgressSize
import eu.kanade.tachiyomi.source.entry.PlaybackMarker
import eu.kanade.tachiyomi.ui.video.player.introtimes.VideoPlayerMatchTimes
import eu.kanade.tachiyomi.ui.video.player.introtimes.markSource
import eu.kanade.tachiyomi.ui.video.player.markers.VideoPlayerChapterName
import eu.kanade.tachiyomi.ui.video.player.playback.formatPlaybackTimestamp
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * What [service] has for the episode or film pointed at on its match page, named by [title]: its [times] by kind and
 * where they fall in the [lengthMs] of the cut playing, in the colour they take on the timeline. [onUse] keeps the
 * match that points there; it is null while there is none to keep.
 */
@Composable
internal fun VideoPlayerMatchTimesCard(
    service: IntroTimesService,
    title: String,
    times: VideoPlayerMatchTimes,
    lengthMs: Long,
    onRetry: () -> Unit,
    onUse: (() -> Unit)?,
) {
    val markers = (times as? VideoPlayerMatchTimes.Found)?.markers.orEmpty()
    RowGroupItem(position = RowGroupPosition.of(0, 1)) {
        when (times) {
            VideoPlayerMatchTimes.Unreachable -> VideoPlayerMatchFailedRow(
                title = stringResource(MR.strings.anime_intro_times_unreachable),
                onRetry = onRetry,
            )
            else -> SettingsRow(
                title = title,
                subtitle = when {
                    times !is VideoPlayerMatchTimes.Found -> stringResource(MR.strings.anime_intro_times_looking)
                    markers.isEmpty() -> stringResource(MR.strings.anime_intro_times_none_for_episode)
                    else -> markers.map { markerRange(it) }.joinToString(" · ")
                },
                trailing = if (times is VideoPlayerMatchTimes.Found) {
                    null
                } else {
                    { CircularProgressIndicator(size = ProgressSize.Inline) }
                },
            )
        }
        if (markers.isNotEmpty() && lengthMs > 0L) {
            ControlRow {
                MediaMarksLine(
                    spans = markers.map { marker ->
                        MediaMarkedSpan(
                            start = marker.startMs.toFloat() / lengthMs,
                            end = marker.endMs.toFloat() / lengthMs,
                            source = service.markSource,
                        )
                    },
                )
            }
        }
        if (onUse != null) {
            RowActions(listOf(RowAction(label = stringResource(MR.strings.anime_intro_times_use), onClick = onUse)))
        }
    }
}

@Composable
private fun markerRange(marker: PlaybackMarker): String = stringResource(
    MR.strings.anime_intro_times_marker_range,
    VideoPlayerChapterName.Marker(marker.kind).text(),
    formatPlaybackTimestamp(marker.startMs),
    formatPlaybackTimestamp(marker.endMs),
)
