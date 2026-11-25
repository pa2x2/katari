package app.katari.presentation.kit.list

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.DragHandle
import app.katari.presentation.kit.content.Icon
import app.katari.presentation.kit.pointer.moveCursor
import app.katari.presentation.kit.theme.ContentTone

/**
 * The grip a row in a reorderable list is dragged by, such as a category in Categories or a download in the queue.
 * [modifier] carries the list's drag handling; in a window worked with a mouse the grip shows the move cursor, so it
 * reads as something to drag rather than a plain icon. It takes an icon's room in the row, and a touch reaches it over
 * the standard touch target around it.
 */
@Composable
fun ReorderHandle(
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    Icon(
        imageVector = MaterialSymbols.Rounded.DragHandle,
        contentDescription = contentDescription,
        modifier = modifier.moveCursor(),
        tone = ContentTone.Secondary,
    )
}
