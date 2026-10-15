package mihon.presentation.kit.menu

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.MoreVert
import mihon.presentation.kit.button.IconButton
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** A "More" button that opens a menu; [content] receives a callback that closes it. */
@Composable
fun OverflowMenu(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.(close: () -> Unit) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier.wrapContentSize(Alignment.TopStart)) {
        IconButton(
            icon = MaterialSymbols.Rounded.MoreVert,
            contentDescription = stringResource(MR.strings.label_more),
            onClick = { expanded = true },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            content { expanded = false }
        }
    }
}
