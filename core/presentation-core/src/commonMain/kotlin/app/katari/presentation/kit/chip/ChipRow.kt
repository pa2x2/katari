package app.katari.presentation.kit.chip

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.katari.presentation.kit.layout.HorizontalScrollbarFrame
import app.katari.presentation.kit.theme.KatariTheme

/**
 * Chips side by side, such as an entry's tags: on one line that scrolls sideways, or with [wrap] continued onto as many
 * lines as they need. Chips keep their own touch margin, which also spaces wrapped lines apart.
 */
@Composable
fun ChipRow(
    wrap: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val gap = Arrangement.spacedBy(KatariTheme.spacing.xs)
    if (wrap) {
        FlowRow(
            modifier = modifier.padding(horizontal = KatariTheme.spacing.contentInset),
            horizontalArrangement = gap,
        ) {
            content()
        }
    } else {
        val scrollState = rememberScrollState()
        HorizontalScrollbarFrame(scrollState, modifier) { rowModifier ->
            Row(
                modifier = rowModifier
                    .horizontalScroll(scrollState)
                    .padding(horizontal = KatariTheme.spacing.contentInset),
                horizontalArrangement = gap,
            ) {
                content()
            }
        }
    }
}
