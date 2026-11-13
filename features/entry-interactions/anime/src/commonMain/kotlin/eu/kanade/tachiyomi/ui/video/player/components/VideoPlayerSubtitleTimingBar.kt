package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.Add
import app.katari.icons.materialsymbols.rounded.Check
import app.katari.icons.materialsymbols.rounded.Close
import app.katari.icons.materialsymbols.rounded.RecordVoiceOver
import app.katari.icons.materialsymbols.rounded.Remove
import app.katari.icons.materialsymbols.rounded.RestartAlt
import app.katari.icons.materialsymbols.rounded.Subtitles
import app.katari.presentation.kit.button.Button
import app.katari.presentation.kit.button.ButtonStyle
import app.katari.presentation.kit.button.IconButton
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.navigation.MediaToolbar
import app.katari.presentation.kit.navigation.MediaToolbarButton
import app.katari.presentation.kit.navigation.mediaToolbarSlot
import app.katari.presentation.kit.surface.OverlayPill
import app.katari.presentation.kit.surface.OverlayStyle
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole
import eu.kanade.tachiyomi.ui.video.player.subtitle.timing.SubtitleLineSync
import eu.kanade.tachiyomi.ui.video.player.subtitle.timing.signedSubtitleTimingSeconds
import eu.kanade.tachiyomi.ui.video.player.subtitle.timing.steppedSubtitleTiming
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The subtitle timing bar over the playing video (D002), under the top bar: − and + move subtitles a step
 * earlier or later, again and again while held (D003); the value opens a dialog to type it; Sync to a line moves them
 * by two marks (D006); Reset puts them back and ✓ closes the bar. [positionMs] is the playback position now, which the
 * marks take; [onStep] moves the timing steps from where it is when the step comes.
 */
@Composable
internal fun VideoPlayerSubtitleTimingBar(
    delayMs: Long,
    onSet: (Long) -> Unit,
    onStep: (Int) -> Unit,
    positionMs: () -> Long,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var sync by remember { mutableStateOf<SubtitleLineSync?>(null) }
    var typing by remember { mutableStateOf(false) }
    OverlayPill(modifier = modifier.videoPlayerUnderTopBarSlot(), style = OverlayStyle.Scrim) {
        // As wide as its buttons with their labels, and no wider than the window, where labels drop out
        MediaToolbar(
            modifier = Modifier
                .width(IntrinsicSize.Max)
                .padding(horizontal = KatariTheme.spacing.xs),
        ) {
            val marking = sync
            if (marking == null) {
                TimingSteps(delayMs = delayMs, onStep = onStep, onType = { typing = true })
                MediaToolbarButton(
                    icon = MaterialSymbols.Rounded.RecordVoiceOver,
                    label = stringResource(MR.strings.anime_subtitle_timing_sync),
                    iconDescription = stringResource(MR.strings.anime_subtitle_timing_sync),
                    onClick = { sync = SubtitleLineSync() },
                    modifier = Modifier.mediaToolbarSlot(labelDropOrder = 1),
                )
                MediaToolbarButton(
                    icon = MaterialSymbols.Rounded.RestartAlt,
                    label = stringResource(MR.strings.action_reset),
                    iconDescription = stringResource(MR.strings.action_reset),
                    onClick = { onSet(0L) },
                    modifier = Modifier.mediaToolbarSlot(labelDropOrder = 0),
                )
                IconButton(
                    icon = MaterialSymbols.Rounded.Check,
                    contentDescription = stringResource(MR.strings.action_done),
                    onClick = onClose,
                    modifier = Modifier.mediaToolbarSlot(trailing = true),
                )
            } else {
                LineMarks(
                    sync = marking,
                    onMark = { marked ->
                        val shift = marked.shiftMs
                        if (shift == null) {
                            sync = marked
                        } else {
                            sync = null
                            onSet(steppedSubtitleTiming(delayMs + shift))
                        }
                    },
                    positionMs = positionMs,
                )
                IconButton(
                    icon = MaterialSymbols.Rounded.Close,
                    contentDescription = stringResource(MR.strings.action_cancel),
                    onClick = { sync = null },
                    modifier = Modifier.mediaToolbarSlot(trailing = true),
                )
            }
        }
    }
    if (typing) {
        VideoPlayerSubtitleTimingDialog(
            delayMs = delayMs,
            onSet = { typed ->
                typing = false
                onSet(typed)
            },
            onDismissRequest = { typing = false },
        )
    }
}

/** − value +: the steps, and the value, which opens the dialog to type it. */
@Composable
private fun TimingSteps(delayMs: Long, onStep: (Int) -> Unit, onType: () -> Unit) {
    IconButton(
        icon = MaterialSymbols.Rounded.Remove,
        contentDescription = stringResource(MR.strings.anime_subtitle_timing_earlier),
        onClick = { onStep(-1) },
        repeatWhileHeld = true,
    )
    Button(
        label = subtitleTimingLabel(delayMs),
        onClick = onType,
        style = ButtonStyle.Text,
    )
    IconButton(
        icon = MaterialSymbols.Rounded.Add,
        contentDescription = stringResource(MR.strings.anime_subtitle_timing_later),
        onClick = { onStep(1) },
        repeatWhileHeld = true,
    )
}

/**
 * Sync to a line: what to mark next, then Line heard and Subtitle shown, in either order. A mark made shows its check;
 * marking it again takes the position anew.
 */
@Composable
private fun LineMarks(sync: SubtitleLineSync, onMark: (SubtitleLineSync) -> Unit, positionMs: () -> Long) {
    val prompt = when {
        sync.shownAtMs != null -> MR.strings.anime_subtitle_timing_sync_after_shown
        sync.heardAtMs != null -> MR.strings.anime_subtitle_timing_sync_after_heard
        else -> MR.strings.anime_subtitle_timing_sync_start
    }
    Text(
        text = stringResource(prompt),
        role = TextRole.Body,
        // Wraps in a narrow bar rather than squeezing the marks' labels, which say what each mark is
        overflow = TextOverflow.Ellipsis,
        maxLines = 2,
        modifier = Modifier
            .mediaToolbarSlot(fillsLeftover = true)
            .padding(horizontal = KatariTheme.spacing.m),
    )
    MediaToolbarButton(
        icon = if (sync.heardAtMs != null) MaterialSymbols.Rounded.Check else MaterialSymbols.Rounded.RecordVoiceOver,
        label = stringResource(MR.strings.anime_subtitle_timing_line_heard),
        iconDescription = stringResource(MR.strings.anime_subtitle_timing_line_heard),
        onClick = { onMark(sync.heard(positionMs())) },
        modifier = Modifier.mediaToolbarSlot(labelDropOrder = 0),
    )
    MediaToolbarButton(
        icon = if (sync.shownAtMs != null) MaterialSymbols.Rounded.Check else MaterialSymbols.Rounded.Subtitles,
        label = stringResource(MR.strings.anime_subtitle_timing_subtitle_shown),
        iconDescription = stringResource(MR.strings.anime_subtitle_timing_subtitle_shown),
        onClick = { onMark(sync.shown(positionMs())) },
        modifier = Modifier.mediaToolbarSlot(labelDropOrder = 0),
    )
}

/** [delayMs] as the bar and the Settings row show it: "+0.4 s", "−1.5 s", "0.0 s". */
@Composable
internal fun subtitleTimingLabel(delayMs: Long): String =
    stringResource(MR.strings.anime_subtitle_timing_seconds, signedSubtitleTimingSeconds(delayMs))
