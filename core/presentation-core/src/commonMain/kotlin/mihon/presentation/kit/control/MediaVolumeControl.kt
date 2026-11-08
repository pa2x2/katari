package mihon.presentation.kit.control

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.dp
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.VolumeOff
import mihon.icons.materialsymbols.automirroredrounded.VolumeUp
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.button.IconButtonSize
import mihon.presentation.kit.shortcut.KeyboardShortcut
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.window.WindowWidth
import mihon.presentation.kit.window.currentWindowLayout
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.math.roundToInt

/**
 * A player's volume among controls laid over video: a button that mutes and unmutes, and a slider beside it setting
 * [level] from 0 to [maxLevel]. Above [fullLevel] the volume is louder than recorded, and a mark on the track shows where
 * that starts; null leaves the mark out. While [muted] the slider shows the level that unmuting returns to, and moving
 * it unmutes, as [onLevelChange] decides. A compact window leaves room for the other controls by showing the button
 * alone.
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
) {
    val icon = if (muted || level == 0) {
        MaterialSymbols.AutoMirroredRounded.VolumeOff
    } else {
        MaterialSymbols.AutoMirroredRounded.VolumeUp
    }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            icon = icon,
            contentDescription = stringResource(if (muted) MR.strings.action_unmute else MR.strings.action_mute),
            onClick = onToggleMute,
            size = IconButtonSize.Large,
            tone = ContentTone.OnMedia,
            shortcut = muteShortcut,
        )
        if (currentWindowLayout().width != WindowWidth.Compact) {
            VolumeTrack(
                level = level,
                maxLevel = maxLevel.coerceAtLeast(1),
                fullLevel = fullLevel,
                dimmed = muted,
                onLevelChange = onLevelChange,
            )
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
