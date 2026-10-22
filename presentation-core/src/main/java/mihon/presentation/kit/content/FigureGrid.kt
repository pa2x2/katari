package mihon.presentation.kit.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole

/** One figure of a [FigureGrid]: [value] above the [label] that names it, and an optional [detail] under them. */
@Immutable
data class Figure(val value: String, val label: String, val detail: String? = null)

/**
 * [figures] laid out [columns] to a row inside a card, such as a dashboard section's session counts. The card already
 * sets them apart from the screen, so a figure has no tile of its own.
 */
@Composable
fun FigureGrid(figures: List<Figure>, modifier: Modifier = Modifier, columns: Int = 2) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.l)) {
        figures.chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.l)) {
                row.forEach { figure ->
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = figure.value,
                            role = TextRole.SectionTitle,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(text = figure.label, role = TextRole.Supporting, tone = ContentTone.Secondary)
                        figure.detail?.let { Text(text = it, role = TextRole.Badge, tone = ContentTone.Secondary) }
                    }
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
