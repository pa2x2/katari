package mihon.presentation.kit.surface

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole

/** A small rounded label for a value, such as a slider's current setting. */
@Composable
fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    container: SurfaceContainer = SurfaceContainer.High,
    role: TextRole = TextRole.Body,
) {
    Surface(modifier = modifier, container = container, shape = KatariTheme.shapes.full) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = KatariTheme.spacing.s, vertical = KatariTheme.spacing.xxs),
            role = role,
            maxLines = 1,
        )
    }
}

/** The state of an engine, model or service, such as Ready or Setup required. */
enum class StatusTone { Ready, Checking, ActionRequired, Unavailable }

@Composable
fun StatusPill(
    text: String,
    tone: StatusTone,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        container = when (tone) {
            StatusTone.Ready -> SurfaceContainer.TertiaryContainer
            StatusTone.Checking -> SurfaceContainer.Highest
            StatusTone.ActionRequired -> SurfaceContainer.SecondaryContainer
            StatusTone.Unavailable -> SurfaceContainer.ErrorContainer
        },
        shape = KatariTheme.shapes.full,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = KatariTheme.spacing.m, vertical = KatariTheme.spacing.xs),
            role = TextRole.Badge,
            maxLines = 1,
        )
    }
}
