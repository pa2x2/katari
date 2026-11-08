package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.surface.OverlayPill
import app.katari.presentation.kit.surface.OverlayStyle
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole
import eu.kanade.tachiyomi.ui.video.player.playback.formatPlaybackTimestamp
import kotlin.math.abs

/** How far a sideways swipe moves playback, such as "+1:15" or "−0:20", while the timeline shows where it lands. */
@Composable
internal fun VideoPlayerSwipeSeekDelta(
    deltaMs: Long,
    modifier: Modifier = Modifier,
) {
    val sign = if (deltaMs < 0L) MINUS_SIGN else PLUS_SIGN
    OverlayPill(modifier = modifier, style = OverlayStyle.Scrim) {
        Text(
            text = sign + formatPlaybackTimestamp(abs(deltaMs)),
            role = TextRole.SectionTitle,
            modifier = Modifier.padding(horizontal = KatariTheme.spacing.l, vertical = KatariTheme.spacing.s),
        )
    }
}

private const val PLUS_SIGN = "+"
private const val MINUS_SIGN = "−"
