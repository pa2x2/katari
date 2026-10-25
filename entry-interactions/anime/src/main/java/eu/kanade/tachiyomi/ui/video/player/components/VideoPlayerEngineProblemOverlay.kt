package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.kanade.tachiyomi.ui.video.player.engine.EngineFailure
import eu.kanade.tachiyomi.ui.video.player.engine.EngineProblem
import eu.kanade.tachiyomi.ui.video.player.engine.VideoPlayerEngine
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Both engines failed to play the stream under Auto: another stream is the way out. */
@Composable
internal fun VideoPlayerBothEnginesFailedOverlay(
    problem: EngineProblem.BothFailed,
    onBack: () -> Unit,
    changeSource: ChangeSource,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    VideoPlayerProblemFrame(
        title = stringResource(MR.strings.anime_problem_unplayable),
        explanation = stringResource(MR.strings.anime_problem_both_engines_failed),
        onBack = onBack,
        modifier = modifier,
        details = engineFailureDetails(problem.failures),
    ) {
        ChangeSourceButton(changeSource)
        Button(label = stringResource(MR.strings.action_retry), onClick = onRetry)
    }
}

/** The engine the title's override picks failed; [onTryOther] plays this episode in the other one. */
@Composable
internal fun VideoPlayerOverrideFailedOverlay(
    problem: EngineProblem.OverrideFailed,
    onBack: () -> Unit,
    onTryOther: () -> Unit,
    changeSource: ChangeSource,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    VideoPlayerProblemFrame(
        title = stringResource(MR.strings.anime_problem_unplayable),
        explanation = stringResource(
            when (problem.other) {
                VideoPlayerEngine.Mpv -> MR.strings.anime_problem_built_in_failed
                VideoPlayerEngine.BuiltIn -> MR.strings.anime_problem_mpv_failed
            },
        ),
        onBack = onBack,
        modifier = modifier,
        details = engineFailureDetails(problem.failures),
    ) {
        Button(
            label = stringResource(
                when (problem.other) {
                    VideoPlayerEngine.Mpv -> MR.strings.anime_player_engine_try_mpv
                    VideoPlayerEngine.BuiltIn -> MR.strings.anime_player_engine_try_built_in
                },
            ),
            onClick = onTryOther,
        )
        ChangeSourceButton(changeSource)
        Button(label = stringResource(MR.strings.action_retry), onClick = onRetry, style = ButtonStyle.Tonal)
    }
}

/** Each failed engine's error under its name, for the problem screen's details. */
@Composable
internal fun engineFailureDetails(failures: List<EngineFailure>): String? = failures
    .map { failure ->
        stringResource(
            MR.strings.anime_problem_engine_details,
            stringResource(
                when (failure.engine) {
                    VideoPlayerEngine.BuiltIn -> MR.strings.anime_player_engine_built_in_name
                    VideoPlayerEngine.Mpv -> MR.strings.anime_player_engine_mpv
                },
            ),
            failure.details.orEmpty(),
        )
    }
    .takeIf { it.isNotEmpty() }
    ?.joinToString(separator = "\n\n")
