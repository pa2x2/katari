package mihon.presentation.kit.sheet

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.menu.OverflowMenu
import mihon.presentation.kit.surface.HorizontalDivider
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole

/** A sheet's title row, with an optional overflow [menu], separated from the content below by a divider. */
@Composable
fun SheetHeader(
    title: String,
    modifier: Modifier = Modifier,
    menu: (@Composable ColumnScope.(close: () -> Unit) -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = KatariTheme.spacing.rowInset, end = KatariTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = KatariTheme.spacing.l),
                role = TextRole.SheetTitle,
            )
            menu?.let { OverflowMenu(content = it) }
        }
        HorizontalDivider()
    }
}
