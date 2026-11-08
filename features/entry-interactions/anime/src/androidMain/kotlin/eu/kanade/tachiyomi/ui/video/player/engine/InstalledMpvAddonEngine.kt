package eu.kanade.tachiyomi.ui.video.player.engine

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import eu.kanade.tachiyomi.ui.video.player.components.SourceChoiceAction
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerAddonProblemOverlay
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerHeadingText
import mihon.video.engine.mpv.MpvVideoEngine
import mihon.video.engine.mpv.addon.MpvAddon
import mihon.video.engine.mpv.install.MpvAddonInstallation

/**
 * Android's mpv, played from the add-on APK the viewer installs through [installation] or from the anime player
 * settings [onOpenSettings] opens.
 */
internal class InstalledMpvAddonEngine(
    override val engine: MpvVideoEngine,
    private val addon: MpvAddon,
    private val installation: MpvAddonInstallation,
    private val onOpenSettings: () -> Unit,
) : MpvAddonEngine {
    override fun openSettings() = onOpenSettings()

    @Composable
    override fun availability(): MpvAvailability {
        val state by addon.state.collectAsState()
        return addon.availability(state)
    }

    @Composable
    override fun AddonProblem(
        problem: EngineProblem.NeedsAddon,
        onBack: () -> Unit,
        sourceChoice: SourceChoiceAction?,
        heading: VideoPlayerHeadingText?,
        onInstalled: () -> Unit,
        modifier: Modifier,
    ) {
        VideoPlayerAddonProblemOverlay(
            problem = problem,
            addon = addon,
            installation = installation,
            onBack = onBack,
            sourceChoice = sourceChoice,
            heading = heading,
            onInstalled = onInstalled,
            modifier = modifier,
        )
    }
}
