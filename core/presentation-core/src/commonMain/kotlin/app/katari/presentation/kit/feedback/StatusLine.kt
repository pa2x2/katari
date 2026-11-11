package app.katari.presentation.kit.feedback

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.automirroredrounded.KeyboardArrowRight
import app.katari.presentation.kit.button.Button
import app.katari.presentation.kit.button.ButtonSize
import app.katari.presentation.kit.button.ButtonStyle
import app.katari.presentation.kit.content.Icon
import app.katari.presentation.kit.list.TrailingControlsLayout
import app.katari.presentation.kit.pointer.clickCursor
import app.katari.presentation.kit.progress.LinearProgressIndicator
import app.katari.presentation.kit.theme.ContentTone
import app.katari.presentation.kit.theme.KatariTheme

/**
 * A line at the top of a list saying how the work behind the list stands, such as when the library last updated on
 * Updates, or how far a running update got. Unlike a [StatusStrip], it belongs to the list and scrolls with it.
 *
 * Its end holds the [actionLabel] action, such as cancelling the update, and, when [onClick] opens the work's details,
 * the arrow that says so. They keep their place whatever the [lines] say: the lines wrap beside them, and only a window
 * too narrow for a word beside them moves them under the lines. [progress] (0..1) adds a bar under the lines.
 */
@Composable
fun StatusLine(
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    progress: (() -> Float)? = null,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
    lines: @Composable ColumnScope.() -> Unit,
) {
    val spacing = KatariTheme.spacing
    TrailingControlsLayout(
        leading = {},
        words = { Column(content = lines) },
        trailing = {
            if (actionLabel != null || onClick != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (actionLabel != null) {
                        Button(
                            label = actionLabel,
                            onClick = onAction,
                            style = ButtonStyle.Text,
                            size = ButtonSize.Small,
                        )
                    }
                    if (onClick != null) {
                        Icon(
                            imageVector = MaterialSymbols.AutoMirroredRounded.KeyboardArrowRight,
                            contentDescription = null,
                            tone = ContentTone.Secondary,
                        )
                    }
                }
            }
        },
        leadingGap = KatariTheme.sizes.none,
        trailingGap = spacing.s,
        below = {
            if (progress != null) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().padding(top = spacing.s),
                    progress = progress,
                )
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickCursor().clickable(onClick = onClick) else Modifier)
            .padding(
                start = spacing.contentInset,
                end = if (actionLabel != null || onClick != null) spacing.s else spacing.contentInset,
                top = spacing.s,
                bottom = spacing.s,
            ),
    )
}
