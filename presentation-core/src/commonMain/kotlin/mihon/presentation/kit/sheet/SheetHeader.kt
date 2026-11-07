package mihon.presentation.kit.sheet

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.ArrowBack
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.menu.OverflowMenu
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * A sheet's title row, with optional [actions] such as a guide button and an overflow [menu] at its end. A page opened
 * inside the sheet passes [onBack], which leads the row with a back button. [subtitle] sums up the page under the
 * title, such as how many of its items are selected.
 */
@Composable
fun SheetHeader(
    title: String,
    modifier: Modifier = Modifier,
    actions: (@Composable RowScope.() -> Unit)? = null,
    menu: (@Composable ColumnScope.(close: () -> Unit) -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    subtitle: String? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = if (onBack != null) KatariTheme.spacing.xs else KatariTheme.spacing.rowInset,
                end = KatariTheme.spacing.xs,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(
                icon = MaterialSymbols.AutoMirroredRounded.ArrowBack,
                contentDescription = stringResource(MR.strings.action_back),
                onClick = onBack,
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = KatariTheme.spacing.l),
        ) {
            Text(text = title, role = TextRole.SheetTitle)
            subtitle?.let { Text(text = it, role = TextRole.Supporting) }
        }
        actions?.invoke(this)
        menu?.let { OverflowMenu(content = it) }
    }
}
