package app.katari.presentation.kit.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.katari.presentation.kit.button.Button
import app.katari.presentation.kit.button.ButtonStyle
import app.katari.presentation.kit.theme.KatariTheme

/** One answer to the row above [RowActions]. [destructive] marks an answer that deletes something. */
data class RowAction(val label: String, val onClick: () -> Unit, val destructive: Boolean = false)

/**
 * The answers to the row above, in the same row group slice and lined up with its end, such as which side of a
 * conflict to keep. The last is the proposed one and stands out; they wrap onto more lines where the slice is narrow.
 */
@Composable
fun RowActions(actions: List<RowAction>, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = LocalRowInset.current, end = LocalRowInset.current, bottom = KatariTheme.spacing.m),
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s, Alignment.End),
        verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
    ) {
        actions.forEachIndexed { index, action ->
            Button(
                label = action.label,
                onClick = action.onClick,
                style = if (index == actions.lastIndex) ButtonStyle.Tonal else ButtonStyle.Text,
                destructive = action.destructive,
            )
        }
    }
}
