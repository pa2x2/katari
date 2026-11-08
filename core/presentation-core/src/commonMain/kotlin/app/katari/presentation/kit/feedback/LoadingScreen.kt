package app.katari.presentation.kit.feedback

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.progress.LinearProgressIndicator
import app.katari.presentation.kit.progress.LoadingIndicator
import app.katari.presentation.kit.theme.ContentTone
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole

/**
 * Fills the space while its content loads. [message] names the step the wait is on, [progress] (0..1) adds a bar under
 * it while that step knows how far it has got, and [action] offers a way past the wait under both. The indicator stays
 * at the centre whatever shows under it, so steps coming and going never move it. [onMedia] draws it over video or
 * artwork in the media tones.
 */
@Composable
fun LoadingScreen(
    modifier: Modifier = Modifier,
    message: String? = null,
    progress: (() -> Float)? = null,
    onMedia: Boolean = false,
    action: (@Composable () -> Unit)? = null,
) {
    val indicatorTone = if (onMedia) ContentTone.OnMedia else ContentTone.Primary
    if (message == null && progress == null && action == null) {
        // Also laid out where the height is unbounded, such as inside a dialog, which the weighted column can't be.
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            LoadingIndicator(tone = indicatorTone)
        }
        return
    }
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Equal weights above and below keep the indicator centred however tall the status under it grows.
        Spacer(Modifier.weight(1f))
        LoadingIndicator(tone = indicatorTone)
        Column(
            modifier = Modifier
                .weight(1f)
                .widthIn(max = StatusWidth)
                .padding(horizontal = KatariTheme.spacing.xl)
                .padding(top = KatariTheme.spacing.l),
            verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.l),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            message?.let {
                Text(
                    text = it,
                    role = TextRole.Body,
                    tone = if (onMedia) ContentTone.OnMediaVariant else ContentTone.Secondary,
                    textAlign = TextAlign.Center,
                )
            }
            progress?.let { LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), progress = it) }
            action?.invoke()
        }
    }
}

private val StatusWidth = 320.dp
