package app.katari.presentation.kit.surface

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole

/**
 * A small rounded label for a value, such as a slider's current setting, optionally led by an inline [Avatar]. With
 * [onClick] it opens what sets the value, such as a list's filter summary opening the filter.
 */
@Composable
fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    container: SurfaceContainer = SurfaceContainer.High,
    role: TextRole = TextRole.Body,
    leading: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier.padding(
                start = if (leading != null) KatariTheme.spacing.xxs else KatariTheme.spacing.s,
                end = KatariTheme.spacing.s,
                top = KatariTheme.spacing.xxs,
                bottom = KatariTheme.spacing.xxs,
            ),
            horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leading?.invoke()
            Text(text = text, role = role, maxLines = 1)
        }
    }
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier,
            container = container,
            shape = KatariTheme.shapes.full,
            content = content,
        )
    } else {
        Surface(modifier = modifier, container = container, shape = KatariTheme.shapes.full, content = content)
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
