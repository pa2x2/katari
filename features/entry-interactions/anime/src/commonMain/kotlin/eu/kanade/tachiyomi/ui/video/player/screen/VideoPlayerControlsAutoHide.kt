package eu.kanade.tachiyomi.ui.video.player.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import mihon.presentation.kit.pointer.LocalAppPointerPresence

private const val CONTROLS_AUTO_HIDE_DELAY_MS = 3_000L

/**
 * Hides the controls once they have been left alone a while, playing or paused, and at once when the mouse leaves the
 * app. Nothing hides them while [controlsPinned], loading, at the episode's end, scrubbing, or under the settings, the
 * episode list or the startup overlay; a mouse resting on one of them holds off only the wait.
 */
@Composable
internal fun VideoPlayerControlsAutoHide(ui: VideoPlayerWatchUiState, controlsPinned: Boolean) {
    val held = controlsPinned ||
        ui.playbackSnapshot.isLoading ||
        ui.playbackSnapshot.playbackEnded ||
        ui.isScrubbing ||
        ui.settingsVisible ||
        ui.episodesDrawerVisible ||
        ui.startupOverlayVisible
    LaunchedEffect(held, ui.controlsVisible, ui.controllerInteractionSequence, ui.pointerOverControls) {
        if (held || !ui.controlsVisible || ui.pointerOverControls) return@LaunchedEffect
        delay(CONTROLS_AUTO_HIDE_DELAY_MS)
        ui.controlsVisible = false
    }

    val pointer = LocalAppPointerPresence.current
    val latestHeld by rememberUpdatedState(held)
    if (pointer != null) {
        LaunchedEffect(pointer, ui) {
            // A leave, not being away: controls shown from the keys while the mouse is elsewhere keep their wait. The
            // mouse leaving over a control still counts as resting on it here, as the control hears it go only after.
            snapshotFlow { pointer.isOverApp }
                .drop(1)
                .filter { over -> !over }
                .collect { if (!latestHeld) ui.controlsVisible = false }
        }
    }
}
