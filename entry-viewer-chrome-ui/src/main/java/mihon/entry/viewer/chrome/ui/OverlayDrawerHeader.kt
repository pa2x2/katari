package mihon.entry.viewer.chrome.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Close
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.menu.OverflowMenu
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole

/** The title row of an [OverlayDrawer], with an optional overflow [menu] before the close button. */
@Composable
fun OverlayDrawerHeader(
    title: String,
    icon: ImageVector,
    closeDescription: String,
    onClose: () -> Unit,
    menu: (@Composable ColumnScope.(close: () -> Unit) -> Unit)? = null,
) {
    val spacing = KatariTheme.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = spacing.l, top = spacing.m, end = spacing.s, bottom = spacing.s),
        horizontalArrangement = Arrangement.spacedBy(spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null)
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            role = TextRole.SectionTitle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        menu?.let { OverflowMenu(content = it) }
        IconButton(
            icon = MaterialSymbols.Rounded.Close,
            contentDescription = closeDescription,
            onClick = onClose,
        )
    }
}
