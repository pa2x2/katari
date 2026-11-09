package app.katari.presentation.kit.control

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.dp
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.automirroredrounded.VolumeOff
import app.katari.icons.materialsymbols.automirroredrounded.VolumeUp
import app.katari.presentation.kit.button.IconButton
import app.katari.presentation.kit.button.IconButtonSize
import app.katari.presentation.kit.shortcut.KeyboardShortcut
import app.katari.presentation.kit.theme.ContentTone
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.window.WindowWidth
import app.katari.presentation.kit.window.currentWindowLayout
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.math.roundToInt

/** When a [MediaVolumeControl] shows its slider. */
enum class MediaVolumeSlider {
    /** Beside the button, except in a compact window, which leaves room for the other controls with the button alone. */
    Always,

    /**
     * Only while the mouse points at the control, opening toward the start edge, for a lone button at the end edge of
     * media, such as a clip in the immersive feed.
     */
    WhilePointed,
}

/**
 * A player's volume among controls laid over video: a button that mutes and unmutes, and a slider setting [level] from
 * 0 to [maxLevel], shown as [slider] says. Above [fullLevel] the volume is louder than recorded, and a mark on the track
 * shows where that starts; null leaves the mark out. While [muted] the slider shows the level that unmuting returns to,
 * and moving it unmutes, as [onLevelChange] decides. With a [wheelStep], the wheel over the control changes the level
 * by that much a notch, and the wheel is the control's alone there.
 */
@Composable
fun MediaVolumeControl(
    level: Int,
    maxLevel: Int,
    fullLevel: Int?,
    muted: Boolean,
    onLevelChange: (Int) -> Unit,
    onToggleMute: () -> Unit,
    modifier: Modifier = Modifier,
    muteShortcut: KeyboardShortcut? = null,
    slider: MediaVolumeSlider = MediaVolumeSlider.Always,
    wheelStep: Int? = null,
) {
    val icon = if (muted || level == 0) {
        MaterialSymbols.AutoMirroredRounded.VolumeOff
    } else {
        MaterialSymbols.AutoMirroredRounded.VolumeUp
    }
    val hover = remember { MutableInteractionSource() }
    val pointed by hover.collectIsHoveredAsState()
    val latestLevel by rememberUpdatedState(level)
    val latestMaxLevel by rememberUpdatedState(maxLevel)
    val latestOnLevelChange by rememberUpdatedState(onLevelChange)
    val track: @Composable () -> Unit = {
        VolumeTrack(
            level = level,
            maxLevel = maxLevel.coerceAtLeast(1),
            fullLevel = fullLevel,
            dimmed = muted,
            onLevelChange = onLevelChange,
        )
    }
    Row(
        modifier = modifier
            .hoverable(hover)
            .then(
                if (wheelStep == null) {
                    Modifier
                } else {
                    Modifier.volumeWheel { notches ->
                        val changed = (latestLevel + notches * wheelStep).coerceIn(0, latestMaxLevel)
                        latestOnLevelChange(changed)
                    }
                },
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (slider == MediaVolumeSlider.WhilePointed) {
            AnimatedVisibility(
                visible = pointed,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally(),
            ) {
                track()
            }
        }
        IconButton(
            icon = icon,
            contentDescription = stringResource(if (muted) MR.strings.action_unmute else MR.strings.action_mute),
            onClick = onToggleMute,
            size = IconButtonSize.Large,
            tone = ContentTone.OnMedia,
            shortcut = muteShortcut,
        )
        if (slider == MediaVolumeSlider.Always && currentWindowLayout().width != WindowWidth.Compact) {
            track()
        }
    }
}

/** The wheel over the control, as whole notches up (positive) or down, which it takes from whatever lies under it. */
private fun Modifier.volumeWheel(onNotches: (Int) -> Unit): Modifier = pointerInput(Unit) {
    var pending = 0f
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent()
            if (event.type != PointerEventType.Scroll) continue
            val change = event.changes.firstOrNull() ?: continue
            change.consume()
            // Up is negative, as the wheel scrolls toward the start
            pending -= change.scrollDelta.y
            val notches = pending.toInt()
            if (notches == 0) continue
            pending -= notches
            onNotches(notches)
        }
    }
}

@Composable
private fun VolumeTrack(level: Int, maxLevel: Int, fullLevel: Int?, dimmed: Boolean, onLevelChange: (Int) -> Unit) {
    val colors = KatariTheme.colors
    val activeColor = if (dimmed) colors.onMediaVariant else colors.onMedia
    val restColor = colors.onMedia.copy(alpha = REST_TRACK_ALPHA)
    val markColor = colors.onMediaVariant
    val fraction = (level.toFloat() / maxLevel).coerceIn(0f, 1f)
    val latestMaxLevel by rememberUpdatedState(maxLevel)
    val latestOnLevelChange by rememberUpdatedState(onLevelChange)
    val label = stringResource(MR.strings.media_volume_label)

    Canvas(
        modifier = Modifier
            .width(TrackWidth)
            .height(KatariTheme.sizes.touchTarget)
            .semantics {
                contentDescription = label
                progressBarRangeInfo = ProgressBarRangeInfo(level.toFloat(), 0f..maxLevel.toFloat())
                setProgress { target ->
                    latestOnLevelChange(target.roundToInt().coerceIn(0, latestMaxLevel))
                    true
                }
            }
            .pointerInput(Unit) {
                fun levelAt(x: Float): Int {
                    val inset = Thumb.toPx() / 2f
                    val trackWidth = (size.width - inset * 2f).coerceAtLeast(1f)
                    return (((x - inset) / trackWidth).coerceIn(0f, 1f) * latestMaxLevel).roundToInt()
                }
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val pointerId = down.id
                    latestOnLevelChange(levelAt(down.position.x))
                    down.consume()
                    while (true) {
                        val change = awaitPointerEvent().changes.firstOrNull { it.id == pointerId } ?: break
                        if (!change.pressed) break
                        latestOnLevelChange(levelAt(change.position.x))
                        change.consume()
                    }
                }
            },
    ) {
        val inset = Thumb.toPx() / 2f
        val centerY = size.height / 2f
        val trackWidth = (size.width - inset * 2f).coerceAtLeast(0f)
        val levelX = inset + trackWidth * fraction
        val strokeWidth = Track.toPx()
        drawLine(restColor, Offset(inset, centerY), Offset(inset + trackWidth, centerY), strokeWidth, StrokeCap.Round)
        drawLine(activeColor, Offset(inset, centerY), Offset(levelX, centerY), strokeWidth, StrokeCap.Round)
        if (fullLevel != null && fullLevel in 1 until maxLevel) {
            val markX = inset + trackWidth * fullLevel / maxLevel
            val halfMark = Mark.toPx() / 2f
            drawLine(markColor, Offset(markX, centerY - halfMark), Offset(markX, centerY + halfMark), MarkWidth.toPx())
        }
        drawCircle(activeColor, radius = inset, center = Offset(levelX, centerY))
    }
}

private val TrackWidth = 88.dp
private val Track = 3.dp
private val Thumb = 12.dp
private val Mark = 12.dp
private val MarkWidth = 2.dp

private const val REST_TRACK_ALPHA = 0.32f
