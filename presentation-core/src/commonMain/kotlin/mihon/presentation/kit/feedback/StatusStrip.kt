package mihon.presentation.kit.feedback

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonSize
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.progress.LinearProgressIndicator
import mihon.presentation.kit.progress.LoadingIndicator
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.surface.isLight
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole

/**
 * What a [StatusStrip] reports. The accent tones mark app-wide modes, such as incognito, so each mode keeps its own
 * colour; [Neutral] is work a screen is doing, such as a library update.
 */
enum class StatusStripTone { Primary, Secondary, Tertiary, Neutral }

/** Whether this tone's fill is light, so the system draws dark status bar icons over a strip at the top. */
@Composable
@ReadOnlyComposable
fun StatusStripTone.isLight(): Boolean = container.isLight()

/**
 * A full-width strip saying what the app or a screen is doing, such as incognito mode or a running library update.
 * [waiting] puts the wait indicator before [text]; [progress] (0..1) adds a bar under it. With an [actionLabel], the
 * text sits at the start and the action at the end, such as cancelling the update; without one, the text is centred.
 * [windowInsets] pads the content, not the fill, so a strip at the top runs its colour under the status bar.
 */
@Composable
fun StatusStrip(
    text: String,
    tone: StatusStripTone,
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = WindowInsets(0),
    waiting: Boolean = false,
    progress: (() -> Float)? = null,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    val spacing = KatariTheme.spacing
    Surface(modifier = modifier.fillMaxWidth(), container = tone.container) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(windowInsets)
                .padding(
                    start = spacing.l,
                    end = if (actionLabel != null) spacing.xs else spacing.l,
                    top = spacing.s,
                    bottom = spacing.s,
                ),
            horizontalArrangement = Arrangement.spacedBy(spacing.s, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (waiting) {
                LoadingIndicator(modifier = Modifier.size(KatariTheme.sizes.iconSmall), tone = ContentTone.Inherit)
            }
            val fills = actionLabel != null || progress != null
            Column(
                modifier = if (fills) Modifier.weight(1f) else Modifier,
                verticalArrangement = Arrangement.spacedBy(spacing.s),
            ) {
                Text(
                    text = text,
                    modifier = if (fills) Modifier.fillMaxWidth() else Modifier,
                    role = TextRole.Caption,
                    textAlign = if (actionLabel != null) TextAlign.Start else TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (progress != null) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        progress = progress,
                        onContainer = tone != StatusStripTone.Neutral,
                    )
                }
            }
            if (actionLabel != null) {
                Button(label = actionLabel, onClick = onAction, style = ButtonStyle.Text, size = ButtonSize.Small)
            }
        }
    }
}

private val StatusStripTone.container: SurfaceContainer
    get() = when (this) {
        StatusStripTone.Primary -> SurfaceContainer.Primary
        StatusStripTone.Secondary -> SurfaceContainer.Secondary
        StatusStripTone.Tertiary -> SurfaceContainer.Tertiary
        StatusStripTone.Neutral -> SurfaceContainer.Default
    }
