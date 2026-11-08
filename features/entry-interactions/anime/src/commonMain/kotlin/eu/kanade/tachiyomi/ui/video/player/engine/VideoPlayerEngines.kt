package eu.kanade.tachiyomi.ui.video.player.engine

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.kanade.tachiyomi.ui.video.player.components.SourceChoiceAction
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerHeadingText
import mihon.video.engine.spi.VideoEngine

/**
 * The engines the watched player plays in. [builtIn] is the platform's own. On Android, mpv comes from an add-on the
 * viewer installs: Auto moves to it when the built-in player can't play a stream, and a title can be set to it. Desktop
 * plays everything in mpv as its built-in engine (D001), so its [mpvAddon] is null: there is no engine to choose,
 * switch to or install, and the screens about one never show.
 */
internal class VideoPlayerEngines(
    val builtIn: VideoEngine,
    val mpvAddon: MpvAddonEngine?,
)

/** mpv played from the add-on: its engine, whether it can play now, and the problem screen that gets it. */
internal interface MpvAddonEngine {
    val engine: VideoEngine

    /** Opens the settings where the add-on is installed, as the hint that mpv would play a title better offers. */
    fun openSettings()

    /** Whether mpv can play now; the caller recomposes when the add-on changes. */
    @Composable
    fun availability(): MpvAvailability

    /**
     * The problem screen for a stream only mpv would play while the add-on is missing or too old: it offers the add-on,
     * follows its install, and calls [onInstalled] once mpv can play.
     */
    @Composable
    fun AddonProblem(
        problem: EngineProblem.NeedsAddon,
        onBack: () -> Unit,
        sourceChoice: SourceChoiceAction?,
        heading: VideoPlayerHeadingText?,
        onInstalled: () -> Unit,
        modifier: Modifier,
    )
}
