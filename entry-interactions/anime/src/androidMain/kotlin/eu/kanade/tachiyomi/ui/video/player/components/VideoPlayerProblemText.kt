package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.runtime.Composable
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerProblem
import eu.kanade.tachiyomi.ui.video.player.recovery.VideoProblemAction
import eu.kanade.tachiyomi.ui.video.player.recovery.VideoSourceChoice
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.ViewList
import mihon.icons.materialsymbols.rounded.Extension
import mihon.icons.materialsymbols.rounded.Settings
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * What [cause]'s screen says under its title, naming only buttons the screen has: how to use [choice] when another
 * stream may play, listing the [failedServers] when another server is the choice; else what [action] fixes; else the
 * cause's own explanation.
 */
@Composable
internal fun problemExplanation(
    cause: VideoPlayerProblem.Cause,
    choice: VideoSourceChoice?,
    failedServers: List<String> = emptyList(),
    action: VideoProblemAction? = null,
): String = when {
    choice != null && cause.switchHelps -> if (choice == VideoSourceChoice.Server && failedServers.isNotEmpty()) {
        stringResource(MR.strings.anime_problem_hint_servers_failed, failedServers.joinToString(separator = ", "))
    } else {
        sourceChoiceHint(choice)
    }
    action is VideoProblemAction.OpenEntry -> stringResource(MR.strings.anime_problem_not_found_episode_list)
    action is VideoProblemAction.OpenSourceSettings -> stringResource(
        MR.strings.anime_problem_torrent_only_source_settings,
    )
    else -> stringResource(cause.explanation)
}

@Composable
internal fun VideoProblemActionButton(action: VideoProblemAction, onClick: () -> Unit) {
    when (action) {
        VideoProblemAction.OpenExtensions -> Button(
            label = stringResource(MR.strings.label_extensions),
            onClick = onClick,
            style = ButtonStyle.Tonal,
            icon = MaterialSymbols.Rounded.Extension,
        )
        is VideoProblemAction.OpenEntry -> Button(
            label = stringResource(MR.strings.anime_problem_episode_list),
            onClick = onClick,
            style = ButtonStyle.Tonal,
            icon = MaterialSymbols.AutoMirroredRounded.ViewList,
        )
        is VideoProblemAction.OpenSourceSettings -> Button(
            label = stringResource(MR.strings.source_settings),
            onClick = onClick,
            style = ButtonStyle.Tonal,
            icon = MaterialSymbols.Rounded.Settings,
        )
    }
}
