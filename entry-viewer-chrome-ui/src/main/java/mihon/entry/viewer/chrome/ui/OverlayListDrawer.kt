package mihon.entry.viewer.chrome.ui

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

/** An [OverlayDrawer] with a title row, for reader and player navigation lists. */
@Composable
fun OverlayListDrawer(
    visible: Boolean,
    title: String,
    icon: ImageVector,
    closeDescription: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    drawerModifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    OverlayDrawer(
        visible = visible,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        drawerModifier = drawerModifier,
    ) {
        OverlayDrawerHeader(
            title = title,
            icon = icon,
            closeDescription = closeDescription,
            onClose = onDismissRequest,
        )
        content()
    }
}
