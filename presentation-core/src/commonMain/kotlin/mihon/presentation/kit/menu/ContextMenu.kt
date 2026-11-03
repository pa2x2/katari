package mihon.presentation.kit.menu

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.round

/**
 * A right-click menu, open at [position], where the click landed in the box it is placed in. It goes over the box's
 * content, after it, and opens from the point whatever the content's size.
 */
@Composable
fun BoxScope.ContextMenu(
    position: Offset,
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier = Modifier.matchParentSize()) {
        Box(modifier = Modifier.offset { position.round() }) {
            DropdownMenu(expanded = true, onDismissRequest = onDismissRequest, content = content)
        }
    }
}
