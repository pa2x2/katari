package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import app.katari.presentation.kit.content.MediaPlacementMarks
import app.katari.presentation.kit.content.PlacementGuide
import app.katari.presentation.kit.pointer.moveCursor
import app.katari.presentation.kit.shortcut.KeyboardShortcutHandler
import app.katari.presentation.kit.shortcut.LocalKeyboardShortcuts
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.video.engine.spi.player.SubtitleCue
import eu.kanade.tachiyomi.ui.video.player.gesture.VideoPlayerShortcuts
import eu.kanade.tachiyomi.ui.video.player.subtitle.SubtitleGuide
import eu.kanade.tachiyomi.ui.video.player.subtitle.VideoSubtitleAppearance
import eu.kanade.tachiyomi.ui.video.player.subtitle.VideoSubtitleLayout
import eu.kanade.tachiyomi.ui.video.player.subtitle.VideoSubtitles
import eu.kanade.tachiyomi.ui.video.player.subtitle.guideAt
import eu.kanade.tachiyomi.ui.video.player.subtitle.moveSubtitles
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The subtitle editor's preview: [previewCues] drawn as the player draws subtitles of [appearance] where [layout] puts
 * them, framed to show they move. A drag that starts on them moves them, snapping to the picture's centre and to where
 * each row rests on the way, and where the window has keyboard shortcuts the arrow keys step them; [onDraftChange]
 * hears each move. [onDraggingChange] hears a drag start and end, so the panel can step out of the way.
 */
@Composable
internal fun VideoSubtitleEditorPreview(
    appearance: VideoSubtitleAppearance,
    layout: VideoSubtitleLayout,
    previewCues: List<SubtitleCue>,
    onDraftChange: (VideoSubtitleAppearance) -> Unit,
    onDraggingChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val latestAppearance by rememberUpdatedState(appearance)
    val latestLayout by rememberUpdatedState(layout)
    // Where the last drawing put the cues the app places, and every cue; equal values written again change nothing
    var stack by remember { mutableStateOf<Rect?>(null) }
    var drawn by remember { mutableStateOf<Rect?>(null) }
    var drag by remember { mutableStateOf<PreviewDrag?>(null) }
    var reached by remember { mutableStateOf(emptySet<SubtitleGuide>()) }
    var hovering by remember { mutableStateOf(false) }
    // The hint stays until the first move of this visit to the editor
    var moved by rememberSaveable { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val snapPx = with(density) { KatariTheme.spacing.s.toPx() }
    val reachPx = with(density) { KatariTheme.spacing.m.toPx() }
    val keys = LocalKeyboardShortcuts.current != null

    fun grabs(point: Offset) = drawn?.inflate(reachPx)?.contains(point) == true

    fun moveTo(from: Rect, to: Rect, start: VideoSubtitleAppearance, snap: Float): Set<SubtitleGuide> {
        val move = latestLayout.moveSubtitles(start, from, to, stacked = stack != null, snapPx = snap)
        onDraftChange(move.appearance)
        return move.reached
    }

    fun endDrag() {
        if (drag == null) return
        drag = null
        reached = emptySet()
        onDraggingChange(false)
    }

    Box(modifier) {
        VideoSubtitles(
            cues = previewCues,
            appearance = appearance,
            layout = { _, _ -> layout },
            modifier = Modifier.fillMaxSize(),
            onPlaced = { placed ->
                stack = placed.resting
                drawn = placed.bounds
            },
        )
        MediaPlacementMarks(
            frame = drawn,
            moving = drag != null,
            guides = if (drag != null && stack != null) layout.placementGuides(reached) else emptyList(),
            hint = when {
                moved -> null
                keys -> stringResource(MR.strings.anime_playback_subtitle_drag_keys_hint)
                else -> stringResource(MR.strings.anime_playback_subtitle_drag_hint)
            },
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .moveCursor(enabled = hovering || drag != null)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            hovering = event.type != PointerEventType.Exit &&
                                event.changes.any { grabs(it.position) }
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { point ->
                            val from = (stack ?: drawn)?.takeIf { grabs(point) } ?: return@detectDragGestures
                            drag = PreviewDrag(from, latestAppearance, Offset.Zero)
                            moved = true
                            onDraggingChange(true)
                        },
                        onDragEnd = ::endDrag,
                        onDragCancel = ::endDrag,
                    ) { change, amount ->
                        val current = drag ?: return@detectDragGestures
                        val dragged = current.copy(by = current.by + amount)
                        drag = dragged
                        val nowReached = moveTo(dragged.from, dragged.from.translate(dragged.by), dragged.start, snapPx)
                        if ((nowReached - reached).isNotEmpty()) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        reached = nowReached
                        change.consume()
                    }
                },
        )
    }

    STEPS.forEach { (dx, dy) ->
        listOf(false, true).forEach { far ->
            KeyboardShortcutHandler(VideoPlayerShortcuts.moveSubtitles(dx, dy, far)) {
                val from = stack ?: drawn ?: return@KeyboardShortcutHandler
                val step = if (far) FAR_KEY_STEP else KEY_STEP
                val to = from.translate(dx * step * latestLayout.areaWidth, dy * step * latestLayout.areaHeight)
                moveTo(from, to, latestAppearance, KEY_SNAP_PX)
                moved = true
            }
        }
    }
}

/** A drag that started with the subtitles at [from] and the draft at [start], moved [by] since. */
private data class PreviewDrag(val from: Rect, val start: VideoSubtitleAppearance, val by: Offset)

@Composable
private fun VideoSubtitleLayout.placementGuides(reached: Set<SubtitleGuide>): List<PlacementGuide> =
    SubtitleGuide.entries.map { guide ->
        val vertical = guide == SubtitleGuide.CENTER
        PlacementGuide(
            vertical = vertical,
            at = guideAt(guide),
            from = if (vertical) areaTop else areaLeft,
            to = if (vertical) areaTop + areaHeight else areaLeft + areaWidth,
            label = stringResource(
                when (guide) {
                    SubtitleGuide.CENTER -> MR.strings.anime_playback_subtitle_guide_center
                    SubtitleGuide.TOP -> MR.strings.anime_playback_subtitle_guide_top
                    SubtitleGuide.MIDDLE -> MR.strings.anime_playback_subtitle_guide_middle
                    SubtitleGuide.USUAL -> MR.strings.anime_playback_subtitle_guide_usual
                },
            ),
            reached = guide in reached,
        )
    }

private val STEPS = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)

// Shares of the picture's size an arrow key moves the subtitles by, and with Shift
private const val KEY_STEP = 0.01f
private const val FAR_KEY_STEP = 0.05f

// A step along a row or column keeps its resting place against the rounding of the drawn bounds
private const val KEY_SNAP_PX = 0.5f
