package app.katari.presentation.kit.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import app.katari.presentation.kit.theme.ContentTone
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole

/**
 * Facts read as plain lines, each [Figure.label] at the start and its [Figure.value] at the end, such as the chapter a
 * title was last opened at and the time a session takes on its Activity sheet. A long value wraps onto a second line
 * at the end rather than pushing its label aside.
 */
@Composable
fun FactLines(facts: List<Figure>, modifier: Modifier = Modifier) {
    val spacing = KatariTheme.spacing
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.s)) {
        facts.forEach { fact ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.l),
                verticalAlignment = Alignment.Top,
            ) {
                Text(text = fact.label, role = TextRole.Body, tone = ContentTone.Secondary, maxLines = 1)
                Text(
                    text = fact.value,
                    modifier = Modifier.weight(1f),
                    role = TextRole.Body,
                    textAlign = TextAlign.End,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
