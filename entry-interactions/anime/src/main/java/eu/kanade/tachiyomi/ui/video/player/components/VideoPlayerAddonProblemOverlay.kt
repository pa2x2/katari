package eu.kanade.tachiyomi.ui.video.player.components

import android.text.format.Formatter
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import eu.kanade.tachiyomi.ui.video.player.engine.EngineProblem
import eu.kanade.tachiyomi.ui.video.player.engine.MpvAvailability
import eu.kanade.tachiyomi.ui.video.player.engine.availability
import eu.kanade.tachiyomi.ui.video.player.isOnline
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.progress.LinearProgressIndicator
import mihon.video.engine.mpv.addon.MpvAddon
import mihon.video.engine.mpv.install.MpvAddonInstallStatus
import mihon.video.engine.mpv.install.MpvAddonInstallation
import mihon.video.engine.mpv.install.describe
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Auto needs mpv for a stream the built-in player can't play, and the add-on is missing or too old: the screen offers
 * it, follows its download and Android's install, and calls [onInstalled] once mpv can play. Offline it offers only
 * Retry, which checks the connection again; nothing downloads unless the viewer asks for it here.
 */
@Composable
internal fun VideoPlayerAddonProblemOverlay(
    problem: EngineProblem.NeedsAddon,
    addon: MpvAddon,
    installation: MpvAddonInstallation,
    onBack: () -> Unit,
    sourceChoice: SourceChoiceAction?,
    onInstalled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val install by installation.state.collectAsState()
    val addonState by addon.state.collectAsState()
    var offline by remember { mutableStateOf(!context.isOnline()) }
    // Android's install dialog and its permission screen are other activities; coming back may have missed an event.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { addon.refresh() }
    LaunchedEffect(offline) {
        val current = installation.state.value
        if (!offline && current.offer == null && current.status == MpvAddonInstallStatus.Idle) installation.lookUp()
    }
    val available = addon.availability(addonState) == MpvAvailability.Available
    LaunchedEffect(available) {
        if (available) onInstalled()
    }

    val offer = install.offer
    val size = offer?.let { Formatter.formatShortFileSize(context, it.downloadBytes) }
    val title = stringResource(
        if (problem.update) MR.strings.anime_problem_addon_update else MR.strings.anime_problem_needs_addon,
    )
    val details = engineFailureDetails(problem.failures)
    val gettingTitle = stringResource(MR.strings.anime_problem_getting_addon)
    when (val status = install.status) {
        is MpvAddonInstallStatus.Downloading -> {
            val total = offer?.downloadBytes ?: 0L
            val progress = status.progress
            VideoPlayerProblemFrame(
                title = gettingTitle,
                explanation = if (progress != null && total > 0) {
                    stringResource(
                        MR.strings.anime_problem_addon_progress,
                        Formatter.formatShortFileSize(context, (progress * total).toLong()),
                        Formatter.formatShortFileSize(context, total),
                    )
                } else {
                    stringResource(MR.strings.download_state_downloading)
                },
                onBack = onBack,
                modifier = modifier,
                progress = {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        progress = progress?.let { value -> { value } },
                    )
                },
            ) {
                Button(
                    label = stringResource(MR.strings.action_cancel),
                    onClick = installation::cancel,
                    style = ButtonStyle.Tonal,
                )
            }
        }
        MpvAddonInstallStatus.NeedsPermission -> VideoPlayerProblemFrame(
            title = gettingTitle,
            explanation = stringResource(MR.strings.app_update_waiting_permission),
            onBack = onBack,
            modifier = modifier,
        ) {
            sourceChoice?.let { SourceChoiceButton(it) }
            Button(
                label = stringResource(MR.strings.app_update_allow_installs),
                onClick = { context.startActivity(installation.installPermissionIntent()) },
            )
        }
        // Installed waits for the package event that makes the add-on available.
        MpvAddonInstallStatus.Installing, MpvAddonInstallStatus.Installed -> VideoPlayerProblemFrame(
            title = gettingTitle,
            explanation = stringResource(MR.strings.app_update_installing),
            onBack = onBack,
            modifier = modifier,
            progress = { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) },
        ) {}
        is MpvAddonInstallStatus.Failed -> VideoPlayerProblemFrame(
            title = title,
            explanation = status.error.describe(context),
            onBack = onBack,
            modifier = modifier,
            details = details,
        ) {
            sourceChoice?.let { SourceChoiceButton(it) }
            Button(label = stringResource(MR.strings.action_retry), onClick = installation::install)
        }
        MpvAddonInstallStatus.Idle, MpvAddonInstallStatus.LookingUp -> if (offline) {
            VideoPlayerProblemFrame(
                title = title,
                explanation = size
                    ?.let { stringResource(MR.strings.anime_problem_needs_addon_offline, it) }
                    ?: stringResource(MR.strings.anime_problem_needs_addon_offline_unsized),
                onBack = onBack,
                modifier = modifier,
                details = details,
            ) {
                sourceChoice?.let { SourceChoiceButton(it) }
                Button(
                    label = stringResource(MR.strings.action_retry),
                    onClick = {
                        offline = !context.isOnline()
                        if (!offline) installation.install()
                    },
                )
            }
        } else {
            VideoPlayerProblemFrame(
                title = title,
                explanation = when {
                    problem.update && offer != null && size != null ->
                        stringResource(MR.strings.anime_problem_addon_update_explanation, offer.version, size)
                    problem.update -> stringResource(MR.strings.anime_problem_addon_update_explanation_unsized)
                    size != null -> stringResource(MR.strings.anime_problem_needs_addon_explanation, size)
                    else -> stringResource(MR.strings.anime_problem_needs_addon_explanation_unsized)
                },
                onBack = onBack,
                modifier = modifier,
                details = details,
            ) {
                sourceChoice?.let { SourceChoiceButton(it) }
                Button(
                    label = stringResource(
                        if (problem.update) MR.strings.action_update else MR.strings.anime_problem_install_addon,
                    ),
                    onClick = installation::install,
                )
            }
        }
    }
}
