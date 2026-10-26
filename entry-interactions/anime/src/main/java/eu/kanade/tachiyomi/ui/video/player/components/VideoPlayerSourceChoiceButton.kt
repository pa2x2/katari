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
internal fun SourceChoiceButton(action: SourceChoiceAction, style: ButtonStyle = ButtonStyle.Tonal) {
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
        style = style,
    )
}

/**
 * What [cause]'s screen says under its title: how to use [choice] when another stream may play, after the
 * [failedServers] when another server is the choice, otherwise the cause's own explanation, which names only the
 * buttons every screen for it has.
 */
@Composable
internal fun problemExplanation(
    cause: VideoPlayerProblem.Cause,
    choice: VideoSourceChoice?,
    failedServers: List<String> = emptyList(),
): String = when {
    choice == null || !cause.switchHelps -> stringResource(cause.explanation)
    choice == VideoSourceChoice.Server && failedServers.isNotEmpty() ->
        stringResource(MR.strings.anime_problem_hint_servers_failed, failedServers.joinToString(separator = ", "))
    else -> sourceChoiceHint(choice)
}

@Composable
internal fun sourceChoiceHint(choice: VideoSourceChoice): String = stringResource(
    when (choice) {
        VideoSourceChoice.Server -> MR.strings.anime_problem_hint_server
        VideoSourceChoice.Dub -> MR.strings.anime_problem_hint_dub
        VideoSourceChoice.Quality -> MR.strings.anime_problem_hint_quality
        VideoSourceChoice.Stream -> MR.strings.anime_problem_hint_stream
    },
)
