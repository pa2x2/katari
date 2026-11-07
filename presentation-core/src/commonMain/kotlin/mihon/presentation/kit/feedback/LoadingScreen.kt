package mihon.presentation.kit.feedback

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.progress.LinearProgressIndicator
import mihon.presentation.kit.progress.LoadingIndicator
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole

/**
 * Fills the space while its content loads. A wait long enough to read names its current step in [message], and
 * [progress] (0..1) adds a bar under it while that step knows how far it has got. The bar's room stays reserved
 * whenever [message] shows, so steps with and without a bar don't shift the screen as they take turns.
 */
@Composable
fun LoadingScreen(
    modifier: Modifier = Modifier,
    message: String? = null,
    progress: (() -> Float)? = null,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        if (message == null) {
            LoadingIndicator()
            return@Box
        }
        Column(
            modifier = Modifier
                .widthIn(max = MessageWidth)
                .padding(horizontal = KatariTheme.spacing.xl),
            verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.l),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LoadingIndicator()
            Text(
                text = message,
                role = TextRole.Body,
                tone = ContentTone.Secondary,
                textAlign = TextAlign.Center,
            )
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (progress == null) 0f else 1f),
                progress = progress ?: { 0f },
            )
        }
    }
}

private val MessageWidth = 320.dp
