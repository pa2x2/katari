package app.katari.presentation.kit.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole

/**
 * The figure a dashboard section or a sheet leads with, such as the time spent in the chosen period on Statistics,
 * larger than every figure around it. [facts] are short figures read at its end, each value before its label, such as
 * the period's session count. [detail] is a line under it that puts the figure in context, such as the change from the
 * period before.
 */
@Composable
fun LeadFigure(
    value: String,
    modifier: Modifier = Modifier,
    facts: List<Figure> = emptyList(),
    detail: (@Composable () -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.l),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = value,
                modifier = Modifier.weight(1f),
                role = TextRole.LeadMetric,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (facts.isNotEmpty()) {
                Column(
                    modifier = Modifier.padding(bottom = KatariTheme.spacing.xs),
                    horizontalAlignment = Alignment.End,
                ) {
                    facts.forEach { fact ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(text = fact.value, role = TextRole.Label, maxLines = 1)
                            Text(text = fact.label, role = TextRole.Supporting, maxLines = 1)
                        }
                    }
                }
            }
        }
        detail?.invoke()
    }
}
