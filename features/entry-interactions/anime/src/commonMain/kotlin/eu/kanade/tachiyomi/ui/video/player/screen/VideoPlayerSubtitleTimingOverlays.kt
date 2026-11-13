package eu.kanade.tachiyomi.ui.video.player.screen

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerNoticePill
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerSubtitleTimingBar
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerSubtitleTimingPill
import eu.kanade.tachiyomi.ui.video.player.subtitle.timing.VideoPlayerSubtitleTiming
import eu.kanade.tachiyomi.ui.video.player.subtitle.timing.signedSubtitleTimingSeconds
import kotlinx.coroutines.delay
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The subtitle timing over the video: the timing bar from Settings' Adjust until it is closed, while a subtitle whose
 * timing can be set plays; a pill with the timing a key just set; and, once the first frame shows, the notice that the
 * episode opened with a timing, which offers Reset. The bar steps aside while [barCovered], a failure shown in its
 * place, and comes back after; the pill and the notice wait while [statusShown], another notice in theirs.
 * [positionMs] is the playback position now, which the bar's line marks take.
 */
@Composable
internal fun BoxScope.VideoPlayerSubtitleTimingOverlays(
    timing: VideoPlayerSubtitleTiming,
    ui: VideoPlayerWatchUiState,
    barCovered: Boolean,
    statusShown: Boolean,
    positionMs: () -> Long,
) {
    val state by timing.state.collectAsState()
    val keyed = rememberKeyedTiming(ui)

    val notice = timing.notice.collectAsState().value?.takeIf { !ui.startupOverlayVisible }
    if (notice != null) {
        LaunchedEffect(notice) {
            delay(NOTICE_MS)
            timing.dismissNotice()
        }
    }

    val barShown = ui.subtitleTimingVisible && state.adjustable && !ui.settingsVisible && !barCovered
    when {
        barShown -> VideoPlayerSubtitleTimingBar(
            delayMs = state.delayMs,
            onSet = timing::set,
            onStep = timing::step,
            positionMs = positionMs,
            onClose = { ui.subtitleTimingVisible = false },
            modifier = Modifier.align(Alignment.TopCenter),
        )
        statusShown -> Unit
        keyed -> VideoPlayerSubtitleTimingPill(
            label = stringResource(MR.strings.anime_subtitle_timing_notice, signedSubtitleTimingSeconds(state.delayMs)),
            modifier = Modifier.align(Alignment.TopCenter),
        )
        notice != null -> VideoPlayerNoticePill(
            label = stringResource(MR.strings.anime_subtitle_timing_notice, signedSubtitleTimingSeconds(notice)),
            modifier = Modifier.align(Alignment.TopCenter),
            actionLabel = stringResource(MR.strings.action_reset),
            onAction = { timing.set(0L) },
            onDismiss = timing::dismissNotice,
        )
    }
}

/** Whether a key set the timing a moment ago, held long enough for the pill to be read. */
@Composable
private fun rememberKeyedTiming(ui: VideoPlayerWatchUiState): Boolean {
    var shown by remember { mutableStateOf(false) }
    val steps = ui.subtitleTimingKeySteps
    LaunchedEffect(steps) {
        if (steps == 0) return@LaunchedEffect
        shown = true
        delay(KEYED_PILL_HOLD_MS)
        shown = false
    }
    return shown
}

private const val NOTICE_MS = 8_000L
private const val KEYED_PILL_HOLD_MS = 1_500L
