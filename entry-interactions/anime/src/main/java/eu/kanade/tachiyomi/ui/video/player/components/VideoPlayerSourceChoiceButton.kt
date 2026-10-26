package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.runtime.Composable
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerProblem
import eu.kanade.tachiyomi.ui.video.player.recovery.VideoSourceChoice
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** A problem screen's way to another server, dub, quality or stream, which opens where it is picked. */
internal data class SourceChoiceAction(val choice: VideoSourceChoice, val onClick: () -> Unit)

@Composable
internal fun SourceChoiceButton(action: SourceChoiceAction) {
    Button(
        label = stringResource(
            when (action.choice) {
                VideoSourceChoice.Server -> MR.strings.anime_playback_try_another_server
                VideoSourceChoice.Dub -> MR.strings.anime_playback_change_dub
                VideoSourceChoice.Quality -> MR.strings.anime_playback_change_quality
                VideoSourceChoice.Stream -> MR.strings.anime_playback_change_stream
            },
        ),
        onClick = action.onClick,
        style = ButtonStyle.Tonal,
    )
}

/**
 * What [cause]'s screen says under its title: how to use [choice] when another stream may play, otherwise the cause's
 * own explanation, which names only the buttons every screen for it has.
 */
@Composable
internal fun problemExplanation(cause: VideoPlayerProblem.Cause, choice: VideoSourceChoice?): String =
    if (choice != null && cause.switchHelps) sourceChoiceHint(choice) else stringResource(cause.explanation)

@Composable
internal fun sourceChoiceHint(choice: VideoSourceChoice): String = stringResource(
    when (choice) {
        VideoSourceChoice.Server -> MR.strings.anime_problem_hint_server
        VideoSourceChoice.Dub -> MR.strings.anime_problem_hint_dub
        VideoSourceChoice.Quality -> MR.strings.anime_problem_hint_quality
        VideoSourceChoice.Stream -> MR.strings.anime_problem_hint_stream
    },
)
