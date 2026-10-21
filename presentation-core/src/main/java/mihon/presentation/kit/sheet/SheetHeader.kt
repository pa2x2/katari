package mihon.presentation.kit.sheet

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.menu.OverflowMenu
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole

/** A sheet's title row, with optional [actions] such as a guide button and an overflow [menu] at its end. */
@Composable
fun SheetHeader(
    title: String,
    modifier: Modifier = Modifier,
    actions: (@Composable RowScope.() -> Unit)? = null,
    menu: (@Composable ColumnScope.(close: () -> Unit) -> Unit)? = null,
) {
    Row(
        modifier = modifier
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
        actions?.invoke(this)
        menu?.let { OverflowMenu(content = it) }
    }
}
